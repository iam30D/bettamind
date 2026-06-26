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
    const val firstProductionArtifactFileName: String = "qwen2.5-1.5b-instruct.litertlm"

    val productionTrustAnchors: List<ModelPackTrustAnchor> = emptyList()

    fun releaseGate(): ModelPackTrustReleaseGate =
        if (productionTrustAnchors.isEmpty()) {
            ModelPackTrustReleaseGate.MissingOwnerApprovedTrustAnchor
        } else {
            ModelPackTrustReleaseGate.Ready
        }

    fun approvedSigningKeyIds(): Set<String> =
        productionTrustAnchors.map { it.keyId }.toSet()
}
