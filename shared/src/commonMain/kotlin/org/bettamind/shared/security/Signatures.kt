package org.bettamind.shared.security

const val Ed25519SignatureAlgorithm = "Ed25519"

interface ManifestSignatureVerifier {
    fun verify(
        signedBytes: ByteArray,
        signature: String,
        signingKeyId: String,
    ): Boolean
}
