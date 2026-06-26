package org.bettamind.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.bettamind.shared.daily.DailyMetricLevel
import org.bettamind.shared.privacy.EncryptedBackupPackage
import org.bettamind.shared.privacy.EncryptedRecordStore
import org.bettamind.shared.privacy.StorageKeyManager
import org.bettamind.shared.privacy.StorageKeyMaterial
import org.bettamind.shared.privacy.StorageRecordKey

class BettamindAppServicesTest {
    @Test
    fun encryptedDailyRecordServiceSavesCheckInsThroughEncryptedRepository() {
        val service = EncryptedDailyRecordService(
            store = FakeEncryptedRecordStore(),
            keyManager = FakeStorageKeyManager(),
            nowEpochMillis = { 1_800_000_000_000 },
            localDate = { "2027-01-15" },
        )

        assertTrue(service.available())

        val result = service.saveCheckIn(
            mood = DailyMetricLevel.Steady,
            energy = DailyMetricLevel.High,
            stress = DailyMetricLevel.Low,
            sleep = DailyMetricLevel.Steady,
        )

        assertIs<DailyRecordSaveResult.Saved>(result)
        assertEquals(1, result.totalRecords)
        assertEquals(1, service.recordCount())
    }

    @Test
    fun unavailableDailyRecordServiceDoesNotCreateFallbackRecords() {
        val result = UnavailableDailyRecordService.saveCheckIn(
            mood = DailyMetricLevel.Steady,
            energy = DailyMetricLevel.Steady,
            stress = DailyMetricLevel.Steady,
            sleep = DailyMetricLevel.Steady,
        )

        assertFalse(UnavailableDailyRecordService.available())
        assertEquals(DailyRecordSaveResult.StorageUnavailable, result)
        assertEquals(0, UnavailableDailyRecordService.recordCount())
    }
}

private class FakeStorageKeyManager : StorageKeyManager {
    private var key = StorageKeyMaterial.fromBytes(ByteArray(32) { 7 })

    override fun loadOrCreateDatabaseKey(): StorageKeyMaterial =
        StorageKeyMaterial.fromBytes(key.copyBytes())

    override fun replaceDatabaseKey(newKey: StorageKeyMaterial) {
        key.destroy()
        key = StorageKeyMaterial.fromBytes(newKey.copyBytes())
    }

    override fun deleteDatabaseKey() {
        key.destroy()
    }
}

private class FakeEncryptedRecordStore : EncryptedRecordStore {
    private val records = mutableMapOf<String, ByteArray>()
    private var open = false

    override fun open(key: StorageKeyMaterial) {
        open = true
    }

    override fun put(recordKey: StorageRecordKey, value: ByteArray) {
        check(open) { "Store must be open before writes." }
        records[recordKey.value] = value.copyOf()
    }

    override fun get(recordKey: StorageRecordKey): ByteArray? {
        check(open) { "Store must be open before reads." }
        return records[recordKey.value]?.copyOf()
    }

    override fun delete(recordKey: StorageRecordKey) {
        check(open) { "Store must be open before deletes." }
        records.remove(recordKey.value)
    }

    override fun rotateKey(currentKey: StorageKeyMaterial, newKey: StorageKeyMaterial) = Unit

    override fun exportEncryptedBackup(key: StorageKeyMaterial): EncryptedBackupPackage =
        EncryptedBackupPackage(formatVersion = 1, ciphertext = byteArrayOf(1))

    override fun restoreEncryptedBackup(key: StorageKeyMaterial, backup: EncryptedBackupPackage) = Unit

    override fun deleteAll() {
        records.clear()
    }

    override fun close() {
        open = false
    }
}
