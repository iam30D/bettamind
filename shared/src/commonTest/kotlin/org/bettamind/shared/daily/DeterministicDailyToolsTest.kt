package org.bettamind.shared.daily

import org.bettamind.shared.privacy.EncryptedBackupPackage
import org.bettamind.shared.privacy.EncryptedRecordStore
import org.bettamind.shared.privacy.EncryptedStorageException
import org.bettamind.shared.privacy.StorageKeyMaterial
import org.bettamind.shared.privacy.StorageRecordKey
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeterministicDailyToolsTest {
    @Test
    fun checkInsAreStoredOnlyThroughOpenedEncryptedRecords() {
        val store = FakeEncryptedRecordStore(key(1))
        val repository = EncryptedDailyRecordRepository(store)
        val record = DailyToolRecordFactory.checkIn(
            id = "checkin-2026-06-18",
            createdAtEpochMillis = 10_000L,
            checkIn = sampleCheckIn("2026-06-18"),
        )

        assertFailsWith<EncryptedStorageException.StoreUnavailable> {
            repository.save(record)
        }

        store.open(key(1))
        repository.save(record)

        val restored = repository.get("checkin-2026-06-18")
        assertEquals(record, restored)
        assertEquals(listOf(record), repository.list())
        assertTrue(store.writtenKeys.all { it.startsWith("daily/v1/") })
    }

    @Test
    fun encryptedBackupAndRestoreRoundTripDailyRecordsWithCorrectKeyOnly() {
        val store = FakeEncryptedRecordStore(key(2))
        val repository = EncryptedDailyRecordRepository(store)
        store.open(key(2))
        repository.save(
            DailyToolRecordFactory.checkIn(
                id = "checkin-a",
                createdAtEpochMillis = 1L,
                checkIn = sampleCheckIn("2026-06-18"),
            ),
        )

        val backup = repository.exportEncryptedBackup(key(2))
        val restoredStore = FakeEncryptedRecordStore(key(2))
        val restoredRepository = EncryptedDailyRecordRepository(restoredStore)

        assertFailsWith<EncryptedStorageException.WrongKey> {
            restoredRepository.restoreEncryptedBackup(key(3), backup)
        }

        restoredRepository.restoreEncryptedBackup(key(2), backup)
        assertNotNull(restoredRepository.get("checkin-a")?.checkIn)
    }

    @Test
    fun privacyPolicyForDailyToolsForbidsFallbacksBackendAiRankingAndWorthScores() {
        assertTrue(DailyToolPrivacyPolicy.requiresEncryptedStorage(DailyToolRecordKind.CheckIn))
        assertFalse(DailyToolPrivacyPolicy.allowsUnencryptedFallback())
        assertFalse(DailyToolPrivacyPolicy.allowsBackendSyncByDefault())
        assertFalse(DailyToolPrivacyPolicy.requiresAi())
        assertFalse(DailyToolPrivacyPolicy.allowsPublicRanking())
        assertFalse(DailyToolPrivacyPolicy.allowsManipulativeStreaks())
        assertFalse(DailyToolPrivacyPolicy.producesHumanWorthScore())
    }

    @Test
    fun breathingGroundingAndTimersAreDeterministic() {
        val breathing = BreathingExerciseCatalog.boxBreathing()
        val grounding = GroundingExerciseCatalog.fiveFourThreeTwoOne()
        val timer = DailyTimerRecord(
            startedAtEpochMillis = 1_000L,
            durationMillis = 60_000L,
            purpose = DailyTimerPurpose.Breathing,
        )

        assertEquals(16, breathing.cycleDurationSeconds)
        assertEquals(
            listOf(BreathingPhase.Inhale, BreathingPhase.Hold, BreathingPhase.Exhale, BreathingPhase.Rest),
            breathing.steps.map { it.phase },
        )
        assertEquals(listOf(5, 4, 3, 2, 1), grounding.map { it.count })

        assertEquals(
            DailyTimerRecovery(
                elapsedMillis = 20_000L,
                remainingMillis = 40_000L,
                complete = false,
            ),
            DailyTimerEngine.recover(timer, nowEpochMillis = 21_000L),
        )
        assertEquals(
            DailyTimerRecovery(
                elapsedMillis = 60_000L,
                remainingMillis = 0L,
                complete = true,
            ),
            DailyTimerEngine.recover(timer, nowEpochMillis = 90_000L),
        )
    }

    @Test
    fun localReminderPolicyUsesNeutralCopyAndHonoursQuietHoursPauseAndSnooze() {
        val settings = DailyReminderSettings(enabled = true)

        val send = DailyReminderPolicy.nextDecision(
            settings = settings,
            nowEpochMillis = 10_000L,
            currentMinuteOfDay = MinuteOfDay(9 * 60),
        )
        assertEquals(DailyReminderAction.SendNeutralReminder, send.action)
        assertEquals(DailyReminderPreview.NeutralPrivateReminder, send.preview)
        assertEquals("daily_reminder_neutral_preview", DailyReminderPolicy.neutralPreviewTextKey())
        assertFalse(DailyReminderPolicy.allowsPersonalDetailsOnLockScreen())

        val quiet = DailyReminderPolicy.nextDecision(
            settings = settings,
            nowEpochMillis = 10_000L,
            currentMinuteOfDay = MinuteOfDay(22 * 60),
        )
        assertEquals(DailyReminderSuppressionReason.QuietHours, quiet.suppressionReason)

        val paused = DailyReminderPolicy.nextDecision(
            settings = settings.copy(pauseAllUntilEpochMillis = 20_000L),
            nowEpochMillis = 10_000L,
            currentMinuteOfDay = MinuteOfDay(9 * 60),
        )
        assertEquals(DailyReminderSuppressionReason.Paused, paused.suppressionReason)

        val snoozed = DailyReminderPolicy.nextDecision(
            settings = settings.copy(snoozedUntilEpochMillis = 20_000L),
            nowEpochMillis = 10_000L,
            currentMinuteOfDay = MinuteOfDay(9 * 60),
        )
        assertEquals(DailyReminderSuppressionReason.Snoozed, snoozed.suppressionReason)
    }

    @Test
    fun privateCalendarHandoffRequiresExplicitApprovalAndNeverReadsBroadCalendarData() {
        val entry = PrivateCalendarEntry(
            localDate = "2026-06-18",
            titleKey = "daily_calendar_checkin_title",
            startMinuteOfDay = MinuteOfDay(8 * 60),
            durationMinutes = 10,
            systemHandoffRequested = true,
        )

        val withoutApproval = CalendarHandoffPolicy.evaluate(entry, explicitUserApproval = false)
        val withApproval = CalendarHandoffPolicy.evaluate(entry, explicitUserApproval = true)

        assertTrue(withoutApproval.keepPrivateInApp)
        assertFalse(withoutApproval.offerSystemCalendarHandoff)
        assertTrue(withApproval.offerSystemCalendarHandoff)
        assertFalse(withApproval.readsSystemCalendar)
        assertFalse(withApproval.includesPrivateNotes)
        assertFalse(CalendarHandoffPolicy.readsSystemCalendarByDefault())
    }

    @Test
    fun worksheetsUseDeterministicPromptTemplatesWithoutAi() {
        val template = DecisionWorksheetCatalog.template(DecisionWorksheetKind.ValuesToAction)
        val worksheet = DecisionWorksheetRecord(
            kind = DecisionWorksheetKind.ValuesToAction,
            completedPromptKeys = template.promptKeys.take(2),
        )
        val record = DailyToolRecordFactory.worksheet(
            id = "worksheet-values",
            createdAtEpochMillis = 20_000L,
            worksheet = worksheet,
        )

        assertEquals(DecisionWorksheetKind.ValuesToAction, template.kind)
        assertEquals(3, template.promptKeys.size)
        assertEquals(worksheet, record.worksheet)
        assertFalse(DailyToolPrivacyPolicy.requiresAi())
    }

    @Test
    fun trendSummaryIsLocalDeterministicAndNotAWorthScore() {
        val summary = DailyTrendSummarizer.summarize(
            listOf(
                sampleCheckIn("2026-06-18", stress = DailyMetricLevel.High),
                sampleCheckIn("2026-06-19", mood = DailyMetricLevel.High, stress = DailyMetricLevel.VeryHigh),
            ),
        )

        assertEquals(2, summary.checkInCount)
        assertEquals(3.5, summary.averageMood)
        assertEquals(4.5, summary.averageStress)
        assertEquals(listOf("2026-06-19"), summary.highestStressLocalDates)
        assertFalse(summary.usesAi)
        assertFalse(summary.producesHumanWorthScore)
    }

    @Test
    fun recordValidationRejectsInvalidDatesAndIds() {
        assertFailsWith<IllegalArgumentException> {
            sampleCheckIn("06-18-2026")
        }
        assertFailsWith<IllegalArgumentException> {
            DailyToolRecordFactory.checkIn(
                id = "../not-safe",
                createdAtEpochMillis = 1L,
                checkIn = sampleCheckIn("2026-06-18"),
            )
        }
    }

    private fun sampleCheckIn(
        localDate: String,
        mood: DailyMetricLevel = DailyMetricLevel.Steady,
        energy: DailyMetricLevel = DailyMetricLevel.Low,
        stress: DailyMetricLevel = DailyMetricLevel.High,
        sleep: DailyMetricLevel = DailyMetricLevel.Steady,
    ): DailyCheckInRecord =
        DailyCheckInRecord(
            localDate = localDate,
            mood = mood,
            energy = energy,
            stress = stress,
            sleep = sleep,
        )

    private fun key(seed: Int): StorageKeyMaterial {
        val random = Random(seed)
        return StorageKeyMaterial.fromBytes(ByteArray(32) { random.nextInt(0, 256).toByte() })
    }
}

private class FakeEncryptedRecordStore(
    initialKey: StorageKeyMaterial,
) : EncryptedRecordStore {
    private var acceptedKey = fingerprint(initialKey)
    private var isOpen = false
    private val records = mutableMapOf<String, ByteArray>()
    val writtenKeys = mutableListOf<String>()

    override fun open(key: StorageKeyMaterial) {
        if (fingerprint(key) != acceptedKey) {
            throw EncryptedStorageException.WrongKey()
        }
        isOpen = true
    }

    override fun put(recordKey: StorageRecordKey, value: ByteArray) {
        ensureOpen()
        writtenKeys += recordKey.value
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
                append(recordName)
                append('=')
                append(records.getValue(recordName).joinToString(",") { byte ->
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
            val value = line.substringAfter('=')
                .split(',')
                .filter { it.isNotBlank() }
                .map { it.toInt().toByte() }
                .toByteArray()
            records[name] = value
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
