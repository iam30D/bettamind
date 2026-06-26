package org.bettamind.shared.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelPackTrustPolicyTest {
    @Test
    fun qwenIsTheFirstProductionModelTargetButReleaseGateWaitsForOwnerTrustAnchor() {
        assertEquals("Qwen/Qwen2.5-1.5B-Instruct", BettamindModelPackTrustPolicy.firstProductionModelId)
        assertEquals("qwen2.5-1.5b-instruct.litertlm", BettamindModelPackTrustPolicy.firstProductionArtifactFileName)
        assertEquals(
            ModelPackTrustReleaseGate.MissingOwnerApprovedTrustAnchor,
            BettamindModelPackTrustPolicy.releaseGate(),
        )
        assertTrue(BettamindModelPackTrustPolicy.approvedSigningKeyIds().isEmpty())
    }
}
