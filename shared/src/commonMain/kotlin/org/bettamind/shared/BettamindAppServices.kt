package org.bettamind.shared

import org.bettamind.shared.daily.DailyCheckInRecord
import org.bettamind.shared.daily.DailyMetricLevel
import org.bettamind.shared.daily.DailyToolRecordFactory
import org.bettamind.shared.daily.EncryptedDailyRecordRepository
import org.bettamind.shared.ai.BettamindModelPackTrustPolicy
import org.bettamind.shared.ai.LocalAiRuntime
import org.bettamind.shared.ai.UnavailableLocalAiRuntime
import org.bettamind.shared.privacy.EncryptedRecordStore
import org.bettamind.shared.privacy.StorageKeyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BettamindAppServices(
    val dailyRecords: DailyRecordService = UnavailableDailyRecordService,
    val reminders: ReminderPlatformService = UnavailableReminderPlatformService,
    val calendar: CalendarPlatformService = UnavailableCalendarPlatformService,
    val speech: SpeechPlatformService = UnavailableSpeechPlatformService,
    val modelPacks: ModelPackPlatformService = UnavailableModelPackPlatformService,
    val aiRuntime: LocalAiRuntime = UnavailableLocalAiRuntime,
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
    val installState: ModelPackInstallState = ModelPackInstallState.NotInstalled,
    val installedModelId: String? = null,
    val installedModelVersion: Int? = null,
    val installedArtifactFileName: String? = null,
    val installProgressBytes: Long = 0L,
    val installTotalBytes: Long = 0L,
    val lastFailure: ModelPackInstallFailure? = null,
)

enum class ModelPackInstallState {
    NotInstalled,
    AwaitingUserSelection,
    Installing,
    Installed,
    Failed,
    InstallerUnavailable,
}

enum class ModelPackInstallFailure {
    SelectionCanceled,
    MissingManifest,
    MissingArtifact,
    InvalidManifest,
    UnapprovedModel,
    UntrustedSigningKey,
    InvalidSignature,
    ChecksumMismatch,
    ArtifactSizeMismatch,
    StorageFailed,
    PlatformVerifierUnavailable,
}

enum class ModelPackPlatformActionResult {
    Started,
    InstallerUnavailable,
    AlreadyBusy,
    NothingInstalled,
}

interface ModelPackPlatformService {
    val status: StateFlow<ModelPackPlatformStatus>
    fun requestUserInstall(): ModelPackPlatformActionResult
    fun removeInstalledModel(): ModelPackPlatformActionResult
}

object UnavailableModelPackPlatformService : ModelPackPlatformService {
    private val statusFlow = MutableStateFlow(
        ModelPackPlatformStatus(
            installerAvailable = false,
            runtimeAvailable = false,
            firstModelId = BettamindModelPackTrustPolicy.firstProductionModelId,
            requiresSignedManifest = true,
            autoDownloadDisabled = true,
            installState = ModelPackInstallState.InstallerUnavailable,
        )
    )

    override val status: StateFlow<ModelPackPlatformStatus> = statusFlow.asStateFlow()

    override fun requestUserInstall(): ModelPackPlatformActionResult =
        ModelPackPlatformActionResult.InstallerUnavailable

    override fun removeInstalledModel(): ModelPackPlatformActionResult =
        ModelPackPlatformActionResult.NothingInstalled
}
