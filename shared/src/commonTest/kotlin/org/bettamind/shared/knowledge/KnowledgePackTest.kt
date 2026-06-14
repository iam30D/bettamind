package org.bettamind.shared.knowledge

import org.bettamind.shared.security.sha256Hex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KnowledgePackTest {
    private val verifier = TestKnowledgePackSignatureVerifier()
    private val installer = KnowledgePackInstaller(verifier)

    @Test
    fun sha256MatchesKnownVectors() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb924" +
                "27ae41e4649b934ca495991b7852b855",
            "".encodeToByteArray().sha256Hex(),
        )
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223" +
                "b00361a396177a9cb410ff61f20015ad",
            "abc".encodeToByteArray().sha256Hex(),
        )
    }

    @Test
    fun signedManifestAndChecksumAreRequired() {
        val payload = samplePayload()
        val manifest = signedManifest(payload)

        val installed = installer.install(manifest, payload)

        assertEquals("bettamind-foundations", installed.manifest.packId)
        assertEquals(1, installed.manifest.version)

        val checksumFailure = assertFailsWith<KnowledgePackRejectedException> {
            installer.install(
                manifest.copy(payloadChecksumSha256 = "00"),
                payload,
            )
        }
        assertEquals(KnowledgePackRejectionReason.ChecksumMismatch, checksumFailure.reason)

        val signatureFailure = assertFailsWith<KnowledgePackRejectedException> {
            installer.install(
                manifest.copy(signature = "tampered"),
                payload,
            )
        }
        assertEquals(KnowledgePackRejectionReason.InvalidSignature, signatureFailure.reason)
    }

    @Test
    fun rollbackAndRevocationAreRejected() {
        val payload = samplePayload()
        val manifest = signedManifest(payload, version = 2)

        val rollback = assertFailsWith<KnowledgePackRejectedException> {
            installer.install(
                manifest,
                payload,
                KnowledgePackInstallPolicy(
                    installedVersions = mapOf("bettamind-foundations" to 2),
                ),
            )
        }
        assertEquals(KnowledgePackRejectionReason.RollbackOrReplay, rollback.reason)

        val revokedKey = assertFailsWith<KnowledgePackRejectedException> {
            installer.install(
                manifest,
                payload,
                KnowledgePackInstallPolicy(
                    revocations = KnowledgePackRevocations(
                        revokedSigningKeyIds = setOf("bettamind-test-key"),
                    ),
                ),
            )
        }
        assertEquals(KnowledgePackRejectionReason.RevokedSigningKey, revokedKey.reason)

        val revokedVersion = assertFailsWith<KnowledgePackRejectedException> {
            installer.install(
                manifest,
                payload,
                KnowledgePackInstallPolicy(
                    revocations = KnowledgePackRevocations(
                        minimumAcceptedVersions = mapOf("bettamind-foundations" to 3),
                    ),
                ),
            )
        }
        assertEquals(KnowledgePackRejectionReason.RollbackOrReplay, revokedVersion.reason)
    }

    @Test
    fun localRetrievalWorksWithoutNetwork() {
        val payload = samplePayload()
        val installed = installer.install(signedManifest(payload), payload)
        val retriever = LocalKnowledgeRetriever(listOf(installed))

        val results = retriever.search(query = "repair", locale = "en-US")

        assertEquals(1, results.size)
        assertEquals("repair-step", results.single().item.id)
        assertTrue(results.single().score > 0)
    }

    @Test
    fun localeMatchingDoesNotCollapseDifferentScripts() {
        val payload = samplePayload()
        val installed = installer.install(signedManifest(payload, locale = "zh-Hans"), payload)
        val retriever = LocalKnowledgeRetriever(listOf(installed))

        assertEquals(1, retriever.search(query = "repair", locale = "zh").size)
        assertEquals(1, retriever.search(query = "repair", locale = "zh-Hans").size)
        assertEquals(0, retriever.search(query = "repair", locale = "zh-Hant").size)
    }

    @Test
    fun unsupportedSignatureAlgorithmIsRejected() {
        val payload = samplePayload()
        val manifest = signedManifest(payload).copy(signatureAlgorithm = "HMAC-SHA256")

        val failure = assertFailsWith<KnowledgePackRejectedException> {
            installer.install(manifest, payload)
        }

        assertEquals(KnowledgePackRejectionReason.UnsupportedSignatureAlgorithm, failure.reason)
    }

    private fun samplePayload(): KnowledgePackPayload =
        KnowledgePackPayload(
            items = listOf(
                KnowledgePackItem(
                    id = "repair-step",
                    title = "Repair after conflict",
                    body = "Pause, name the consequence and choose the next repair action.",
                    tags = listOf("repair", "growth"),
                ),
            ),
        )

    private fun signedManifest(
        payload: KnowledgePackPayload,
        version: Int = 1,
        locale: String = "en",
    ): KnowledgePackManifest {
        val unsigned = KnowledgePackManifest(
            packId = "bettamind-foundations",
            version = version,
            locale = locale,
            title = "Bettamind foundations",
            payloadChecksumSha256 = KnowledgePackCodec.payloadChecksumSha256(payload),
            signingKeyId = "bettamind-test-key",
            signature = "",
        )
        return unsigned.copy(signature = verifier.sign(unsigned))
    }
}

private class TestKnowledgePackSignatureVerifier : KnowledgePackSignatureVerifier {
    fun sign(manifest: KnowledgePackManifest): String =
        signatureFor(
            signedBytes = KnowledgePackCodec.signedManifestBytes(manifest),
            signingKeyId = manifest.signingKeyId,
        )

    override fun verify(
        signedBytes: ByteArray,
        signature: String,
        signingKeyId: String,
    ): Boolean =
        signature == signatureFor(signedBytes, signingKeyId)

    private fun signatureFor(
        signedBytes: ByteArray,
        signingKeyId: String,
    ): String =
        "test-ed25519:$signingKeyId:${signedBytes.sha256Hex()}"
}
