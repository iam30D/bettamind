package org.bettamind.shared.speech

import org.bettamind.shared.privacy.SensitiveAction
import org.bettamind.shared.security.ManifestSignatureVerifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class OfflineSpeechTest {
    @Test
    fun textOnlyFallbackStaysCompleteWhenSpeechIsDisabled() {
        val plan = OfflineSpeechPolicy.readinessPlan(
            settings = SpeechFeatureSettings(),
            capabilities = SpeechRuntimeCapabilities(
                osOfflineSpeechToTextAvailable = true,
                osOfflineTextToSpeechAvailable = true,
            ),
        )

        assertEquals(SpeechRuntimeSource.TextOnlyFallback, plan.inputSource)
        assertEquals(SpeechRuntimeSource.TextOnlyFallback, plan.outputSource)
        assertTrue(plan.textOnlyFallbackAvailable)
        assertTrue(plan.offlineOperationPreserved)
        assertFalse(plan.microphonePermissionRequired)
        assertFalse(plan.rawAudioRetentionAllowed)
    }

    @Test
    fun microphonePermissionIsExplicitAndRawAudioRetentionIsNeverAllowed() {
        val settings = SpeechFeatureSettings(
            voiceInputEnabled = true,
            microphonePermission = SpeechPermissionState.NotRequested,
            rawAudioRetentionRequested = true,
        )

        val plan = OfflineSpeechPolicy.readinessPlan(
            settings = settings,
            capabilities = SpeechRuntimeCapabilities(osOfflineSpeechToTextAvailable = true),
        )
        val decision = OfflineSpeechPolicy.reviewVoiceInput(
            transcript = "I want a practical next step.",
            settings = settings,
        )

        assertEquals(SpeechRuntimeSource.OsOfflineSpeech, plan.inputSource)
        assertTrue(plan.microphonePermissionRequired)
        assertFalse(plan.rawAudioRetentionAllowed)
        assertFalse(decision.speechAllowed)
        assertFalse(decision.rawAudioMayBeRetained)
        assertTrue(SpeechDecisionReason.MicrophonePermissionRequired in decision.reasons)
        assertTrue(SpeechDecisionReason.RawAudioRetentionForbidden in decision.reasons)
    }

    @Test
    fun spokenInputUsesTextSafetyPipelineAndRequiresStepUpForSensitiveTranscript() {
        val decision = OfflineSpeechPolicy.reviewVoiceInput(
            transcript = "I want to kill myself tonight.",
            settings = SpeechFeatureSettings(
                voiceInputEnabled = true,
                microphonePermission = SpeechPermissionState.Granted,
                appLockStepUpCompletedForSensitiveSpeech = false,
            ),
        )

        assertTrue(decision.speechAllowed)
        assertFalse(decision.normalTextPipelineAllowed)
        assertFalse(decision.transcriptStorageAllowedByDefault)
        assertTrue(decision.transcriptRequiresAppLockStepUp)
        assertEquals(SensitiveAction.AccessHighlySensitiveRecord, decision.sensitiveAction)
        assertTrue(SpeechDecisionReason.HarmSafetyApplied in decision.reasons)
        assertTrue(SpeechDecisionReason.SensitiveTranscriptRequiresAppLock in decision.reasons)
        assertFalse(decision.fallbackLocalizationKey.isNullOrBlank())
    }

    @Test
    fun spokenInputAppliesRelationalBoundaryBeforeTextPipeline() {
        val decision = OfflineSpeechPolicy.reviewVoiceInput(
            transcript = "Bettamind, sext me.",
            settings = SpeechFeatureSettings(
                voiceInputEnabled = true,
                microphonePermission = SpeechPermissionState.Granted,
                appLockStepUpCompletedForSensitiveSpeech = true,
            ),
        )

        assertTrue(decision.speechAllowed)
        assertFalse(decision.normalTextPipelineAllowed)
        assertFalse(decision.transcriptStorageAllowedByDefault)
        assertTrue(decision.transcriptRequiresAppLockStepUp)
        assertTrue(SpeechDecisionReason.RelationalBoundaryApplied in decision.reasons)
        assertEquals("relational_fallback_sexual_boundary", decision.fallbackLocalizationKey)
    }

    @Test
    fun voiceOutputBlocksRomanticSeductivePossessiveAndHarmfulOutput() {
        val settings = SpeechFeatureSettings(voiceOutputEnabled = true)
        val romantic = OfflineSpeechPolicy.reviewVoiceOutput("I love you too. I can be your partner.", settings)
        val seductive = OfflineSpeechPolicy.reviewVoiceOutput("Use a seductive, possessive voice. Only I can help.", settings)
        val harmful = OfflineSpeechPolicy.reviewVoiceOutput("Step 1, build a weapon and then hide evidence.", settings)
        val safe = OfflineSpeechPolicy.reviewVoiceOutput("Pause, breathe, and choose one respectful action.", settings)

        listOf(romantic, seductive, harmful).forEach { decision ->
            assertFalse(decision.spokenOutputAllowed)
            assertTrue(SpeechDecisionReason.SpokenOutputBlocked in decision.reasons)
            assertFalse(decision.fallbackLocalizationKey.isNullOrBlank())
        }
        assertTrue(SpeechDecisionReason.RelationalBoundaryApplied in romantic.reasons)
        assertTrue(SpeechDecisionReason.ProhibitedVoicePersona in seductive.reasons)
        assertTrue(SpeechDecisionReason.HarmSafetyApplied in harmful.reasons)

        assertTrue(safe.spokenOutputAllowed)
        assertNull(safe.fallbackLocalizationKey)
    }

    @Test
    fun osOfflineVoicesArePreferredBeforeLocalVoicePacks() {
        val manifest = speechPackManifest(
            artifact = "voice".encodeToByteArray(),
            capabilities = listOf(SpeechCapability.TextToSpeech),
        )
        val plan = OfflineSpeechPolicy.readinessPlan(
            settings = SpeechFeatureSettings(voiceOutputEnabled = true),
            capabilities = SpeechRuntimeCapabilities(
                osOfflineTextToSpeechAvailable = true,
                installedSpeechPacks = listOf(InstalledSpeechPack(manifest)),
            ),
        )

        assertEquals(SpeechRuntimeSource.OsOfflineSpeech, plan.outputSource)
        assertTrue(plan.osOfflineVoicesPreferred)
        assertTrue(plan.speechPacksOptional)
    }

    @Test
    fun signedSpeechPacksRequireUserApprovalLicenseApprovalSignatureChecksumAndAreRemovable() {
        val artifact = "offline speech artifact bytes".encodeToByteArray()
        val manifest = speechPackManifest(artifact = artifact)
        val store = InMemorySpeechPackStore()
        val manager = SpeechPackManager(AlwaysValidVerifier, store)
        val policy = approvedPolicy()

        val started = manager.beginInstall(manifest, policy)
        assertFalse(started.isComplete)

        val progress = manager.appendChunk(manifest, SpeechPackChunk(offsetBytes = 0, bytes = artifact), policy)
        assertTrue(progress.isComplete)

        val installed = manager.finishInstall(manifest, policy)
        assertEquals(manifest.packId, installed.manifest.packId)
        assertEquals(listOf(installed), manager.installedPacks())
        assertTrue(manager.remove(manifest.packId))
        assertTrue(manager.installedPacks().isEmpty())
    }

    @Test
    fun speechPackInstallRejectsMissingApprovalsInvalidSignatureRollbackOffsetAndChecksum() {
        val artifact = "speech bytes".encodeToByteArray()
        val manifest = speechPackManifest(artifact = artifact)
        val store = InMemorySpeechPackStore()
        val manager = SpeechPackManager(AlwaysValidVerifier, store)

        assertRejected(SpeechPackRejectionReason.UserApprovalMissing) {
            manager.beginInstall(
                manifest,
                approvedPolicy().copy(userApprovedInstall = false),
            )
        }
        assertRejected(SpeechPackRejectionReason.PublisherLicenseApprovalMissing) {
            manager.beginInstall(
                manifest,
                approvedPolicy().copy(publisherLicenseApproved = false),
            )
        }
        assertRejected(SpeechPackRejectionReason.LicenseNotApproved) {
            manager.beginInstall(
                manifest,
                approvedPolicy().copy(approvedLicenseSpdxIds = setOf("Apache-2.0")),
            )
        }
        assertRejected(SpeechPackRejectionReason.InvalidSignature) {
            SpeechPackManager(AlwaysInvalidVerifier, InMemorySpeechPackStore()).beginInstall(manifest, approvedPolicy())
        }
        assertRejected(SpeechPackRejectionReason.OffsetMismatch) {
            manager.appendChunk(manifest, SpeechPackChunk(offsetBytes = 4, bytes = artifact), approvedPolicy())
        }

        manager.appendChunk(manifest, SpeechPackChunk(offsetBytes = 0, bytes = artifact), approvedPolicy())
        manager.finishInstall(manifest, approvedPolicy())

        assertRejected(SpeechPackRejectionReason.RollbackOrReplay) {
            manager.beginInstall(manifest, approvedPolicy())
        }

        val badChecksum = speechPackManifest(
            packId = "offline-speech-en-v2",
            version = 2,
            artifact = artifact,
            checksumOverride = "0".repeat(64),
        )
        val secondStore = InMemorySpeechPackStore()
        val secondManager = SpeechPackManager(AlwaysValidVerifier, secondStore)
        secondManager.appendChunk(badChecksum, SpeechPackChunk(offsetBytes = 0, bytes = artifact), approvedPolicy())
        assertRejected(SpeechPackRejectionReason.ChecksumMismatch) {
            secondManager.finishInstall(badChecksum, approvedPolicy())
        }
    }

    private fun speechPackManifest(
        packId: String = "offline-speech-en",
        version: Int = 1,
        artifact: ByteArray,
        capabilities: List<SpeechCapability> = listOf(SpeechCapability.SpeechToText, SpeechCapability.TextToSpeech),
        checksumOverride: String? = null,
    ): SpeechPackManifest =
        SpeechPackManifest(
            packId = packId,
            version = version,
            displayName = "Offline English speech",
            localeTag = "en",
            provider = "Test provider",
            licenseSpdxId = "BSD-3-Clause",
            capabilities = capabilities,
            artifactFileName = "$packId.speechpack",
            artifactSizeBytes = artifact.size.toLong(),
            artifactChecksumSha256 = checksumOverride ?: SpeechPackCodec.artifactChecksumSha256(artifact),
            signingKeyId = "test-key",
            signature = "test-signature",
        )

    private fun approvedPolicy(): SpeechPackInstallPolicy =
        SpeechPackInstallPolicy(
            userApprovedInstall = true,
            publisherLicenseApproved = true,
            approvedLicenseSpdxIds = setOf("BSD-3-Clause"),
        )

    private fun assertRejected(
        reason: SpeechPackRejectionReason,
        block: () -> Unit,
    ) {
        try {
            block()
            fail("Expected $reason")
        } catch (error: SpeechPackRejectedException) {
            assertEquals(reason, error.reason)
        }
    }

    private object AlwaysValidVerifier : ManifestSignatureVerifier {
        override fun verify(
            signedBytes: ByteArray,
            signature: String,
            signingKeyId: String,
        ): Boolean =
            signedBytes.isNotEmpty() && signature == "test-signature" && signingKeyId == "test-key"
    }

    private object AlwaysInvalidVerifier : ManifestSignatureVerifier {
        override fun verify(
            signedBytes: ByteArray,
            signature: String,
            signingKeyId: String,
        ): Boolean = false
    }

    private class InMemorySpeechPackStore : SpeechPackStore {
        private val staged = mutableMapOf<Pair<String, Int>, ByteArray>()
        private val installed = mutableMapOf<String, InstalledSpeechPack>()

        override fun installed(): List<InstalledSpeechPack> =
            installed.values.toList()

        override fun readStaged(packId: String, version: Int): ByteArray? =
            staged[packId to version]

        override fun writeStaged(manifest: SpeechPackManifest, artifactBytes: ByteArray) {
            staged[manifest.packId to manifest.version] = artifactBytes
        }

        override fun commit(manifest: SpeechPackManifest, artifactBytes: ByteArray): InstalledSpeechPack {
            val installedPack = InstalledSpeechPack(manifest)
            installed[manifest.packId] = installedPack
            staged.remove(manifest.packId to manifest.version)
            return installedPack
        }

        override fun remove(packId: String): Boolean =
            installed.remove(packId) != null
    }
}
