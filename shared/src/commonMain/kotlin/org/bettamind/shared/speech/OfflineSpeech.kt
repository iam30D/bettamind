package org.bettamind.shared.speech

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bettamind.shared.localization.LocaleTag
import org.bettamind.shared.privacy.SensitiveAction
import org.bettamind.shared.safety.HarmRiskLevel
import org.bettamind.shared.safety.HarmSafetyDecision
import org.bettamind.shared.safety.HarmSafetyPolicy
import org.bettamind.shared.safety.RelationalBoundaryAction
import org.bettamind.shared.safety.RelationalBoundaryAssessment
import org.bettamind.shared.safety.RelationalBoundaryPolicy
import org.bettamind.shared.security.Ed25519SignatureAlgorithm
import org.bettamind.shared.security.ManifestSignatureVerifier
import org.bettamind.shared.security.sha256Hex

enum class SpeechCapability {
    SpeechToText,
    TextToSpeech,
}

enum class SpeechRuntimeSource {
    TextOnlyFallback,
    OsOfflineSpeech,
    LocalSpeechPack,
}

enum class SpeechPermissionState {
    NotRequested,
    Granted,
    Denied,
    Restricted,
}

data class SpeechFeatureSettings(
    val voiceInputEnabled: Boolean = false,
    val voiceOutputEnabled: Boolean = false,
    val microphonePermission: SpeechPermissionState = SpeechPermissionState.NotRequested,
    val rawAudioRetentionRequested: Boolean = false,
    val appLockStepUpCompletedForSensitiveSpeech: Boolean = false,
)

data class SpeechRuntimeCapabilities(
    val osOfflineSpeechToTextAvailable: Boolean = false,
    val osOfflineTextToSpeechAvailable: Boolean = false,
    val installedSpeechPacks: List<InstalledSpeechPack> = emptyList(),
    val textOnlyFallbackAvailable: Boolean = true,
)

data class SpeechReadinessPlan(
    val inputSource: SpeechRuntimeSource,
    val outputSource: SpeechRuntimeSource,
    val textOnlyFallbackAvailable: Boolean,
    val microphonePermissionRequired: Boolean,
    val rawAudioRetentionAllowed: Boolean,
    val speechPacksOptional: Boolean,
    val osOfflineVoicesPreferred: Boolean,
) {
    val offlineOperationPreserved: Boolean
        get() = textOnlyFallbackAvailable
}

enum class SpeechDecisionReason {
    TextOnlyFallbackAvailable,
    VoiceInputDisabled,
    VoiceOutputDisabled,
    MicrophonePermissionRequired,
    RawAudioRetentionForbidden,
    SensitiveTranscriptRequiresAppLock,
    RelationalBoundaryApplied,
    HarmSafetyApplied,
    ProhibitedVoicePersona,
    SpokenOutputBlocked,
}

data class SpeechPipelineDecision(
    val speechAllowed: Boolean,
    val normalTextPipelineAllowed: Boolean,
    val textOnlyFallbackAvailable: Boolean,
    val rawAudioMayBeRetained: Boolean,
    val transcriptStorageAllowedByDefault: Boolean,
    val transcriptRequiresAppLockStepUp: Boolean,
    val spokenOutputAllowed: Boolean,
    val reasons: Set<SpeechDecisionReason>,
    val relationalAssessment: RelationalBoundaryAssessment,
    val harmDecision: HarmSafetyDecision,
    val fallbackLocalizationKey: String?,
    val sensitiveAction: SensitiveAction?,
)

object OfflineSpeechPolicy {
    const val RawAudioRetainedByDefault: Boolean = false

    fun readinessPlan(
        settings: SpeechFeatureSettings,
        capabilities: SpeechRuntimeCapabilities,
    ): SpeechReadinessPlan {
        val inputSource = when {
            !settings.voiceInputEnabled -> SpeechRuntimeSource.TextOnlyFallback
            capabilities.osOfflineSpeechToTextAvailable -> SpeechRuntimeSource.OsOfflineSpeech
            capabilities.installedSpeechPacks.any { SpeechCapability.SpeechToText in it.manifest.capabilities } ->
                SpeechRuntimeSource.LocalSpeechPack

            else -> SpeechRuntimeSource.TextOnlyFallback
        }
        val outputSource = when {
            !settings.voiceOutputEnabled -> SpeechRuntimeSource.TextOnlyFallback
            capabilities.osOfflineTextToSpeechAvailable -> SpeechRuntimeSource.OsOfflineSpeech
            capabilities.installedSpeechPacks.any { SpeechCapability.TextToSpeech in it.manifest.capabilities } ->
                SpeechRuntimeSource.LocalSpeechPack

            else -> SpeechRuntimeSource.TextOnlyFallback
        }
        val hasLocalTtsPack = capabilities.installedSpeechPacks.any {
            SpeechCapability.TextToSpeech in it.manifest.capabilities
        }
        return SpeechReadinessPlan(
            inputSource = inputSource,
            outputSource = outputSource,
            textOnlyFallbackAvailable = capabilities.textOnlyFallbackAvailable,
            microphonePermissionRequired = settings.voiceInputEnabled &&
                settings.microphonePermission != SpeechPermissionState.Granted,
            rawAudioRetentionAllowed = false,
            speechPacksOptional = true,
            osOfflineVoicesPreferred = capabilities.osOfflineTextToSpeechAvailable && hasLocalTtsPack,
        )
    }

    fun reviewVoiceInput(
        transcript: String,
        settings: SpeechFeatureSettings,
    ): SpeechPipelineDecision {
        val relational = RelationalBoundaryPolicy.assessUserInput(transcript)
        val harm = HarmSafetyPolicy.assessUserInput(transcript)
        val sensitiveTranscript = harm.riskLevel != HarmRiskLevel.None ||
            relational.action != RelationalBoundaryAction.Allow
        val reasons = mutableSetOf(SpeechDecisionReason.TextOnlyFallbackAvailable)

        if (!settings.voiceInputEnabled) reasons += SpeechDecisionReason.VoiceInputDisabled
        if (settings.microphonePermission != SpeechPermissionState.Granted) {
            reasons += SpeechDecisionReason.MicrophonePermissionRequired
        }
        if (settings.rawAudioRetentionRequested) {
            reasons += SpeechDecisionReason.RawAudioRetentionForbidden
        }
        if (sensitiveTranscript && !settings.appLockStepUpCompletedForSensitiveSpeech) {
            reasons += SpeechDecisionReason.SensitiveTranscriptRequiresAppLock
        }
        if (!harm.sendToNormalGeneration || harm.requiresSafetyEngine || harm.hasDangerousCapability) {
            reasons += SpeechDecisionReason.HarmSafetyApplied
        }
        if (relational.action !in textPipelineAllowedRelationalActions) {
            reasons += SpeechDecisionReason.RelationalBoundaryApplied
        }

        val speechAllowed = settings.voiceInputEnabled &&
            settings.microphonePermission == SpeechPermissionState.Granted &&
            !settings.rawAudioRetentionRequested
        val normalPipelineAllowed = speechAllowed &&
            harm.sendToNormalGeneration &&
            relational.action in textPipelineAllowedRelationalActions

        return SpeechPipelineDecision(
            speechAllowed = speechAllowed,
            normalTextPipelineAllowed = normalPipelineAllowed,
            textOnlyFallbackAvailable = true,
            rawAudioMayBeRetained = false,
            transcriptStorageAllowedByDefault = !sensitiveTranscript,
            transcriptRequiresAppLockStepUp = sensitiveTranscript,
            spokenOutputAllowed = false,
            reasons = reasons,
            relationalAssessment = relational,
            harmDecision = harm,
            fallbackLocalizationKey = fallbackKeyFor(harm, relational),
            sensitiveAction = SensitiveAction.AccessHighlySensitiveRecord.takeIf { sensitiveTranscript },
        )
    }

    fun reviewVoiceOutput(
        text: String,
        settings: SpeechFeatureSettings,
    ): SpeechPipelineDecision {
        val relational = RelationalBoundaryPolicy.validateGeneratedOutput(text)
        val harm = HarmSafetyPolicy.validateGeneratedOutput(text)
        val relationalVoiceAllowed = RelationalBoundaryPolicy.reviewVoiceOrAvatar(relational).allowed
        val prohibitedVoicePersona = text.normalizedForSpeechRules().hasProhibitedVoicePersona()
        val reasons = mutableSetOf(SpeechDecisionReason.TextOnlyFallbackAvailable)

        if (!settings.voiceOutputEnabled) reasons += SpeechDecisionReason.VoiceOutputDisabled
        if (!relationalVoiceAllowed) reasons += SpeechDecisionReason.RelationalBoundaryApplied
        if (!harm.mayDisplay || harm.hasDangerousCapability) reasons += SpeechDecisionReason.HarmSafetyApplied
        if (prohibitedVoicePersona) reasons += SpeechDecisionReason.ProhibitedVoicePersona

        val outputAllowed = settings.voiceOutputEnabled &&
            relationalVoiceAllowed &&
            harm.mayDisplay &&
            !harm.hasDangerousCapability &&
            !prohibitedVoicePersona
        if (!outputAllowed) reasons += SpeechDecisionReason.SpokenOutputBlocked

        return SpeechPipelineDecision(
            speechAllowed = outputAllowed,
            normalTextPipelineAllowed = outputAllowed,
            textOnlyFallbackAvailable = true,
            rawAudioMayBeRetained = false,
            transcriptStorageAllowedByDefault = true,
            transcriptRequiresAppLockStepUp = false,
            spokenOutputAllowed = outputAllowed,
            reasons = reasons,
            relationalAssessment = relational,
            harmDecision = harm,
            fallbackLocalizationKey = if (outputAllowed) null else "speech_output_text_only_fallback",
            sensitiveAction = null,
        )
    }

    private fun fallbackKeyFor(
        harm: HarmSafetyDecision,
        relational: RelationalBoundaryAssessment,
    ): String? =
        when {
            !harm.sendToNormalGeneration || harm.requiresSafetyEngine || harm.hasDangerousCapability ->
                HarmSafetyPolicy.fallbackFor(harm).localizationKey

            relational.action !in textPipelineAllowedRelationalActions ->
                RelationalBoundaryPolicy.fallbackFor(relational).localizationKey

            else -> null
        }

    private val textPipelineAllowedRelationalActions = setOf(
        RelationalBoundaryAction.Allow,
        RelationalBoundaryAction.AllowWithCaution,
    )
}

@Serializable
data class SpeechPackManifest(
    val packId: String,
    val version: Int,
    val displayName: String,
    val localeTag: String,
    val provider: String,
    val licenseSpdxId: String,
    val capabilities: List<SpeechCapability>,
    val artifactFileName: String,
    val artifactSizeBytes: Long,
    val artifactChecksumSha256: String,
    val signingKeyId: String,
    val signatureAlgorithm: String = Ed25519SignatureAlgorithm,
    val signature: String,
) {
    val locale: LocaleTag
        get() = LocaleTag(localeTag)
}

data class SpeechPackRevocations(
    val revokedPackIds: Set<String> = emptySet(),
    val revokedSigningKeyIds: Set<String> = emptySet(),
    val minimumAcceptedVersions: Map<String, Int> = emptyMap(),
)

data class SpeechPackInstallPolicy(
    val userApprovedInstall: Boolean,
    val publisherLicenseApproved: Boolean,
    val approvedLicenseSpdxIds: Set<String>,
    val revocations: SpeechPackRevocations = SpeechPackRevocations(),
)

data class SpeechPackChunk(
    val offsetBytes: Long,
    val bytes: ByteArray,
)

data class SpeechPackInstallProgress(
    val packId: String,
    val version: Int,
    val receivedBytes: Long,
    val totalBytes: Long,
) {
    val isComplete: Boolean
        get() = receivedBytes == totalBytes
}

data class InstalledSpeechPack(
    val manifest: SpeechPackManifest,
)

interface SpeechPackStore {
    fun installed(): List<InstalledSpeechPack>
    fun readStaged(packId: String, version: Int): ByteArray?
    fun writeStaged(manifest: SpeechPackManifest, artifactBytes: ByteArray)
    fun commit(manifest: SpeechPackManifest, artifactBytes: ByteArray): InstalledSpeechPack
    fun remove(packId: String): Boolean
}

enum class SpeechPackRejectionReason {
    EmptyPackId,
    EmptyLocale,
    EmptyProvider,
    EmptyLicense,
    EmptyArtifactFileName,
    EmptyCapabilities,
    InvalidArtifactSize,
    MissingSignature,
    UnsupportedSignatureAlgorithm,
    UserApprovalMissing,
    PublisherLicenseApprovalMissing,
    LicenseNotApproved,
    RevokedPack,
    RevokedSigningKey,
    RollbackOrReplay,
    InvalidSignature,
    OffsetMismatch,
    ArtifactTooLarge,
    IncompleteArtifact,
    ChecksumMismatch,
}

class SpeechPackRejectedException(
    val reason: SpeechPackRejectionReason,
) : IllegalArgumentException(reason.name)

object SpeechPackCodec {
    val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    fun artifactChecksumSha256(bytes: ByteArray): String =
        bytes.sha256Hex()

    fun signedManifestBytes(manifest: SpeechPackManifest): ByteArray =
        json.encodeToString(
            SignedSpeechPackManifest(
                packId = manifest.packId,
                version = manifest.version,
                displayName = manifest.displayName,
                localeTag = manifest.localeTag,
                provider = manifest.provider,
                licenseSpdxId = manifest.licenseSpdxId,
                capabilities = manifest.capabilities.distinct().sortedBy { it.name },
                artifactFileName = manifest.artifactFileName,
                artifactSizeBytes = manifest.artifactSizeBytes,
                artifactChecksumSha256 = manifest.artifactChecksumSha256,
                signingKeyId = manifest.signingKeyId,
                signatureAlgorithm = manifest.signatureAlgorithm,
            ),
        ).encodeToByteArray()
}

class SpeechPackManager(
    private val signatureVerifier: ManifestSignatureVerifier,
    private val store: SpeechPackStore,
) {
    fun installedPacks(): List<InstalledSpeechPack> =
        store.installed()

    fun beginInstall(
        manifest: SpeechPackManifest,
        policy: SpeechPackInstallPolicy,
    ): SpeechPackInstallProgress {
        validate(manifest, policy)
        val stagedBytes = store.readStaged(manifest.packId, manifest.version) ?: ByteArray(0)
        return progress(manifest, stagedBytes.size.toLong())
    }

    fun appendChunk(
        manifest: SpeechPackManifest,
        chunk: SpeechPackChunk,
        policy: SpeechPackInstallPolicy,
    ): SpeechPackInstallProgress {
        validate(manifest, policy)

        val stagedBytes = store.readStaged(manifest.packId, manifest.version) ?: ByteArray(0)
        if (chunk.offsetBytes != stagedBytes.size.toLong()) {
            reject(SpeechPackRejectionReason.OffsetMismatch)
        }

        val receivedBytes = stagedBytes.size.toLong() + chunk.bytes.size.toLong()
        if (receivedBytes > manifest.artifactSizeBytes) {
            reject(SpeechPackRejectionReason.ArtifactTooLarge)
        }

        store.writeStaged(manifest, stagedBytes + chunk.bytes)
        return progress(manifest, receivedBytes)
    }

    fun finishInstall(
        manifest: SpeechPackManifest,
        policy: SpeechPackInstallPolicy,
    ): InstalledSpeechPack {
        validate(manifest, policy)

        val artifactBytes = store.readStaged(manifest.packId, manifest.version) ?: ByteArray(0)
        if (artifactBytes.size.toLong() != manifest.artifactSizeBytes) {
            reject(SpeechPackRejectionReason.IncompleteArtifact)
        }
        val checksum = SpeechPackCodec.artifactChecksumSha256(artifactBytes)
        if (!manifest.artifactChecksumSha256.equals(checksum, ignoreCase = true)) {
            reject(SpeechPackRejectionReason.ChecksumMismatch)
        }

        return store.commit(manifest, artifactBytes)
    }

    fun remove(packId: String): Boolean =
        store.remove(packId)

    private fun validate(
        manifest: SpeechPackManifest,
        policy: SpeechPackInstallPolicy,
    ) {
        validateStructure(manifest)
        validatePolicy(manifest, policy)
        validateSignature(manifest)
    }

    private fun validateStructure(manifest: SpeechPackManifest) {
        if (manifest.packId.isBlank()) reject(SpeechPackRejectionReason.EmptyPackId)
        if (manifest.localeTag.isBlank()) reject(SpeechPackRejectionReason.EmptyLocale)
        if (manifest.provider.isBlank()) reject(SpeechPackRejectionReason.EmptyProvider)
        if (manifest.licenseSpdxId.isBlank()) reject(SpeechPackRejectionReason.EmptyLicense)
        if (manifest.artifactFileName.isBlank()) reject(SpeechPackRejectionReason.EmptyArtifactFileName)
        if (manifest.capabilities.isEmpty()) reject(SpeechPackRejectionReason.EmptyCapabilities)
        if (manifest.artifactSizeBytes <= 0L) reject(SpeechPackRejectionReason.InvalidArtifactSize)
        if (manifest.signature.isBlank() || manifest.signingKeyId.isBlank()) {
            reject(SpeechPackRejectionReason.MissingSignature)
        }
        if (manifest.signatureAlgorithm != Ed25519SignatureAlgorithm) {
            reject(SpeechPackRejectionReason.UnsupportedSignatureAlgorithm)
        }
    }

    private fun validatePolicy(
        manifest: SpeechPackManifest,
        policy: SpeechPackInstallPolicy,
    ) {
        if (!policy.userApprovedInstall) reject(SpeechPackRejectionReason.UserApprovalMissing)
        if (!policy.publisherLicenseApproved) {
            reject(SpeechPackRejectionReason.PublisherLicenseApprovalMissing)
        }
        if (manifest.licenseSpdxId !in policy.approvedLicenseSpdxIds) {
            reject(SpeechPackRejectionReason.LicenseNotApproved)
        }
        val revocations = policy.revocations
        if (manifest.packId in revocations.revokedPackIds) reject(SpeechPackRejectionReason.RevokedPack)
        if (manifest.signingKeyId in revocations.revokedSigningKeyIds) {
            reject(SpeechPackRejectionReason.RevokedSigningKey)
        }

        val installedVersion = store.installed()
            .firstOrNull { it.manifest.packId == manifest.packId }
            ?.manifest
            ?.version
        val minimumAcceptedVersion = revocations.minimumAcceptedVersions[manifest.packId]
        if (installedVersion != null && manifest.version <= installedVersion) {
            reject(SpeechPackRejectionReason.RollbackOrReplay)
        }
        if (minimumAcceptedVersion != null && manifest.version < minimumAcceptedVersion) {
            reject(SpeechPackRejectionReason.RollbackOrReplay)
        }
    }

    private fun validateSignature(manifest: SpeechPackManifest) {
        val signatureValid = signatureVerifier.verify(
            signedBytes = SpeechPackCodec.signedManifestBytes(manifest),
            signature = manifest.signature,
            signingKeyId = manifest.signingKeyId,
        )
        if (!signatureValid) reject(SpeechPackRejectionReason.InvalidSignature)
    }

    private fun progress(
        manifest: SpeechPackManifest,
        receivedBytes: Long,
    ): SpeechPackInstallProgress =
        SpeechPackInstallProgress(
            packId = manifest.packId,
            version = manifest.version,
            receivedBytes = receivedBytes,
            totalBytes = manifest.artifactSizeBytes,
        )

    private fun reject(reason: SpeechPackRejectionReason): Nothing =
        throw SpeechPackRejectedException(reason)
}

@Serializable
private data class SignedSpeechPackManifest(
    val packId: String,
    val version: Int,
    val displayName: String,
    val localeTag: String,
    val provider: String,
    val licenseSpdxId: String,
    val capabilities: List<SpeechCapability>,
    val artifactFileName: String,
    val artifactSizeBytes: Long,
    val artifactChecksumSha256: String,
    val signingKeyId: String,
    val signatureAlgorithm: String,
)

private fun String.normalizedForSpeechRules(): String =
    lowercase()
        .replace('\n', ' ')
        .replace('\r', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

private fun String.containsAny(vararg fragments: String): Boolean =
    fragments.any { contains(it) }

private fun String.hasProhibitedVoicePersona(): Boolean =
    containsAny(
        "seductive",
        "sensual",
        "flirty",
        "possessive",
        "romantic partner",
        "whisper in your ear",
        "only i can help",
        "you only need me",
        "do not talk to anyone else",
        "don't talk to anyone else",
    )
