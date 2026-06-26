package org.bettamind.shared

import org.bettamind.shared.daily.DailyCheckInRecord
import org.bettamind.shared.daily.DailyMetricLevel
import org.bettamind.shared.daily.DailyToolRecordFactory
import org.bettamind.shared.daily.EncryptedDailyRecordRepository
import org.bettamind.shared.ai.BettamindModelPackTrustPolicy
import org.bettamind.shared.privacy.EncryptedRecordStore
import org.bettamind.shared.privacy.StorageKeyManager

data class BettamindAppServices(
    val dailyRecords: DailyRecordService = UnavailableDailyRecordService,
    val reminders: ReminderPlatformService = UnavailableReminderPlatformService,
    val calendar: CalendarPlatformService = UnavailableCalendarPlatformService,
    val speech: SpeechPlatformService = UnavailableSpeechPlatformService,
    val modelPacks: ModelPackPlatformService = UnavailableModelPackPlatformService,
)

sealed interface DailyRecordSaveResult {
    data class Saved(val recordId: String, val totalRecords: Int) : DailyRecordSaveResult
    data object StorageUnavailable : DailyRecordSaveResult
    data object Failed : DailyRecordSaveResult
}

interface DailyRecordService {
    fun available(): Boolean
    fun saveCheckIn(
        mood: DailyMetricLevel,
        energy: DailyMetricLevel,
        stress: DailyMetricLevel,
        sleep: DailyMetricLevel,
    ): DailyRecordSaveResult
    fun recordCount(): Int
}

class EncryptedDailyRecordService(
    private val store: EncryptedRecordStore,
    private val keyManager: StorageKeyManager,
    private val nowEpochMillis: () -> Long,
    private val localDate: () -> String,
) : DailyRecordService {
    private var sequence: Int = 0

    override fun available(): Boolean =
        runCatching {
            withRepository { it.list() }
        }.isSuccess

    override fun saveCheckIn(
        mood: DailyMetricLevel,
        energy: DailyMetricLevel,
        stress: DailyMetricLevel,
        sleep: DailyMetricLevel,
    ): DailyRecordSaveResult {
        val createdAt = nowEpochMillis()
        val id = "daily-$createdAt-${sequence++}"
        val record = DailyToolRecordFactory.checkIn(
            id = id,
            createdAtEpochMillis = createdAt,
            checkIn = DailyCheckInRecord(
                localDate = localDate(),
                mood = mood,
                energy = energy,
                stress = stress,
                sleep = sleep,
            ),
        )
        return runCatching {
            withRepository { repository ->
                repository.save(record)
                DailyRecordSaveResult.Saved(
                    recordId = id,
                    totalRecords = repository.list().size,
                )
            }
        }.getOrElse {
            DailyRecordSaveResult.Failed
        }
    }

    override fun recordCount(): Int =
        runCatching {
            withRepository { it.list().size }
        }.getOrDefault(0)

    private fun <T> withRepository(block: (EncryptedDailyRecordRepository) -> T): T {
        val key = keyManager.loadOrCreateDatabaseKey()
        return try {
            store.open(key)
            block(EncryptedDailyRecordRepository(store))
        } finally {
            key.destroy()
            store.close()
        }
    }
}

object UnavailableDailyRecordService : DailyRecordService {
    override fun available(): Boolean = false

    override fun saveCheckIn(
        mood: DailyMetricLevel,
        energy: DailyMetricLevel,
        stress: DailyMetricLevel,
        sleep: DailyMetricLevel,
    ): DailyRecordSaveResult = DailyRecordSaveResult.StorageUnavailable

    override fun recordCount(): Int = 0
}

data class ReminderPlatformStatus(
    val available: Boolean,
    val neutralPreviewOnly: Boolean,
    val platformName: String,
)

interface ReminderPlatformService {
    fun status(): ReminderPlatformStatus
}

object UnavailableReminderPlatformService : ReminderPlatformService {
    override fun status(): ReminderPlatformStatus =
        ReminderPlatformStatus(
            available = false,
            neutralPreviewOnly = true,
            platformName = "unavailable",
        )
}

data class CalendarPlatformStatus(
    val available: Boolean,
    val explicitHandoffOnly: Boolean,
    val readsBroadCalendarData: Boolean,
    val platformName: String,
)

interface CalendarPlatformService {
    fun status(): CalendarPlatformStatus
}

object UnavailableCalendarPlatformService : CalendarPlatformService {
    override fun status(): CalendarPlatformStatus =
        CalendarPlatformStatus(
            available = false,
            explicitHandoffOnly = true,
            readsBroadCalendarData = false,
            platformName = "unavailable",
        )
}

data class SpeechPlatformStatus(
    val available: Boolean,
    val osOfflinePreferred: Boolean,
    val requiresExplicitMicrophonePermission: Boolean,
    val textFallbackAvailable: Boolean,
    val platformName: String,
)

interface SpeechPlatformService {
    fun status(): SpeechPlatformStatus
}

object UnavailableSpeechPlatformService : SpeechPlatformService {
    override fun status(): SpeechPlatformStatus =
        SpeechPlatformStatus(
            available = false,
            osOfflinePreferred = true,
            requiresExplicitMicrophonePermission = true,
            textFallbackAvailable = true,
            platformName = "unavailable",
        )
}

data class ModelPackPlatformStatus(
    val installerAvailable: Boolean,
    val runtimeAvailable: Boolean,
    val firstModelId: String,
    val requiresSignedManifest: Boolean,
    val autoDownloadDisabled: Boolean,
)

interface ModelPackPlatformService {
    fun status(): ModelPackPlatformStatus
}

object UnavailableModelPackPlatformService : ModelPackPlatformService {
    override fun status(): ModelPackPlatformStatus =
        ModelPackPlatformStatus(
            installerAvailable = false,
            runtimeAvailable = false,
            firstModelId = BettamindModelPackTrustPolicy.firstProductionModelId,
            requiresSignedManifest = true,
            autoDownloadDisabled = true,
        )
}
