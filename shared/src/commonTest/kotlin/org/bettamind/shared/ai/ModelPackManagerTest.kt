package org.bettamind.shared.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.bettamind.shared.security.ManifestSignatureVerifier
import org.bettamind.shared.security.sha256Hex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelPackManagerTest {
    private val verifier = TestManifestSignatureVerifier()
    private val store = InMemoryModelPackStore()
    private val manager = ModelPackManager(verifier, store)

    @Test
    fun unavailableRuntimeKeepsAiOptional() = runTest {
        val capabilities = UnavailableLocalAiRuntime.capabilities()
        val loadResult = UnavailableLocalAiRuntime.load(
            InstalledModel(
                id = "missing",
                version = "1",
                checksumSha256 = "none",
            ),
        )

        assertFalse(capabilities.available)
        assertEquals(LoadResult.Unavailable("local_ai_runtime_unavailable"), loadResult)
        assertEquals(emptyList(), UnavailableLocalAiRuntime.generate(AiRequest("hello")).toList())
    }

    @Test
    fun liteRtAdapterDelegatesBehindReplaceableRuntimeInterface() = runTest {
        val bridge = RecordingLiteRtLmBridge()
        val runtime: LocalAiRuntime = LiteRtLmRuntimeAdapter(bridge)
        val model = InstalledModel(id = "lite", version = "1", checksumSha256 = "abc")

        assertTrue(runtime.capabilities().available)
        assertEquals(LoadResult.Loaded, runtime.load(model))
        assertEquals(listOf(AiToken("token")), runtime.generate(AiRequest("prompt")).toList())
        assertEquals(ClassificationResult("local", 1.0), runtime.classify(ClassificationRequest("text")))
        assertEquals(1, runtime.embed(listOf("text")).single().size)
        runtime.unload()

        assertEquals(listOf("capabilities", "load:lite", "generate", "classify", "embed", "unload"), bridge.calls)
    }

    @Test
    fun signedModelPackInstallsInChunksAndIsRemovable() {
        val artifact = "local model bytes".encodeToByteArray()
        val manifest = signedManifest(artifact)

        val first = manager.appendChunk(
            manifest,
            ModelPackChunk(offsetBytes = 0, bytes = artifact.copyOfRange(0, 5)),
        )
        val second = manager.appendChunk(
            manifest,
            ModelPackChunk(offsetBytes = 5, bytes = artifact.copyOfRange(5, artifact.size)),
        )
        val installed = manager.finishInstall(manifest)

        assertEquals(5, first.receivedBytes)
        assertTrue(second.isComplete)
        assertEquals("bettamind-lite-test", installed.manifest.modelId)
        assertEquals(listOf(installed.asInstalledModel()), manager.installedModels())
        assertTrue(manager.remove("bettamind-lite-test"))
        assertEquals(emptyList(), manager.installedModels())
    }

    @Test
    fun partialInstallCanResumeFromStoredOffset() {
        val artifact = "resumable model bytes".encodeToByteArray()
        val manifest = signedManifest(artifact)

        manager.beginInstall(manifest)
        manager.appendChunk(
            manifest,
            ModelPackChunk(offsetBytes = 0, bytes = artifact.copyOfRange(0, 9)),
        )

        val resumedManager = ModelPackManager(verifier, store)
        val progress = resumedManager.beginInstall(manifest)
        resumedManager.appendChunk(
            manifest,
            ModelPackChunk(offsetBytes = 9, bytes = artifact.copyOfRange(9, artifact.size)),
        )

        assertEquals(9, progress.receivedBytes)
        assertEquals(artifact.size.toLong(), resumedManager.finishInstall(manifest).manifest.artifactSizeBytes)
    }

    @Test
    fun signatureChecksumAndOffsetsAreEnforced() {
        val artifact = "verified bytes".encodeToByteArray()
        val manifest = signedManifest(artifact)

        val invalidSignature = assertFailsWith<ModelPackRejectedException> {
            manager.beginInstall(manifest.copy(signature = "tampered"))
        }
        assertEquals(ModelPackRejectionReason.InvalidSignature, invalidSignature.reason)

        val wrongOffset = assertFailsWith<ModelPackRejectedException> {
            manager.appendChunk(
                manifest,
                ModelPackChunk(offsetBytes = 1, bytes = artifact.copyOfRange(0, 3)),
            )
        }
        assertEquals(ModelPackRejectionReason.OffsetMismatch, wrongOffset.reason)

        val tamperedArtifact = artifact.copyOf()
        tamperedArtifact[0] = '!'.code.toByte()
        manager.appendChunk(manifest, ModelPackChunk(offsetBytes = 0, bytes = tamperedArtifact))

        val checksumFailure = assertFailsWith<ModelPackRejectedException> {
            manager.finishInstall(manifest)
        }
        assertEquals(ModelPackRejectionReason.ChecksumMismatch, checksumFailure.reason)
    }

    @Test
    fun rollbackAndRevocationAreRejected() {
        val artifact = "revoked model".encodeToByteArray()
        val versionOne = signedManifest(artifact, version = 1)
        val versionTwo = signedManifest(artifact, version = 2)

        manager.appendChunk(versionTwo, ModelPackChunk(offsetBytes = 0, bytes = artifact))
        manager.finishInstall(versionTwo)

        val rollback = assertFailsWith<ModelPackRejectedException> {
            manager.beginInstall(versionOne)
        }
        assertEquals(ModelPackRejectionReason.RollbackOrReplay, rollback.reason)

        val revokedKey = assertFailsWith<ModelPackRejectedException> {
            manager.beginInstall(
                signedManifest(artifact, version = 3),
                ModelPackInstallPolicy(
                    revocations = ModelPackRevocations(
                        revokedSigningKeyIds = setOf("bettamind-test-model-key"),
                    ),
                ),
            )
        }
        assertEquals(ModelPackRejectionReason.RevokedSigningKey, revokedKey.reason)

        val minimumVersion = assertFailsWith<ModelPackRejectedException> {
            manager.beginInstall(
                signedManifest(artifact, version = 3),
                ModelPackInstallPolicy(
                    revocations = ModelPackRevocations(
                        minimumAcceptedVersions = mapOf("bettamind-lite-test" to 4),
                    ),
                ),
            )
        }
        assertEquals(ModelPackRejectionReason.RollbackOrReplay, minimumVersion.reason)
    }

    private fun signedManifest(
        artifactBytes: ByteArray,
        version: Int = 1,
    ): ModelPackManifest {
        val unsigned = ModelPackManifest(
            modelId = "bettamind-lite-test",
            version = version,
            displayName = "Bettamind Lite test model",
            artifactFileName = "bettamind-lite-test.model",
            artifactSizeBytes = artifactBytes.size.toLong(),
            artifactChecksumSha256 = ModelPackCodec.artifactChecksumSha256(artifactBytes),
            capabilities = listOf(ModelCapability.Classification, ModelCapability.Generation),
            signingKeyId = "bettamind-test-model-key",
            signature = "",
        )
        return unsigned.copy(signature = verifier.sign(ModelPackCodec.signedManifestBytes(unsigned), unsigned.signingKeyId))
    }
}

private class TestManifestSignatureVerifier : ManifestSignatureVerifier {
    fun sign(
        signedBytes: ByteArray,
        signingKeyId: String,
    ): String =
        "test-ed25519:$signingKeyId:${signedBytes.sha256Hex()}"

    override fun verify(
        signedBytes: ByteArray,
        signature: String,
        signingKeyId: String,
    ): Boolean =
        signature == sign(signedBytes, signingKeyId)
}

private class InMemoryModelPackStore : ModelPackStore {
    private val staged = mutableMapOf<Pair<String, Int>, ByteArray>()
    private val installed = mutableMapOf<String, InstalledModelPack>()

    override fun installed(): List<InstalledModelPack> =
        installed.values.sortedBy { it.manifest.modelId }

    override fun readStaged(modelId: String, version: Int): ByteArray? =
        staged[modelId to version]

    override fun writeStaged(
        manifest: ModelPackManifest,
        artifactBytes: ByteArray,
    ) {
        staged[manifest.modelId to manifest.version] = artifactBytes
    }

    override fun commit(
        manifest: ModelPackManifest,
        artifactBytes: ByteArray,
    ): InstalledModelPack {
        check(artifactBytes.isNotEmpty())
        staged.remove(manifest.modelId to manifest.version)
        val installedPack = InstalledModelPack(manifest)
        installed[manifest.modelId] = installedPack
        return installedPack
    }

    override fun remove(modelId: String): Boolean {
        val installedRemoved = installed.remove(modelId) != null
        val stagedKeys = staged.keys.filter { it.first == modelId }
        stagedKeys.forEach { staged.remove(it) }
        return installedRemoved || stagedKeys.isNotEmpty()
    }
}

private class RecordingLiteRtLmBridge : LiteRtLmBridge {
    val calls = mutableListOf<String>()

    override suspend fun capabilities(): AiCapabilities {
        calls += "capabilities"
        return AiCapabilities(
            available = true,
            supportsGeneration = true,
            supportsClassification = true,
            supportsEmbeddings = true,
        )
    }

    override suspend fun load(model: InstalledModel): LoadResult {
        calls += "load:${model.id}"
        return LoadResult.Loaded
    }

    override fun generate(request: AiRequest): Flow<AiToken> {
        calls += "generate"
        return flowOf(AiToken("token"))
    }

    override suspend fun classify(request: ClassificationRequest): ClassificationResult {
        calls += "classify"
        return ClassificationResult("local", 1.0)
    }

    override suspend fun embed(texts: List<String>): List<FloatArray> {
        calls += "embed"
        return listOf(floatArrayOf(1.0f))
    }

    override suspend fun unload() {
        calls += "unload"
    }
}
