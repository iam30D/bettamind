package org.bettamind.shared.privacy

private const val MinimumKeyBytes = 32

class StorageKeyMaterial private constructor(private val bytes: ByteArray) {
    init {
        require(bytes.size >= MinimumKeyBytes) {
            "Storage key material must be at least $MinimumKeyBytes bytes."
        }
    }

    fun copyBytes(): ByteArray = bytes.copyOf()

    fun destroy() {
        bytes.fill(0)
    }

    companion object {
        fun fromBytes(bytes: ByteArray): StorageKeyMaterial = StorageKeyMaterial(bytes.copyOf())
    }
}

data class StorageRecordKey(val value: String) {
    init {
        require(value.isNotBlank()) { "Storage record key cannot be blank." }
    }
}

data class EncryptedBackupPackage(
    val formatVersion: Int,
    val ciphertext: ByteArray,
) {
    init {
        require(formatVersion > 0) { "Backup format version must be positive." }
        require(ciphertext.isNotEmpty()) { "Backup package cannot be empty." }
    }
}

sealed class EncryptedStorageException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {
    class StoreUnavailable(cause: Throwable? = null) :
        EncryptedStorageException("Encrypted storage is unavailable.", cause)

    class WrongKey(cause: Throwable? = null) :
        EncryptedStorageException("Encrypted storage key was rejected.", cause)

    class IntegrityCheckFailed(cause: Throwable? = null) :
        EncryptedStorageException("Encrypted storage integrity check failed.", cause)

    class Deleted :
        EncryptedStorageException("Encrypted storage has been deleted.")
}

interface EncryptedRecordStore {
    fun open(key: StorageKeyMaterial)
    fun put(recordKey: StorageRecordKey, value: ByteArray)
    fun get(recordKey: StorageRecordKey): ByteArray?
    fun delete(recordKey: StorageRecordKey)
    fun rotateKey(currentKey: StorageKeyMaterial, newKey: StorageKeyMaterial)
    fun exportEncryptedBackup(key: StorageKeyMaterial): EncryptedBackupPackage
    fun restoreEncryptedBackup(key: StorageKeyMaterial, backup: EncryptedBackupPackage)
    fun deleteAll()
    fun close()
}

interface StorageKeyManager {
    fun loadOrCreateDatabaseKey(): StorageKeyMaterial
    fun replaceDatabaseKey(newKey: StorageKeyMaterial)
    fun deleteDatabaseKey()
}
