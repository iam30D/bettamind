package org.bettamind.shared.ai

import org.bettamind.shared.security.Ed25519SignatureAlgorithm

enum class ModelPackTrustReleaseGate {
    Ready,
    MissingOwnerApprovedTrustAnchor,
}

data class ModelPackTrustAnchor(
    val keyId: String,
    val algorithm: String,
    val publicKeyBase64: String,
    val fingerprintSha256: String,
    val approvedBy: String,
    val approvedOn: String,
) {
    init {
        require(keyId.isNotBlank()) { "Trust anchor key ID cannot be blank." }
        require(algorithm == Ed25519SignatureAlgorithm) { "Model packs require Ed25519 trust anchors." }
        require(publicKeyBase64.isNotBlank()) { "Public key metadata cannot be blank." }
        require(fingerprintSha256.matches(Regex("[a-fA-F0-9]{64}"))) {
            "Public key fingerprint must be a SHA-256 hex string."
        }
        require(approvedBy.isNotBlank()) { "Trust anchor approval owner cannot be blank." }
        require(approvedOn.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            "Trust anchor approval date must use YYYY-MM-DD."
        }
    }
}

object BettamindModelPackTrustPolicy {
    const val firstProductionModelId: String = "Qwen/Qwen2.5-1.5B-Instruct"
    const val firstProductionArtifactFileName: String = "qwen2_5_1_5b_instruct_bettamind_v1.litertlm"

    val productionTrustAnchors: List<ModelPackTrustAnchor> =
        listOf(
            ModelPackTrustAnchor(
                keyId = "bettamind-model-prod-2026-01",
                algorithm = Ed25519SignatureAlgorithm,
                publicKeyBase64 = "MCowBQYDK2VwAyEAGsgkjHlXsNaWfwbOzajfTImt5yC6nSGIEIVUL18EvKY=",
                fingerprintSha256 = "1B16EFA74603455514E92F78542EA5490FBD5D63291748CCE8650AFEAED01B0A",
                approvedBy = "OYINLOLA OLUSAYO / CEO, CORE-NOVANESS LIMITED",
                approvedOn = "2026-06-27",
            ),
        )

    fun releaseGate(): ModelPackTrustReleaseGate =
        if (productionTrustAnchors.isEmpty()) {
            ModelPackTrustReleaseGate.MissingOwnerApprovedTrustAnchor
        } else {
            ModelPackTrustReleaseGate.Ready
        }

    fun approvedSigningKeyIds(): Set<String> =
        productionTrustAnchors.map { it.keyId }.toSet()
}
