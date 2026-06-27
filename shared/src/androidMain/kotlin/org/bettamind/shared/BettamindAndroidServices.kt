package org.bettamind.shared

import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import java.io.File
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.bettamind.shared.ai.BettamindModelPackTrustPolicy
import org.bettamind.shared.ai.AiCapabilities
import org.bettamind.shared.ai.AiRequest
import org.bettamind.shared.ai.AiToken
import org.bettamind.shared.ai.ClassificationRequest
import org.bettamind.shared.ai.ClassificationResult
import org.bettamind.shared.ai.InstalledModel
import org.bettamind.shared.ai.LiteRtLmRuntimeId
import org.bettamind.shared.ai.LiteRtLmBridge
import org.bettamind.shared.ai.LiteRtLmRuntimeAdapter
import org.bettamind.shared.ai.LoadResult
import org.bettamind.shared.ai.ModelCapability
import org.bettamind.shared.ai.ModelPackCodec
import org.bettamind.shared.ai.ModelPackManifest
import org.bettamind.shared.security.ManifestSignatureVerifier
import org.bettamind.shared.privacy.AndroidKeystoreStorageKeyManager
import org.bettamind.shared.privacy.AndroidSqlCipherEncryptedRecordStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

fun createAndroidBettamindAppServices(activity: FragmentActivity): BettamindAppServices {
    val modelPackService = AndroidModelPackPlatformService(activity)
    return BettamindAppServices(
        dailyRecords = EncryptedDailyRecordService(
            store = AndroidSqlCipherEncryptedRecordStore(activity),
            keyManager = AndroidKeystoreStorageKeyManager(activity),
            nowEpochMillis = { System.currentTimeMillis() },
            localDate = { AndroidLocalDateFormatter.format(Date()) },
        ),
        reminders = AndroidReminderPlatformService,
        calendar = AndroidCalendarPlatformService,
        speech = AndroidSpeechPlatformService,
        modelPacks = modelPackService,
        aiRuntime = LiteRtLmRuntimeAdapter(AndroidLiteRtLmBridge(activity)),
    )
}

private val AndroidLocalDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private const val ModelPacksDirectoryName = "model-packs"
private const val InstalledModelPacksDirectoryName = "installed"
private const val StagedModelPacksDirectoryName = "staged"
private const val ManifestFileName = "manifest.json"

private object AndroidReminderPlatformService : ReminderPlatformService {
    override fun status(): ReminderPlatformStatus =
        ReminderPlatformStatus(
            available = true,
            neutralPreviewOnly = true,
            platformName = "android-local-notifications",
        )
}

private object AndroidCalendarPlatformService : CalendarPlatformService {
    override fun status(): CalendarPlatformStatus =
        CalendarPlatformStatus(
            available = true,
            explicitHandoffOnly = true,
            readsBroadCalendarData = false,
            platformName = "android-calendar-insert-intent",
        )
}

private object AndroidSpeechPlatformService : SpeechPlatformService {
    override fun status(): SpeechPlatformStatus =
        SpeechPlatformStatus(
            available = true,
            osOfflinePreferred = true,
            requiresExplicitMicrophonePermission = true,
            textFallbackAvailable = true,
            platformName = "android-os-speech",
        )
}

private class AndroidModelPackPlatformService(
    private val activity: FragmentActivity,
) : ModelPackPlatformService {
    private val verifier = AndroidManifestSignatureVerifier()
    private val root = File(activity.noBackupFilesDir, ModelPacksDirectoryName)
    private val installedRoot = File(root, InstalledModelPacksDirectoryName)
    private val stagedRoot = File(root, StagedModelPacksDirectoryName)
    private val statusFlow = MutableStateFlow(loadStatus())
    private val documentPicker = activity.registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isEmpty()) {
            statusFlow.value = baseStatus(
                installState = ModelPackInstallState.Failed,
                lastFailure = ModelPackInstallFailure.SelectionCanceled,
            )
            return@registerForActivityResult
        }
        activity.lifecycleScope.launch(Dispatchers.IO) {
            installSelectedDocuments(uris)
        }
    }

    override val status: StateFlow<ModelPackPlatformStatus> = statusFlow.asStateFlow()

    override fun requestUserInstall(): ModelPackPlatformActionResult {
        if (statusFlow.value.installState == ModelPackInstallState.Installing) {
            return ModelPackPlatformActionResult.AlreadyBusy
        }
        statusFlow.value = baseStatus(installState = ModelPackInstallState.AwaitingUserSelection)
        documentPicker.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
        return ModelPackPlatformActionResult.Started
    }

    override fun removeInstalledModel(): ModelPackPlatformActionResult {
        val installed = installedPackDirectory() ?: return ModelPackPlatformActionResult.NothingInstalled
        val removed = installed.deleteRecursively()
        statusFlow.value = if (removed) {
            baseStatus()
        } else {
            baseStatus(
                installState = ModelPackInstallState.Failed,
                lastFailure = ModelPackInstallFailure.StorageFailed,
            )
        }
        return if (removed) {
            ModelPackPlatformActionResult.Started
        } else {
            ModelPackPlatformActionResult.NothingInstalled
        }
    }

    private fun installSelectedDocuments(uris: List<Uri>) {
        runCatching {
            val manifest = selectedManifest(uris) ?: return fail(ModelPackInstallFailure.MissingManifest)
            val structureFailure = manifest.installValidationFailure()
            if (structureFailure != null) {
                return fail(structureFailure)
            }
            if (!verifier.verify(
                    signedBytes = ModelPackCodec.signedManifestBytes(manifest),
                    signature = manifest.signature,
                    signingKeyId = manifest.signingKeyId,
                )
            ) {
                return fail(ModelPackInstallFailure.InvalidSignature)
            }
            val artifactUri = selectedArtifactUri(uris, manifest)
                ?: return fail(ModelPackInstallFailure.MissingArtifact)
            installArtifact(manifest, artifactUri)
        }.onFailure {
            fail(ModelPackInstallFailure.StorageFailed)
        }
    }

    private fun selectedManifest(uris: List<Uri>): ModelPackManifest? =
        uris.firstNotNullOfOrNull { uri ->
            val metadata = uri.metadata()
            val looksLikeManifest = metadata.displayName?.endsWith(".json", ignoreCase = true) == true ||
                (metadata.sizeBytes != null && metadata.sizeBytes <= MAX_MANIFEST_BYTES)
            if (!looksLikeManifest) {
                return@firstNotNullOfOrNull null
            }
            runCatching {
                activity.contentResolver.openInputStream(uri)?.use { input ->
                    val text = input.bufferedReader().readText()
                    ModelPackCodec.json.decodeFromString<ModelPackManifest>(text)
                }
            }.getOrNull()
        }

    private fun selectedArtifactUri(
        uris: List<Uri>,
        manifest: ModelPackManifest,
    ): Uri? =
        uris.firstOrNull { uri ->
            val metadata = uri.metadata()
            metadata.displayName == manifest.artifactFileName ||
                metadata.sizeBytes == manifest.artifactSizeBytes
        }

    private fun installArtifact(
        manifest: ModelPackManifest,
        artifactUri: Uri,
    ) {
        val installDir = installedDirectoryFor(manifest)
        val stageDir = File(stagedRoot, installDir.name)
        val stagedArtifact = File(stageDir, "${manifest.artifactFileName}.part")
        val digest = MessageDigest.getInstance("SHA-256")
        var copiedBytes = 0L

        statusFlow.value = baseStatus(
            installState = ModelPackInstallState.Installing,
            installProgressBytes = 0L,
            installTotalBytes = manifest.artifactSizeBytes,
        )
        stageDir.deleteRecursively()
        stageDir.mkdirs()

        activity.contentResolver.openInputStream(artifactUri)?.use { input ->
            stagedArtifact.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_COPY_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)
                    copiedBytes += read.toLong()
                    if (copiedBytes == manifest.artifactSizeBytes || copiedBytes % PROGRESS_UPDATE_BYTES < read) {
                        statusFlow.value = baseStatus(
                            installState = ModelPackInstallState.Installing,
                            installProgressBytes = copiedBytes,
                            installTotalBytes = manifest.artifactSizeBytes,
                        )
                    }
                }
            }
        } ?: return fail(ModelPackInstallFailure.MissingArtifact)

        if (copiedBytes != manifest.artifactSizeBytes) {
            stageDir.deleteRecursively()
            return fail(ModelPackInstallFailure.ArtifactSizeMismatch)
        }
        val checksum = digest.digest().toHex()
        if (!manifest.artifactChecksumSha256.equals(checksum, ignoreCase = true)) {
            stageDir.deleteRecursively()
            return fail(ModelPackInstallFailure.ChecksumMismatch)
        }

        installDir.deleteRecursively()
        installDir.mkdirs()
        val installedArtifact = File(installDir, manifest.artifactFileName)
        if (!stagedArtifact.renameTo(installedArtifact)) {
            stageDir.deleteRecursively()
            return fail(ModelPackInstallFailure.StorageFailed)
        }
        File(installDir, ManifestFileName).writeText(
            ModelPackCodec.json.encodeToString(manifest),
            Charsets.UTF_8,
        )
        stageDir.deleteRecursively()
        statusFlow.value = installedStatus(manifest)
    }

    private fun ModelPackManifest.installValidationFailure(): ModelPackInstallFailure? =
        when {
            modelId != BettamindModelPackTrustPolicy.firstProductionModelId ->
                ModelPackInstallFailure.UnapprovedModel
            artifactFileName != BettamindModelPackTrustPolicy.firstProductionArtifactFileName ->
                ModelPackInstallFailure.UnapprovedModel
            runtimeId != LiteRtLmRuntimeId ->
                ModelPackInstallFailure.InvalidManifest
            ModelCapability.Generation !in capabilities ->
                ModelPackInstallFailure.InvalidManifest
            signingKeyId !in BettamindModelPackTrustPolicy.approvedSigningKeyIds() ->
                ModelPackInstallFailure.UntrustedSigningKey
            artifactSizeBytes <= 0L || artifactChecksumSha256.isBlank() || signature.isBlank() ->
                ModelPackInstallFailure.InvalidManifest
            else -> null
        }

    private fun fail(failure: ModelPackInstallFailure) {
        statusFlow.value = baseStatus(
            installState = ModelPackInstallState.Failed,
            lastFailure = failure,
        )
    }

    private fun loadStatus(): ModelPackPlatformStatus {
        val manifest = installedManifest()
        return if (manifest == null) {
            baseStatus()
        } else {
            installedStatus(manifest)
        }
    }

    private fun installedManifest(): ModelPackManifest? =
        installedRoot.listFiles()
            ?.firstNotNullOfOrNull { directory ->
                runCatching {
                    val manifest = ModelPackCodec.json.decodeFromString<ModelPackManifest>(
                        File(directory, ManifestFileName).readText(Charsets.UTF_8),
                    )
                    val artifact = File(directory, manifest.artifactFileName)
                    manifest.takeIf {
                        artifact.exists() && artifact.length() == manifest.artifactSizeBytes
                    }
                }.getOrNull()
            }

    private fun installedPackDirectory(): File? =
        installedManifest()?.let { installedDirectoryFor(it) }

    private fun installedDirectoryFor(manifest: ModelPackManifest): File =
        File(installedRoot, "${manifest.modelId.safeFileSegment()}-v${manifest.version}")

    private fun installedStatus(manifest: ModelPackManifest): ModelPackPlatformStatus =
        baseStatus(
            installState = ModelPackInstallState.Installed,
            installedModelId = manifest.modelId,
            installedModelVersion = manifest.version,
            installedArtifactFileName = manifest.artifactFileName,
        )

    private fun baseStatus(
        installState: ModelPackInstallState = ModelPackInstallState.NotInstalled,
        installedModelId: String? = null,
        installedModelVersion: Int? = null,
        installedArtifactFileName: String? = null,
        installProgressBytes: Long = 0L,
        installTotalBytes: Long = 0L,
        lastFailure: ModelPackInstallFailure? = null,
    ): ModelPackPlatformStatus =
        ModelPackPlatformStatus(
            installerAvailable = true,
            runtimeAvailable = installState == ModelPackInstallState.Installed,
            firstModelId = BettamindModelPackTrustPolicy.firstProductionModelId,
            requiresSignedManifest = true,
            autoDownloadDisabled = true,
            installState = installState,
            installedModelId = installedModelId,
            installedModelVersion = installedModelVersion,
            installedArtifactFileName = installedArtifactFileName,
            installProgressBytes = installProgressBytes,
            installTotalBytes = installTotalBytes,
            lastFailure = lastFailure,
        )

    private fun Uri.metadata(): DocumentMetadata {
        var displayName: String? = null
        var sizeBytes: Long? = null
        activity.contentResolver.query(this, null, null, null, null)?.useCursor { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex >= 0) displayName = cursor.getString(nameIndex)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) sizeBytes = cursor.getLong(sizeIndex)
            }
        }
        return DocumentMetadata(displayName = displayName, sizeBytes = sizeBytes)
    }

    private fun <T> Cursor.useCursor(block: (Cursor) -> T): T =
        use { block(it) }

    private fun String.safeFileSegment(): String =
        replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private data class DocumentMetadata(
        val displayName: String?,
        val sizeBytes: Long?,
    )

    private companion object {
        const val DEFAULT_COPY_BUFFER_SIZE = 1024 * 1024
        const val PROGRESS_UPDATE_BYTES = 32L * 1024L * 1024L
        const val MAX_MANIFEST_BYTES = 1024L * 1024L
    }
}

private class AndroidLiteRtLmBridge(
    private val activity: FragmentActivity,
) : LiteRtLmBridge {
    private val installedRoot = File(
        File(activity.noBackupFilesDir, ModelPacksDirectoryName),
        InstalledModelPacksDirectoryName,
    )
    private val engineLock = Any()
    private var engine: Engine? = null
    private var loadedModelKey: String? = null

    override suspend fun capabilities(): AiCapabilities {
        val installed = installedModelPack()
        return AiCapabilities(
            available = installed != null,
            supportsGeneration = installed != null,
            supportsClassification = false,
            supportsEmbeddings = false,
        )
    }

    override suspend fun load(model: InstalledModel): LoadResult {
        val installed = installedModelPack()
            ?: return LoadResult.Unavailable("litert_lm_model_not_installed")
        if (installed.manifest.modelId != model.id) {
            return LoadResult.Unavailable("litert_lm_requested_model_not_installed")
        }
        return runCatching {
            ensureEngine(installed)
            LoadResult.Loaded
        }.getOrElse {
            LoadResult.Unavailable("litert_lm_load_failed")
        }
    }

    override fun generate(request: AiRequest): Flow<AiToken> =
        flow {
            val installed = installedModelPack()
                ?: error("litert_lm_model_not_installed")
            val conversation = ensureEngine(installed).createConversation()
            conversation.sendMessageAsync(request.prompt).collect { message ->
                val text = message.contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString(separator = "") { content -> content.text }
                if (text.isNotEmpty()) {
                    emit(AiToken(text))
                }
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun classify(request: ClassificationRequest): ClassificationResult =
        ClassificationResult(label = "unavailable", confidence = 0.0)

    override suspend fun embed(texts: List<String>): List<FloatArray> = emptyList()

    override suspend fun unload() {
        synchronized(engineLock) {
            engine?.close()
            engine = null
            loadedModelKey = null
        }
    }

    private fun ensureEngine(installed: AndroidInstalledModelPack): Engine =
        synchronized(engineLock) {
            val modelKey = installed.modelKey()
            val existing = engine
            if (existing != null && loadedModelKey == modelKey) {
                return@synchronized existing
            }
            existing?.close()
            val loaded = Engine(
                EngineConfig(
                    modelPath = installed.artifact.absolutePath,
                    cacheDir = File(activity.cacheDir, "litert-lm").absolutePath,
                ),
            )
            loaded.initialize()
            engine = loaded
            loadedModelKey = modelKey
            loaded
        }

    private fun installedModelPack(): AndroidInstalledModelPack? =
        installedRoot.listFiles()
            ?.firstNotNullOfOrNull { directory ->
                runCatching {
                    val manifest = ModelPackCodec.json.decodeFromString<ModelPackManifest>(
                        File(directory, ManifestFileName).readText(Charsets.UTF_8),
                    )
                    val artifact = File(directory, manifest.artifactFileName)
                    AndroidInstalledModelPack(manifest, artifact).takeIf {
                        manifest.runtimeId == LiteRtLmRuntimeId &&
                            ModelCapability.Generation in manifest.capabilities &&
                            manifest.modelId == BettamindModelPackTrustPolicy.firstProductionModelId &&
                            artifact.exists() &&
                            artifact.length() == manifest.artifactSizeBytes
                    }
                }.getOrNull()
            }

    private fun AndroidInstalledModelPack.modelKey(): String =
        listOf(
            manifest.modelId,
            manifest.version.toString(),
            manifest.artifactChecksumSha256,
            artifact.absolutePath,
            artifact.length().toString(),
            artifact.lastModified().toString(),
        ).joinToString(separator = "|")

    private data class AndroidInstalledModelPack(
        val manifest: ModelPackManifest,
        val artifact: File,
    )
}

private class AndroidManifestSignatureVerifier : ManifestSignatureVerifier {
    override fun verify(
        signedBytes: ByteArray,
        signature: String,
        signingKeyId: String,
    ): Boolean =
        runCatching {
            val anchor = BettamindModelPackTrustPolicy.productionTrustAnchors
                .firstOrNull { it.keyId == signingKeyId }
                ?: return false
            val publicKey = KeyFactory.getInstance("Ed25519").generatePublic(
                X509EncodedKeySpec(Base64.decode(anchor.publicKeyBase64, Base64.DEFAULT)),
            )
            val verifier = Signature.getInstance("Ed25519")
            verifier.initVerify(publicKey)
            verifier.update(signedBytes)
            verifier.verify(Base64.decode(signature, Base64.DEFAULT))
        }.getOrDefault(false)
}
