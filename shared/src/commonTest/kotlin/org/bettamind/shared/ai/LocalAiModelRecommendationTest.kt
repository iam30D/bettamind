package org.bettamind.shared.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalAiModelRecommendationTest {
    @Test
    fun allEligibleDeviceTiersRecommendQwenFirstForReleasePipeline() {
        val lowDecision = BettamindLocalAiModelPolicy.recommendationFor(
            LocalAiModelRecommendationInput(
                deviceTier = LocalAiDeviceTier.Low,
                availableStorageBytes = 5_000_000_000L,
            ),
        )
        val standardDecision = BettamindLocalAiModelPolicy.recommendationFor(
            LocalAiModelRecommendationInput(
                deviceTier = LocalAiDeviceTier.Standard,
                availableStorageBytes = 5_000_000_000L,
            ),
        )
        val highDecision = BettamindLocalAiModelPolicy.recommendationFor(
            LocalAiModelRecommendationInput(
                deviceTier = LocalAiDeviceTier.High,
                availableStorageBytes = 5_000_000_000L,
            ),
        )

        assertEquals(BettamindLocalAiModelCatalog.qwen25OnePoint5B, lowDecision.recommendedPack)
        assertEquals(LocalAiModelRecommendationStatus.RecommendInstall, standardDecision.status)
        assertEquals(BettamindLocalAiModelCatalog.qwen25OnePoint5B, standardDecision.recommendedPack)
        assertEquals(BettamindLocalAiModelCatalog.qwen25OnePoint5B, highDecision.recommendedPack)
        assertRecommendationRequiresExplicitApproval(lowDecision)
        assertRecommendationRequiresExplicitApproval(standardDecision)
        assertRecommendationRequiresExplicitApproval(highDecision)
    }

    @Test
    fun standardDevicesStillUseQwenWhenGemmaStorageWouldHaveBeenAvailable() {
        val storageLimitedDecision = BettamindLocalAiModelPolicy.recommendationFor(
            LocalAiModelRecommendationInput(
                deviceTier = LocalAiDeviceTier.Standard,
                availableStorageBytes = 8_000_000_000L,
            ),
        )

        assertEquals(BettamindLocalAiModelCatalog.qwen25OnePoint5B, storageLimitedDecision.recommendedPack)
        assertRecommendationRequiresExplicitApproval(storageLimitedDecision)
    }

    @Test
    fun insufficientStorageKeepsDeterministicFallbackOnly() {
        val decision = BettamindLocalAiModelPolicy.recommendationFor(
            LocalAiModelRecommendationInput(
                deviceTier = LocalAiDeviceTier.Low,
                availableStorageBytes = 1_000_000_000L,
            ),
        )

        assertEquals(LocalAiModelRecommendationStatus.DeterministicFallbackOnly, decision.status)
        assertNull(decision.recommendedPack)
        assertFalse(decision.autoInstallAllowed)
        assertTrue(decision.deterministicFallbackAvailable)
    }

    @Test
    fun declinedOrRemovedModelDoesNotAutoPromptInstallation() {
        val declined = BettamindLocalAiModelPolicy.recommendationFor(
            LocalAiModelRecommendationInput(
                deviceTier = LocalAiDeviceTier.High,
                availableStorageBytes = 8_000_000_000L,
                installState = LocalAiModelInstallState.Declined,
            ),
        )
        val removed = BettamindLocalAiModelPolicy.recommendationFor(
            LocalAiModelRecommendationInput(
                deviceTier = LocalAiDeviceTier.High,
                availableStorageBytes = 8_000_000_000L,
                installState = LocalAiModelInstallState.Removed,
            ),
        )

        assertEquals(LocalAiModelRecommendationStatus.DeterministicFallbackOnly, declined.status)
        assertEquals(LocalAiModelRecommendationStatus.DeterministicFallbackOnly, removed.status)
        assertNull(declined.recommendedPack)
        assertNull(removed.recommendedPack)
        assertFalse(declined.autoInstallAllowed)
        assertFalse(removed.autoInstallAllowed)
        assertTrue(declined.deterministicFallbackAvailable)
        assertTrue(removed.deterministicFallbackAvailable)
    }

    @Test
    fun catalogUsesLiteRtLmSignedPackInputsAndOwnerLicenseGate() {
        BettamindLocalAiModelCatalog.all.forEach { pack ->
            assertEquals(LiteRtLmRuntimeId, pack.runtimeId)
            assertEquals(setOf(ModelCapability.Generation), pack.capabilities)
            assertEquals("Apache-2.0", pack.licenseSpdxId)
            assertTrue(pack.ownerLicenseAcceptanceRequired)
            assertTrue(pack.artifactFileName.endsWith(".litertlm"))
            assertTrue(pack.artifactSizeEstimateBytes > 0L)
            assertTrue(pack.recommendedFreeStorageBytes > pack.artifactSizeEstimateBytes)
        }
    }

    private fun assertRecommendationRequiresExplicitApproval(decision: LocalAiModelRecommendation) {
        assertEquals(LocalAiModelRecommendationStatus.RecommendInstall, decision.status)
        assertTrue(decision.userApprovalRequiredBeforeInstall)
        assertFalse(decision.autoInstallAllowed)
        assertTrue(decision.deterministicFallbackAvailable)
    }
}
