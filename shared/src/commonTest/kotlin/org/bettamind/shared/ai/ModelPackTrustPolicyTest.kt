package org.bettamind.shared.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelPackTrustPolicyTest {
    @Test
    fun qwenIsTheFirstProductionModelTargetWithOwnerApprovedTrustAnchor() {
        assertEquals("Qwen/Qwen2.5-1.5B-Instruct", BettamindModelPackTrustPolicy.firstProductionModelId)
        assertEquals(
            "qwen2_5_1_5b_instruct_bettamind_v1.litertlm",
            BettamindModelPackTrustPolicy.firstProductionArtifactFileName,
        )
        assertEquals(
            ModelPackTrustReleaseGate.Ready,
            BettamindModelPackTrustPolicy.releaseGate(),
        )
        assertEquals(
            setOf("bettamind-model-prod-2026-01"),
            BettamindModelPackTrustPolicy.approvedSigningKeyIds(),
        )

        val anchor = BettamindModelPackTrustPolicy.productionTrustAnchors.single()
        assertEquals("Ed25519", anchor.algorithm)
        assertEquals("2026-06-27", anchor.approvedOn)
        assertEquals(
            "1B16EFA74603455514E92F78542EA5490FBD5D63291748CCE8650AFEAED01B0A",
            anchor.fingerprintSha256,
        )
        assertTrue(anchor.publicKeyBase64.isNotBlank())
    }
}
