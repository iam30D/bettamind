package org.bettamind.shared.privacy

import kotlin.random.Random
import platform.Foundation.NSTemporaryDirectory
import platform.posix.remove

fun runIosEncryptedStorageAppValidation(): String =
    runIosEncryptedStorageAppValidationWithAccessGroup(keychainAccessGroup = null)

fun runIosEncryptedStorageAppValidationWithAccessGroup(keychainAccessGroup: String?): String =
    try {
        IosEncryptedStorageAppValidation(keychainAccessGroup = keychainAccessGroup).run()
        "PASS: iOS Keychain and SQLCipher app-hosted validation completed."
    } catch (throwable: Throwable) {
        "FAIL: ${throwable.message ?: throwable.toString()}${throwable.causeMessage()}"
    }

private class IosEncryptedStorageAppValidation(
    private val keychainAccessGroup: String?,
) {
    fun run() {
        val service = "org.bettamind.validation.${Random.nextInt()}"
        val manager = IosKeychainStorageKeyManager(
            service = service,
            account = "sqlcipher-key",
            accessGroup = keychainAccessGroup?.takeIf { group -> group.isNotBlank() },
        )
        val databasePath = temporaryDatabasePath()
        val restoredPath = temporaryDatabasePath()
        val store = IosSqlCipherEncryptedRecordStore(databasePath = databasePath)
        val restored = IosSqlCipherEncryptedRecordStore(databasePath = restoredPath)
        var validationFailure: Throwable? = null

        try {
            IosSqlCipherEncryptedRecordStore.assertSqlCipherAvailable()
            val firstKey = manager.loadOrCreateDatabaseKey()
            val reloadedKey = manager.loadOrCreateDatabaseKey()
            require(firstKey.copyBytes().contentEquals(reloadedKey.copyBytes())) {
                "iOS Keychain did not reload the stored SQLCipher key."
            }

            val recordKey = StorageRecordKey("app-validation")
            store.open(firstKey)
            store.put(recordKey, byteArrayOf(10, 20, 30, 40))
            require(store.get(recordKey)?.contentEquals(byteArrayOf(10, 20, 30, 40)) == true) {
                "iOS SQLCipher did not return the stored record."
            }

            val backup = store.exportEncryptedBackup(firstKey)
            val replacementKey = seededKey(9)
            manager.replaceDatabaseKey(replacementKey)
            store.rotateKey(currentKey = firstKey, newKey = replacementKey)
            try {
                restored.restoreEncryptedBackup(seededKey(5), backup)
                error("iOS SQLCipher accepted an encrypted backup with the wrong key.")
            } catch (_: EncryptedStorageException.WrongKey) {
                // Expected: the encrypted backup must only restore with its SQLCipher key.
            }
            restored.restoreEncryptedBackup(firstKey, backup)
            require(restored.get(recordKey)?.contentEquals(byteArrayOf(10, 20, 30, 40)) == true) {
                "iOS SQLCipher encrypted backup restore failed."
            }
            restored.close()

            val loadedReplacement = manager.loadOrCreateDatabaseKey()
            require(replacementKey.copyBytes().contentEquals(loadedReplacement.copyBytes())) {
                "iOS Keychain did not reload the replacement SQLCipher key."
            }
        } catch (throwable: Throwable) {
            validationFailure = throwable
            throw throwable
        } finally {
            val cleanupFailure = sequenceOf(
                cleanupFailure { store.deleteAll() },
                cleanupFailure { restored.deleteAll() },
                cleanupFailure { manager.deleteDatabaseKey() },
                cleanupFailure { remove("$databasePath.restore") },
                cleanupFailure { remove("$restoredPath.restore") },
            ).firstOrNull { failure -> failure != null }

            if (validationFailure == null && cleanupFailure != null) {
                throw cleanupFailure
            }
        }
    }

    private fun temporaryDatabasePath(): String =
        "${NSTemporaryDirectory().trimEnd('/')}/bettamind-validation-${Random.nextInt()}.db"

    private fun seededKey(seed: Int): StorageKeyMaterial {
        val random = Random(seed)
        return StorageKeyMaterial.fromBytes(ByteArray(32) { random.nextInt(0, 256).toByte() })
    }
}

private inline fun cleanupFailure(block: () -> Unit): Throwable? =
    try {
        block()
        null
    } catch (throwable: Throwable) {
        throwable
    }

private fun Throwable.causeMessage(): String =
    cause?.let { cause -> " Cause: ${cause.message ?: cause.toString()}" }.orEmpty()
