package org.bettamind.shared.privacy

import cnames.structs.__CFData
import kotlinx.cinterop.*
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecRandomDefault
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.darwin.OSStatus
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
class IosKeychainStorageKeyManager(
    private val service: String = "org.bettamind.phase3",
    private val account: String = "sqlcipher-key",
) : StorageKeyManager {
    override fun loadOrCreateDatabaseKey(): StorageKeyMaterial {
        readKey()?.let { return StorageKeyMaterial.fromBytes(it) }

        val key = randomBytes(SqlCipherKeyBytes)
        return try {
            val status = SecItemAdd(baseQuery().withValueData(key), null)
            if (status != errSecSuccess) {
                throw keychainUnavailable("add database key", status)
            }
            StorageKeyMaterial.fromBytes(key)
        } finally {
            key.fill(0)
        }
    }

    override fun replaceDatabaseKey(newKey: StorageKeyMaterial) {
        val keyBytes = newKey.copyBytes()
        val update = mutableDictionary().withValueData(keyBytes)
        val status = SecItemUpdate(baseQuery(), update)
        try {
            if (status == errSecItemNotFound) {
                val addStatus = SecItemAdd(baseQuery().withValueData(keyBytes), null)
                if (addStatus != errSecSuccess) {
                    throw keychainUnavailable("add replacement database key", addStatus)
                }
            } else if (status != errSecSuccess) {
                throw keychainUnavailable("update database key", status)
            }
        } finally {
            keyBytes.fill(0)
        }
    }

    override fun deleteDatabaseKey() {
        val status = SecItemDelete(baseQuery())
        if (status != errSecSuccess && status != errSecItemNotFound) {
            throw keychainUnavailable("delete database key", status)
        }
    }

    private fun readKey(): ByteArray? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status: OSStatus = SecItemCopyMatching(
            baseQuery().apply {
                setValue(kSecReturnData, kCFBooleanTrue)
                setValue(kSecMatchLimit, kSecMatchLimitOne)
            },
            result.ptr,
        )
        when (status) {
            errSecSuccess -> result.value?.reinterpret<__CFData>()?.toByteArray()
                ?: throw EncryptedStorageException.StoreUnavailable()

            errSecItemNotFound -> null
            else -> throw keychainUnavailable("read database key", status)
        }
    }

    private fun baseQuery(): CFMutableDictionaryRef =
        mutableDictionary().apply {
            setValue(kSecClass, kSecClassGenericPassword)
            setValue(kSecAttrService, service.toCfString())
            setValue(kSecAttrAccount, account.toCfString())
            setValue(kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        }

    private fun mutableDictionary(): CFMutableDictionaryRef =
        CFDictionaryCreateMutable(
            kCFAllocatorDefault,
            0,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )
            ?: throw EncryptedStorageException.StoreUnavailable()

    private fun CFMutableDictionaryRef.withValueData(bytes: ByteArray): CFMutableDictionaryRef =
        apply { setValue(kSecValueData, bytes.toCfData()) }

    private fun CFMutableDictionaryRef.setValue(key: CFTypeRef?, value: CFTypeRef?) {
        CFDictionarySetValue(this, key, value)
    }

    private fun String.toCfString(): CFTypeRef? =
        CFStringCreateWithCString(kCFAllocatorDefault, this, kCFStringEncodingUTF8)

    private fun ByteArray.toCfData(): CFTypeRef? =
        usePinned { pinned ->
            CFDataCreate(kCFAllocatorDefault, pinned.addressOf(0).reinterpret(), size.convert())
        }

    private fun randomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        val status = bytes.usePinned {
            SecRandomCopyBytes(kSecRandomDefault, size.convert(), it.addressOf(0))
        }
        if (status != errSecSuccess) {
            throw keychainUnavailable("generate random database key", status)
        }
        return bytes
    }

    private fun kotlinx.cinterop.CPointer<__CFData>.toByteArray(): ByteArray {
        val length = CFDataGetLength(this).toInt()
        val output = ByteArray(length)
        output.usePinned { pinned ->
            memcpy(pinned.addressOf(0), CFDataGetBytePtr(this), length.convert())
        }
        return output
    }

    companion object {
        private const val SqlCipherKeyBytes = 32
    }
}

private fun keychainUnavailable(operation: String, status: OSStatus): EncryptedStorageException.StoreUnavailable =
    EncryptedStorageException.StoreUnavailable(
        IllegalStateException("iOS Keychain $operation failed with OSStatus $status."),
    )
