package org.bettamind.shared.privacy

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.fail
import platform.Foundation.NSTemporaryDirectory
import platform.posix.remove

@OptIn(ExperimentalForeignApi::class)
class IosEncryptedStorageIntegrationTest {
    @Test
    fun sqlCipherStoreRejectsWrongKeyAndRestoresEncryptedBackup() {
        val path = temporaryDatabasePath()
        val restoredPath = temporaryDatabasePath()
        val store = IosSqlCipherEncryptedRecordStore(databasePath = path)
        val restored = IosSqlCipherEncryptedRecordStore(databasePath = restoredPath)
        val recordKey = StorageRecordKey("entry")

        try {
            IosSqlCipherEncryptedRecordStore.assertSqlCipherAvailable()
            store.open(key(1))
            store.put(recordKey, byteArrayOf(1, 2, 3, 4))
            val backup = store.exportEncryptedBackup(key(1))

            assertFailsWith<EncryptedStorageException.WrongKey> {
                store.open(key(2))
            }
            assertFailsWith<EncryptedStorageException.WrongKey> {
                restored.restoreEncryptedBackup(key(2), backup)
            }

            restored.restoreEncryptedBackup(key(1), backup)
            assertContentEquals(byteArrayOf(1, 2, 3, 4), restored.get(recordKey))

            restored.rotateKey(currentKey = key(1), newKey = key(3))
            assertFailsWith<EncryptedStorageException.WrongKey> {
                restored.open(key(1))
            }
            restored.open(key(3))
            assertContentEquals(byteArrayOf(1, 2, 3, 4), restored.get(recordKey))
        } finally {
            store.deleteAll()
            restored.deleteAll()
            remove("$path.restore")
            remove("$restoredPath.restore")
        }
    }

    // Real iOS Keychain validation runs in the app-hosted Codemagic simulator step.
    // The standalone Kotlin/Native test binary does not have the same Keychain context.
    @Ignore
    @Test
    fun keychainManagerStoresReplacesAndDeletesDatabaseKey() {
        val service = "org.bettamind.test.${Random.nextInt()}"
        val manager = IosKeychainStorageKeyManager(service = service, account = "sqlcipher-key")
        var keychainAvailable = false
        try {
            val first = keychainOperation("load first database key") {
                manager.loadOrCreateDatabaseKey()
            }
            keychainAvailable = true
            val second = keychainOperation("load existing database key") {
                manager.loadOrCreateDatabaseKey()
            }
            assertContentEquals(first.copyBytes(), second.copyBytes())

            val replacement = key(9)
            keychainOperation("replace database key") {
                manager.replaceDatabaseKey(replacement)
            }
            val afterReplacement = keychainOperation("load replacement database key") {
                manager.loadOrCreateDatabaseKey()
            }
            assertContentEquals(replacement.copyBytes(), afterReplacement.copyBytes())

            keychainOperation("delete database key") {
                manager.deleteDatabaseKey()
            }
            val recreated = keychainOperation("recreate database key") {
                manager.loadOrCreateDatabaseKey()
            }
            val reloaded = keychainOperation("reload recreated database key") {
                manager.loadOrCreateDatabaseKey()
            }
            assertContentEquals(recreated.copyBytes(), reloaded.copyBytes())
        } finally {
            if (keychainAvailable) {
                keychainOperation("cleanup database key") {
                    manager.deleteDatabaseKey()
                }
            }
        }
    }

    @Test
    fun deleteAllRemovesRecordsAndRequiresFreshOpen() {
        val path = temporaryDatabasePath()
        val store = IosSqlCipherEncryptedRecordStore(databasePath = path)
        val recordKey = StorageRecordKey("entry")
        try {
            store.open(key(4))
            store.put(recordKey, byteArrayOf(42))
            store.deleteAll()

            assertFailsWith<EncryptedStorageException.StoreUnavailable> {
                store.get(recordKey)
            }

            store.open(key(4))
            assertNull(store.get(recordKey))
        } finally {
            store.deleteAll()
            remove("$path.restore")
        }
    }

    private fun temporaryDatabasePath(): String =
        "${NSTemporaryDirectory().trimEnd('/')}/bettamind-${Random.nextInt()}.db"

    private fun key(seed: Int): StorageKeyMaterial {
        val random = Random(seed)
        return StorageKeyMaterial.fromBytes(ByteArray(32) { random.nextInt(0, 256).toByte() })
    }

    private fun <T> keychainOperation(operation: String, block: () -> T): T =
        try {
            block()
        } catch (exception: EncryptedStorageException.StoreUnavailable) {
            val message = "iOS Keychain $operation failed: ${exception.cause?.message ?: exception.message}"
            println(message)
            fail(message)
        }
}
