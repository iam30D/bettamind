package org.bettamind.shared.daily

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bettamind.shared.privacy.EncryptedBackupPackage
import org.bettamind.shared.privacy.EncryptedRecordStore
import org.bettamind.shared.privacy.StorageKeyMaterial
import org.bettamind.shared.privacy.StorageRecordKey

private const val DailyRecordSchemaVersion = 1
private val SafeIdPattern = Regex("[A-Za-z0-9._:-]+")

@Serializable
enum class DailyMetricLevel(val score: Int) {
    VeryLow(1),
    Low(2),
    Steady(3),
    High(4),
    VeryHigh(5),
}

@Serializable
data class DailyCheckInRecord(
    val localDate: String,
    val mood: DailyMetricLevel,
    val energy: DailyMetricLevel,
    val stress: DailyMetricLevel,
    val sleep: DailyMetricLevel,
) {
    init {
        require(localDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            "Daily check-in dates must use local YYYY-MM-DD form."
        }
    }
}

@Serializable
enum class DailyToolRecordKind {
    CheckIn,
    Timer,
    CalendarEntry,
    Worksheet,
}

@Serializable
data class DailyToolRecord(
    val id: String,
    val kind: DailyToolRecordKind,
    val createdAtEpochMillis: Long,
    val schemaVersion: Int = DailyRecordSchemaVersion,
    val checkIn: DailyCheckInRecord? = null,
    val timer: DailyTimerRecord? = null,
    val calendarEntry: PrivateCalendarEntry? = null,
    val worksheet: DecisionWorksheetRecord? = null,
) {
    init {
        require(id.isNotBlank() && id.matches(SafeIdPattern)) {
            "Daily record IDs must be non-empty and local-safe."
        }
        require(createdAtEpochMillis >= 0) { "Daily record timestamps must be non-negative." }
        require(schemaVersion == DailyRecordSchemaVersion) {
            "Unsupported daily record schema version $schemaVersion."
        }
        when (kind) {
            DailyToolRecordKind.CheckIn -> require(checkIn != null && timer == null && calendarEntry == null && worksheet == null)
            DailyToolRecordKind.Timer -> require(timer != null && checkIn == null && calendarEntry == null && worksheet == null)
            DailyToolRecordKind.CalendarEntry -> require(calendarEntry != null && checkIn == null && timer == null && worksheet == null)
            DailyToolRecordKind.Worksheet -> require(worksheet != null && checkIn == null && timer == null && calendarEntry == null)
        }
    }
}

object DailyToolRecordFactory {
    fun checkIn(
        id: String,
        createdAtEpochMillis: Long,
        checkIn: DailyCheckInRecord,
    ): DailyToolRecord =
        DailyToolRecord(
            id = id,
            kind = DailyToolRecordKind.CheckIn,
            createdAtEpochMillis = createdAtEpochMillis,
            checkIn = checkIn,
        )

    fun timer(
        id: String,
        createdAtEpochMillis: Long,
        timer: DailyTimerRecord,
    ): DailyToolRecord =
        DailyToolRecord(
            id = id,
            kind = DailyToolRecordKind.Timer,
            createdAtEpochMillis = createdAtEpochMillis,
            timer = timer,
        )

    fun calendarEntry(
        id: String,
        createdAtEpochMillis: Long,
        calendarEntry: PrivateCalendarEntry,
    ): DailyToolRecord =
        DailyToolRecord(
            id = id,
            kind = DailyToolRecordKind.CalendarEntry,
            createdAtEpochMillis = createdAtEpochMillis,
            calendarEntry = calendarEntry,
        )

    fun worksheet(
        id: String,
        createdAtEpochMillis: Long,
        worksheet: DecisionWorksheetRecord,
    ): DailyToolRecord =
        DailyToolRecord(
            id = id,
            kind = DailyToolRecordKind.Worksheet,
            createdAtEpochMillis = createdAtEpochMillis,
            worksheet = worksheet,
        )
}

@Serializable
internal data class DailyRecordIndex(
    val entries: List<DailyRecordIndexEntry> = emptyList(),
)

@Serializable
internal data class DailyRecordIndexEntry(
    val id: String,
    val kind: DailyToolRecordKind,
    val createdAtEpochMillis: Long,
)

object DailyToolRecordCodec {
    val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    fun encode(record: DailyToolRecord): ByteArray =
        json.encodeToString(record).encodeToByteArray()

    fun decode(bytes: ByteArray): DailyToolRecord =
        json.decodeFromString(bytes.decodeToString())

    private fun encodeIndex(index: DailyRecordIndex): ByteArray =
        json.encodeToString(index).encodeToByteArray()

    private fun decodeIndex(bytes: ByteArray): DailyRecordIndex =
        json.decodeFromString(bytes.decodeToString())

    internal fun indexBytes(index: DailyRecordIndex): ByteArray = encodeIndex(index)

    internal fun indexFromBytes(bytes: ByteArray): DailyRecordIndex = decodeIndex(bytes)
}

class EncryptedDailyRecordRepository(
    private val store: EncryptedRecordStore,
) {
    fun save(record: DailyToolRecord) {
        store.put(record.storageKey(), DailyToolRecordCodec.encode(record))
        upsertIndex(record)
    }

    fun get(id: String): DailyToolRecord? =
        store.get(storageKeyFor(id))?.let(DailyToolRecordCodec::decode)

    fun list(): List<DailyToolRecord> =
        readIndex().entries
            .sortedWith(compareBy<DailyRecordIndexEntry> { it.createdAtEpochMillis }.thenBy { it.id })
            .mapNotNull { get(it.id) }

    fun delete(id: String) {
        store.delete(storageKeyFor(id))
        val next = readIndex().copy(entries = readIndex().entries.filterNot { it.id == id })
        store.put(IndexRecordKey, DailyToolRecordCodec.indexBytes(next))
    }

    fun exportEncryptedBackup(key: StorageKeyMaterial): EncryptedBackupPackage =
        store.exportEncryptedBackup(key)

    fun restoreEncryptedBackup(
        key: StorageKeyMaterial,
        backup: EncryptedBackupPackage,
    ) {
        store.restoreEncryptedBackup(key, backup)
    }

    private fun upsertIndex(record: DailyToolRecord) {
        val current = readIndex()
        val entry = DailyRecordIndexEntry(
            id = record.id,
            kind = record.kind,
            createdAtEpochMillis = record.createdAtEpochMillis,
        )
        val next = current.copy(
            entries = (current.entries.filterNot { it.id == record.id } + entry)
                .sortedWith(compareBy<DailyRecordIndexEntry> { it.createdAtEpochMillis }.thenBy { it.id }),
        )
        store.put(IndexRecordKey, DailyToolRecordCodec.indexBytes(next))
    }

    private fun readIndex(): DailyRecordIndex =
        store.get(IndexRecordKey)?.let(DailyToolRecordCodec::indexFromBytes) ?: DailyRecordIndex()

    private fun DailyToolRecord.storageKey(): StorageRecordKey = storageKeyFor(id)

    private fun storageKeyFor(id: String): StorageRecordKey {
        require(id.isNotBlank() && id.matches(SafeIdPattern)) {
            "Daily record IDs must be non-empty and local-safe."
        }
        return StorageRecordKey("$DailyRecordNamespace/records/$id")
    }

    private companion object {
        const val DailyRecordNamespace = "daily/v1"
        val IndexRecordKey = StorageRecordKey("$DailyRecordNamespace/index")
    }
}

enum class DailyToolPrivacyBoundary {
    RequiresEncryptedStorage,
    NoUnencryptedFallback,
    NoBackendSyncByDefault,
    NoAiDependency,
    NoPublicRanking,
    NoManipulativeStreak,
    NoHumanWorthScore,
}

object DailyToolPrivacyPolicy {
    val boundaries: Set<DailyToolPrivacyBoundary> = DailyToolPrivacyBoundary.entries.toSet()

    fun requiresEncryptedStorage(kind: DailyToolRecordKind): Boolean = true
    fun allowsUnencryptedFallback(): Boolean = false
    fun allowsBackendSyncByDefault(): Boolean = false
    fun requiresAi(): Boolean = false
    fun allowsPublicRanking(): Boolean = false
    fun allowsManipulativeStreaks(): Boolean = false
    fun producesHumanWorthScore(): Boolean = false
}

@Serializable
data class BreathingStep(
    val phase: BreathingPhase,
    val durationSeconds: Int,
    val instructionKey: String,
) {
    init {
        require(durationSeconds > 0) { "Breathing step durations must be positive." }
        require(instructionKey.isNotBlank()) { "Breathing instruction keys cannot be blank." }
    }
}

@Serializable
enum class BreathingPhase {
    Inhale,
    Hold,
    Exhale,
    Rest,
}

@Serializable
data class BreathingExercise(
    val id: String,
    val titleKey: String,
    val steps: List<BreathingStep>,
) {
    init {
        require(id.isNotBlank() && id.matches(SafeIdPattern)) { "Breathing exercise IDs must be local-safe." }
        require(titleKey.isNotBlank()) { "Breathing title keys cannot be blank." }
        require(steps.isNotEmpty()) { "Breathing exercises must have at least one step." }
    }

    val cycleDurationSeconds: Int
        get() = steps.sumOf { it.durationSeconds }
}

object BreathingExerciseCatalog {
    fun boxBreathing(): BreathingExercise =
        BreathingExercise(
            id = "box-breathing-4",
            titleKey = "daily_breathing_box_title",
            steps = listOf(
                BreathingStep(BreathingPhase.Inhale, 4, "daily_breathing_inhale"),
                BreathingStep(BreathingPhase.Hold, 4, "daily_breathing_hold"),
                BreathingStep(BreathingPhase.Exhale, 4, "daily_breathing_exhale"),
                BreathingStep(BreathingPhase.Rest, 4, "daily_breathing_rest"),
            ),
        )
}

@Serializable
enum class GroundingSense {
    See,
    Touch,
    Hear,
    Smell,
    Taste,
}

@Serializable
data class GroundingStep(
    val sense: GroundingSense,
    val count: Int,
    val promptKey: String,
) {
    init {
        require(count > 0) { "Grounding counts must be positive." }
        require(promptKey.isNotBlank()) { "Grounding prompt keys cannot be blank." }
    }
}

object GroundingExerciseCatalog {
    fun fiveFourThreeTwoOne(): List<GroundingStep> =
        listOf(
            GroundingStep(GroundingSense.See, 5, "daily_grounding_see"),
            GroundingStep(GroundingSense.Touch, 4, "daily_grounding_touch"),
            GroundingStep(GroundingSense.Hear, 3, "daily_grounding_hear"),
            GroundingStep(GroundingSense.Smell, 2, "daily_grounding_smell"),
            GroundingStep(GroundingSense.Taste, 1, "daily_grounding_taste"),
        )
}

@Serializable
data class DailyTimerRecord(
    val startedAtEpochMillis: Long,
    val durationMillis: Long,
    val purpose: DailyTimerPurpose,
) {
    init {
        require(startedAtEpochMillis >= 0) { "Timer start must be non-negative." }
        require(durationMillis > 0) { "Timer duration must be positive." }
    }
}

@Serializable
enum class DailyTimerPurpose {
    Grounding,
    Breathing,
    Reflection,
}

data class DailyTimerRecovery(
    val elapsedMillis: Long,
    val remainingMillis: Long,
    val complete: Boolean,
)

object DailyTimerEngine {
    fun recover(
        timer: DailyTimerRecord,
        nowEpochMillis: Long,
    ): DailyTimerRecovery {
        require(nowEpochMillis >= timer.startedAtEpochMillis) {
            "Timer recovery cannot run before the timer start."
        }
        val elapsed = nowEpochMillis - timer.startedAtEpochMillis
        val remaining = (timer.durationMillis - elapsed).coerceAtLeast(0)
        return DailyTimerRecovery(
            elapsedMillis = elapsed.coerceAtMost(timer.durationMillis),
            remainingMillis = remaining,
            complete = remaining == 0L,
        )
    }
}

@Serializable
data class MinuteOfDay(val value: Int) {
    init {
        require(value in 0 until MinutesPerDay) { "Minute of day must be between 0 and 1439." }
    }

    companion object {
        const val MinutesPerDay = 24 * 60
    }
}

@Serializable
data class QuietHours(
    val start: MinuteOfDay,
    val end: MinuteOfDay,
) {
    fun contains(minuteOfDay: MinuteOfDay): Boolean {
        if (start == end) return false
        return if (start.value < end.value) {
            minuteOfDay.value in start.value until end.value
        } else {
            minuteOfDay.value >= start.value || minuteOfDay.value < end.value
        }
    }
}

@Serializable
data class DailyReminderSettings(
    val enabled: Boolean = false,
    val reminderMinuteOfDay: MinuteOfDay = MinuteOfDay(9 * 60),
    val quietHours: QuietHours = QuietHours(
        start = MinuteOfDay(21 * 60),
        end = MinuteOfDay(7 * 60),
    ),
    val pauseAllUntilEpochMillis: Long? = null,
    val snoozedUntilEpochMillis: Long? = null,
) {
    init {
        require(pauseAllUntilEpochMillis == null || pauseAllUntilEpochMillis >= 0) {
            "Pause-all timestamps must be non-negative."
        }
        require(snoozedUntilEpochMillis == null || snoozedUntilEpochMillis >= 0) {
            "Snooze timestamps must be non-negative."
        }
    }
}

enum class DailyReminderAction {
    SendNeutralReminder,
    Suppress,
}

enum class DailyReminderSuppressionReason {
    Disabled,
    Paused,
    Snoozed,
    QuietHours,
}

enum class DailyReminderPreview {
    NeutralPrivateReminder,
}

data class DailyReminderDecision(
    val action: DailyReminderAction,
    val preview: DailyReminderPreview? = null,
    val suppressionReason: DailyReminderSuppressionReason? = null,
) {
    init {
        if (action == DailyReminderAction.SendNeutralReminder) {
            require(preview != null && suppressionReason == null)
        } else {
            require(preview == null && suppressionReason != null)
        }
    }
}

object DailyReminderPolicy {
    fun nextDecision(
        settings: DailyReminderSettings,
        nowEpochMillis: Long,
        currentMinuteOfDay: MinuteOfDay,
    ): DailyReminderDecision {
        if (!settings.enabled) {
            return suppressed(DailyReminderSuppressionReason.Disabled)
        }
        if (settings.pauseAllUntilEpochMillis != null && nowEpochMillis < settings.pauseAllUntilEpochMillis) {
            return suppressed(DailyReminderSuppressionReason.Paused)
        }
        if (settings.snoozedUntilEpochMillis != null && nowEpochMillis < settings.snoozedUntilEpochMillis) {
            return suppressed(DailyReminderSuppressionReason.Snoozed)
        }
        if (settings.quietHours.contains(currentMinuteOfDay)) {
            return suppressed(DailyReminderSuppressionReason.QuietHours)
        }
        return DailyReminderDecision(
            action = DailyReminderAction.SendNeutralReminder,
            preview = DailyReminderPreview.NeutralPrivateReminder,
        )
    }

    fun neutralPreviewTextKey(): String = "daily_reminder_neutral_preview"

    fun allowsPersonalDetailsOnLockScreen(): Boolean = false

    private fun suppressed(reason: DailyReminderSuppressionReason): DailyReminderDecision =
        DailyReminderDecision(
            action = DailyReminderAction.Suppress,
            suppressionReason = reason,
        )
}

@Serializable
data class PrivateCalendarEntry(
    val localDate: String,
    val titleKey: String,
    val startMinuteOfDay: MinuteOfDay,
    val durationMinutes: Int,
    val systemHandoffRequested: Boolean = false,
) {
    init {
        require(localDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            "Calendar entries must use local YYYY-MM-DD form."
        }
        require(titleKey.isNotBlank()) { "Calendar entry title keys cannot be blank." }
        require(durationMinutes > 0) { "Calendar entry duration must be positive." }
    }
}

data class CalendarHandoffDecision(
    val keepPrivateInApp: Boolean,
    val offerSystemCalendarHandoff: Boolean,
    val readsSystemCalendar: Boolean = false,
    val includesPrivateNotes: Boolean = false,
)

object CalendarHandoffPolicy {
    fun evaluate(
        entry: PrivateCalendarEntry,
        explicitUserApproval: Boolean,
    ): CalendarHandoffDecision =
        CalendarHandoffDecision(
            keepPrivateInApp = true,
            offerSystemCalendarHandoff = entry.systemHandoffRequested && explicitUserApproval,
        )

    fun readsSystemCalendarByDefault(): Boolean = false
}

@Serializable
enum class DecisionWorksheetKind {
    ValuesToAction,
    ProblemSolving,
    RepairPreparation,
    DifficultConversation,
}

@Serializable
data class DecisionWorksheetRecord(
    val kind: DecisionWorksheetKind,
    val completedPromptKeys: List<String>,
) {
    init {
        require(completedPromptKeys.all { it.isNotBlank() }) {
            "Worksheet prompt keys cannot be blank."
        }
    }
}

data class DecisionWorksheetTemplate(
    val kind: DecisionWorksheetKind,
    val promptKeys: List<String>,
) {
    init {
        require(promptKeys.isNotEmpty()) { "Worksheet templates need at least one prompt." }
    }
}

object DecisionWorksheetCatalog {
    fun template(kind: DecisionWorksheetKind): DecisionWorksheetTemplate =
        when (kind) {
            DecisionWorksheetKind.ValuesToAction -> DecisionWorksheetTemplate(
                kind = kind,
                promptKeys = listOf(
                    "daily_worksheet_values_prompt",
                    "daily_worksheet_action_prompt",
                    "daily_worksheet_next_step_prompt",
                ),
            )

            DecisionWorksheetKind.ProblemSolving -> DecisionWorksheetTemplate(
                kind = kind,
                promptKeys = listOf(
                    "daily_worksheet_problem_prompt",
                    "daily_worksheet_options_prompt",
                    "daily_worksheet_first_step_prompt",
                ),
            )

            DecisionWorksheetKind.RepairPreparation -> DecisionWorksheetTemplate(
                kind = kind,
                promptKeys = listOf(
                    "daily_worksheet_repair_effect_prompt",
                    "daily_worksheet_repair_need_prompt",
                    "daily_worksheet_repair_action_prompt",
                ),
            )

            DecisionWorksheetKind.DifficultConversation -> DecisionWorksheetTemplate(
                kind = kind,
                promptKeys = listOf(
                    "daily_worksheet_conversation_goal_prompt",
                    "daily_worksheet_conversation_boundary_prompt",
                    "daily_worksheet_conversation_next_prompt",
                ),
            )
        }
}

data class DailyTrendSummary(
    val checkInCount: Int,
    val averageMood: Double,
    val averageEnergy: Double,
    val averageStress: Double,
    val averageSleep: Double,
    val highestStressLocalDates: List<String>,
    val usesAi: Boolean = false,
    val producesHumanWorthScore: Boolean = false,
)

object DailyTrendSummarizer {
    fun summarize(checkIns: List<DailyCheckInRecord>): DailyTrendSummary {
        if (checkIns.isEmpty()) {
            return DailyTrendSummary(
                checkInCount = 0,
                averageMood = 0.0,
                averageEnergy = 0.0,
                averageStress = 0.0,
                averageSleep = 0.0,
                highestStressLocalDates = emptyList(),
            )
        }

        val highestStress = checkIns.maxOf { it.stress.score }
        return DailyTrendSummary(
            checkInCount = checkIns.size,
            averageMood = checkIns.averageOf { it.mood.score },
            averageEnergy = checkIns.averageOf { it.energy.score },
            averageStress = checkIns.averageOf { it.stress.score },
            averageSleep = checkIns.averageOf { it.sleep.score },
            highestStressLocalDates = checkIns
                .filter { it.stress.score == highestStress }
                .map { it.localDate }
                .sorted(),
        )
    }

    private fun List<DailyCheckInRecord>.averageOf(selector: (DailyCheckInRecord) -> Int): Double =
        sumOf(selector).toDouble() / size.toDouble()
}
