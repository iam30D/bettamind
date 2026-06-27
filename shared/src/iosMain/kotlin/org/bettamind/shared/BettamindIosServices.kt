package org.bettamind.shared

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import org.bettamind.shared.ai.BettamindModelPackTrustPolicy
import org.bettamind.shared.ai.AiCapabilities
import org.bettamind.shared.ai.AiRequest
import org.bettamind.shared.ai.AiToken
import org.bettamind.shared.ai.ClassificationRequest
import org.bettamind.shared.ai.ClassificationResult
import org.bettamind.shared.ai.InstalledModel
import org.bettamind.shared.ai.LiteRtLmRuntimeId
import org.bettamind.shared.ai.LoadResult
import org.bettamind.shared.ai.LocalAiRuntime
import org.bettamind.shared.ai.ModelCapability
import org.bettamind.shared.ai.ModelPackCodec
import org.bettamind.shared.ai.ModelPackManifest
import org.bettamind.shared.privacy.IosKeychainStorageKeyManager
import org.bettamind.shared.privacy.IosSqlCipherEncryptedRecordStore
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.posix.time

fun createIosBettamindAppServices(): BettamindAppServices =
    createIosBettamindAppServices(nativeAiBridge = null)

fun createIosBettamindAppServices(nativeAiBridge: IosNativeAiBridge?): BettamindAppServices =
    BettamindAppServices(
        dailyRecords = EncryptedDailyRecordService(
            store = IosSqlCipherEncryptedRecordStore(),
            keyManager = IosKeychainStorageKeyManager(),
            nowEpochMillis = ::currentEpochMillis,
            localDate = { IosLocalDateFormatter.stringFromDate(NSDate()) },
        ),
        reminders = IosReminderPlatformService,
        calendar = IosCalendarPlatformService,
        speech = IosSpeechPlatformService,
        modelPacks = nativeAiBridge?.let { IosModelPackPlatformService(it) }
            ?: UnavailableIosModelPackPlatformService,
        aiRuntime = nativeAiBridge?.let { IosLiteRtLmRuntimeAdapter(it) } ?: org.bettamind.shared.ai.UnavailableLocalAiRuntime,
    )

interface IosNativeAiBridge {
    fun installerAvailable(): Boolean
    fun runtimeAvailable(): Boolean
    fun installedManifestJson(): String?
    fun requestUserInstall(callback: IosModelPackInstallCallback): Boolean
    fun removeInstalledModel(): Boolean
    fun loadRuntime(): Boolean
    fun generate(prompt: String): String?
    fun closeRuntime()
}

interface IosModelPackInstallCallback {
    fun onInstallCompleted(manifestJson: String)
    fun onInstallFailed(failureName: String)
}

private val IosLocalDateFormatter = NSDateFormatter().apply {
    dateFormat = "yyyy-MM-dd"
}

@OptIn(ExperimentalForeignApi::class)
private fun currentEpochMillis(): Long = time(null) * 1000L

private object IosReminderPlatformService : ReminderPlatformService {
    override fun status(): ReminderPlatformStatus =
        ReminderPlatformStatus(
            available = true,
            neutralPreviewOnly = true,
            platformName = "ios-user-notifications",
        )
}

private object IosCalendarPlatformService : CalendarPlatformService {
    override fun status(): CalendarPlatformStatus =
        CalendarPlatformStatus(
            available = true,
            explicitHandoffOnly = true,
            readsBroadCalendarData = false,
            platformName = "ios-eventkit-handoff",
        )
}

private object IosSpeechPlatformService : SpeechPlatformService {
    override fun status(): SpeechPlatformStatus =
        SpeechPlatformStatus(
            available = true,
            osOfflinePreferred = true,
            requiresExplicitMicrophonePermission = true,
            textFallbackAvailable = true,
            platformName = "ios-os-speech",
        )
}

private object UnavailableIosModelPackPlatformService : ModelPackPlatformService {
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

private class IosModelPackPlatformService(
    private val bridge: IosNativeAiBridge,
) : ModelPackPlatformService {
    private val statusFlow = MutableStateFlow(loadStatus())

    override val status: StateFlow<ModelPackPlatformStatus> = statusFlow.asStateFlow()

    override fun requestUserInstall(): ModelPackPlatformActionResult {
        val current = statusFlow.value.installState
        if (current == ModelPackInstallState.Installing || current == ModelPackInstallState.AwaitingUserSelection) {
            return ModelPackPlatformActionResult.AlreadyBusy
        }
        if (!bridge.installerAvailable()) {
            statusFlow.value = baseStatus(installState = ModelPackInstallState.InstallerUnavailable)
            return ModelPackPlatformActionResult.InstallerUnavailable
        }
        statusFlow.value = baseStatus(installState = ModelPackInstallState.AwaitingUserSelection)
        val started = bridge.requestUserInstall(
            object : IosModelPackInstallCallback {
                override fun onInstallCompleted(manifestJson: String) {
                    val manifest = manifestJson.decodeManifest()
                    statusFlow.value = if (manifest != null) {
                        installedStatus(manifest)
                    } else {
                        baseStatus(
                            installState = ModelPackInstallState.Failed,
                            lastFailure = ModelPackInstallFailure.InvalidManifest,
                        )
                    }
                }

                override fun onInstallFailed(failureName: String) {
                    statusFlow.value = baseStatus(
                        installState = ModelPackInstallState.Failed,
                        lastFailure = failureName.toModelPackInstallFailure(),
                    )
                }
            },
        )
        return if (started) {
            ModelPackPlatformActionResult.Started
        } else {
            statusFlow.value = baseStatus(installState = ModelPackInstallState.InstallerUnavailable)
            ModelPackPlatformActionResult.InstallerUnavailable
        }
    }

    override fun removeInstalledModel(): ModelPackPlatformActionResult {
        bridge.closeRuntime()
        return if (bridge.removeInstalledModel()) {
            statusFlow.value = baseStatus()
            ModelPackPlatformActionResult.Started
        } else {
            ModelPackPlatformActionResult.NothingInstalled
        }
    }

    private fun loadStatus(): ModelPackPlatformStatus {
        val manifest = bridge.installedManifestJson()?.decodeManifest()
        return if (manifest == null) baseStatus() else installedStatus(manifest)
    }

    private fun installedStatus(manifest: ModelPackManifest): ModelPackPlatformStatus =
        baseStatus(
            installState = ModelPackInstallState.Installed,
            runtimeAvailable = bridge.runtimeAvailable(),
            installedModelId = manifest.modelId,
            installedModelVersion = manifest.version,
            installedArtifactFileName = manifest.artifactFileName,
        )

    private fun baseStatus(
        installState: ModelPackInstallState = ModelPackInstallState.NotInstalled,
        runtimeAvailable: Boolean = false,
        installedModelId: String? = null,
        installedModelVersion: Int? = null,
        installedArtifactFileName: String? = null,
        lastFailure: ModelPackInstallFailure? = null,
    ): ModelPackPlatformStatus =
        ModelPackPlatformStatus(
            installerAvailable = bridge.installerAvailable(),
            runtimeAvailable = runtimeAvailable,
            firstModelId = BettamindModelPackTrustPolicy.firstProductionModelId,
            requiresSignedManifest = true,
            autoDownloadDisabled = true,
            installState = installState,
            installedModelId = installedModelId,
            installedModelVersion = installedModelVersion,
            installedArtifactFileName = installedArtifactFileName,
            lastFailure = lastFailure,
        )
}

private class IosLiteRtLmRuntimeAdapter(
    private val bridge: IosNativeAiBridge,
) : LocalAiRuntime {
    override suspend fun capabilities(): AiCapabilities {
        val available = bridge.runtimeAvailable()
        return AiCapabilities(
            available = available,
            supportsGeneration = available,
            supportsClassification = false,
            supportsEmbeddings = false,
        )
    }

    override suspend fun load(model: InstalledModel): LoadResult =
        if (withContext(Dispatchers.Default) { bridge.loadRuntime() }) {
            LoadResult.Loaded
        } else {
            LoadResult.Unavailable("ios_litert_lm_load_failed")
        }

    override fun generate(request: AiRequest): Flow<AiToken> =
        flow {
            val text = withContext(Dispatchers.Default) {
                bridge.generate(request.prompt)
            } ?: error("ios_litert_lm_generation_failed")
            if (text.isNotBlank()) {
                emit(AiToken(text))
            }
        }.flowOn(Dispatchers.Default)

    override suspend fun classify(request: ClassificationRequest): ClassificationResult =
        ClassificationResult(label = "unavailable", confidence = 0.0)

    override suspend fun embed(texts: List<String>): List<FloatArray> = emptyList()

    override suspend fun unload() {
        withContext(Dispatchers.Default) {
            bridge.closeRuntime()
        }
    }
}

private fun String.decodeManifest(): ModelPackManifest? =
    runCatching {
        ModelPackCodec.json.decodeFromString<ModelPackManifest>(this)
    }.getOrNull()?.takeIf { manifest ->
        manifest.modelId == BettamindModelPackTrustPolicy.firstProductionModelId &&
            manifest.artifactFileName == BettamindModelPackTrustPolicy.firstProductionArtifactFileName &&
            manifest.runtimeId == LiteRtLmRuntimeId &&
            ModelCapability.Generation in manifest.capabilities
    }

private fun String.toModelPackInstallFailure(): ModelPackInstallFailure =
    when (this) {
        "SelectionCanceled" -> ModelPackInstallFailure.SelectionCanceled
        "MissingManifest" -> ModelPackInstallFailure.MissingManifest
        "MissingArtifact" -> ModelPackInstallFailure.MissingArtifact
        "InvalidManifest" -> ModelPackInstallFailure.InvalidManifest
        "UnapprovedModel" -> ModelPackInstallFailure.UnapprovedModel
        "UntrustedSigningKey" -> ModelPackInstallFailure.UntrustedSigningKey
        "InvalidSignature" -> ModelPackInstallFailure.InvalidSignature
        "ChecksumMismatch" -> ModelPackInstallFailure.ChecksumMismatch
        "ArtifactSizeMismatch" -> ModelPackInstallFailure.ArtifactSizeMismatch
        "PlatformVerifierUnavailable" -> ModelPackInstallFailure.PlatformVerifierUnavailable
        else -> ModelPackInstallFailure.StorageFailed
    }
