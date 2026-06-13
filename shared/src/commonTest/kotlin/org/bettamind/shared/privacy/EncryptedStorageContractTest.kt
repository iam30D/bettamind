package org.bettamind.shared.privacy

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class EncryptedStorageContractTest {
    @Test
    fun wrongKeyIsRejected() {
        val store = ContractOnlyEncryptedRecordStore(key(1))
        store.open(key(1))
        store.put(StorageRecordKey("entry"), byteArrayOf(10, 20, 30))
        store.close()

        assertFailsWith<EncryptedStorageException.WrongKey> {
            store.open(key(2))
        }
    }

    @Test
    fun keyRotationKeepsExistingRecordsAndRejectsOldKey() {
        val store = ContractOnlyEncryptedRecordStore(key(1))
        val recordKey = StorageRecordKey("entry")
        store.open(key(1))
        store.put(recordKey, byteArrayOf(7, 8, 9))

        store.rotateKey(currentKey = key(1), newKey = key(3))

        assertFailsWith<EncryptedStorageException.WrongKey> {
            store.open(key(1))
        }
        store.open(key(3))
        assertContentEquals(byteArrayOf(7, 8, 9), store.get(recordKey))
    }

    @Test
    fun encryptedBackupRestoresWithCorrectKeyOnly() {
        val store = ContractOnlyEncryptedRecordStore(key(4))
        val recordKey = StorageRecordKey("entry")
        store.open(key(4))
        store.put(recordKey, byteArrayOf(1, 2, 3, 4))
        val backup = store.exportEncryptedBackup(key(4))

        val restored = ContractOnlyEncryptedRecordStore(key(4))
        assertFailsWith<EncryptedStorageException.WrongKey> {
            restored.restoreEncryptedBackup(key(5), backup)
        }

        restored.restoreEncryptedBackup(key(4), backup)
        assertContentEquals(byteArrayOf(1, 2, 3, 4), restored.get(recordKey))
    }

    @Test
    fun deletionRemovesRecordsAndRequiresFreshOpen() {
        val store = ContractOnlyEncryptedRecordStore(key(6))
        val recordKey = StorageRecordKey("entry")
        store.open(key(6))
        store.put(recordKey, byteArrayOf(42))

        store.deleteAll()

        assertFailsWith<EncryptedStorageException.StoreUnavailable> {
            store.get(recordKey)
        }
        store.open(key(6))
        assertNull(store.get(recordKey))
    }

    private fun key(seed: Int): StorageKeyMaterial {
        val random = Random(seed)
        return StorageKeyMaterial.fromBytes(ByteArray(32) { random.nextInt(0, 256).toByte() })
    }
}

private class ContractOnlyEncryptedRecordStore(
    initialKey: StorageKeyMaterial,
) : EncryptedRecordStore {
    private var acceptedKey = fingerprint(initialKey)
    private var isOpen = false
    private val records = mutableMapOf<String, ByteArray>()

    override fun open(key: StorageKeyMaterial) {
        if (fingerprint(key) != acceptedKey) {
            throw EncryptedStorageException.WrongKey()
        }
        isOpen = true
    }

    override fun put(recordKey: StorageRecordKey, value: ByteArray) {
        ensureOpen()
        records[recordKey.value] = value.copyOf()
    }

    override fun get(recordKey: StorageRecordKey): ByteArray? {
        ensureOpen()
        return records[recordKey.value]?.copyOf()
    }

    override fun delete(recordKey: StorageRecordKey) {
        ensureOpen()
        records.remove(recordKey.value)
    }

    override fun rotateKey(currentKey: StorageKeyMaterial, newKey: StorageKeyMaterial) {
        open(currentKey)
        acceptedKey = fingerprint(newKey)
    }

    override fun exportEncryptedBackup(key: StorageKeyMaterial): EncryptedBackupPackage {
        open(key)
        val payload = buildString {
            append(acceptedKey)
            append('\n')
            records.keys.sorted().forEach { recordName ->
                val value = records[recordName] ?: error("Missing record for $recordName.")
                append(recordName)
                append('=')
                append(value.joinToString(",") { byte ->
                    val unsigned = byte.toInt()
                    if (unsigned < 0) (unsigned + 256).toString() else unsigned.toString()
                })
                append('\n')
            }
        }.encodeToByteArray()
        return EncryptedBackupPackage(formatVersion = 1, ciphertext = payload)
    }

    override fun restoreEncryptedBackup(key: StorageKeyMaterial, backup: EncryptedBackupPackage) {
        val lines = backup.ciphertext.decodeToString().lines()
        if (lines.firstOrNull() != fingerprint(key).toString()) {
            throw EncryptedStorageException.WrongKey()
        }
        records.clear()
        lines.drop(1).filter { it.isNotBlank() }.forEach { line ->
            val name = line.substringBefore('=')
            val values = line.substringAfter('=')
                .split(',')
                .filter { it.isNotBlank() }
                .map { it.toInt().toByte() }
                .toByteArray()
            records[name] = values
        }
        acceptedKey = fingerprint(key)
        isOpen = true
    }

    override fun deleteAll() {
        records.clear()
        isOpen = false
    }

    override fun close() {
        isOpen = false
    }

    private fun ensureOpen() {
        if (!isOpen) {
            throw EncryptedStorageException.StoreUnavailable()
        }
    }

    private fun fingerprint(key: StorageKeyMaterial): Int =
        key.copyBytes().fold(17) { accumulator, byte -> accumulator * 31 + byte }
}
