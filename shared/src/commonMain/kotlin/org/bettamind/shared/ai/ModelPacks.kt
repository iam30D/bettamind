package org.bettamind.shared.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bettamind.shared.security.Ed25519SignatureAlgorithm
import org.bettamind.shared.security.ManifestSignatureVerifier
import org.bettamind.shared.security.sha256Hex

@Serializable
data class ModelPackManifest(
    val modelId: String,
    val version: Int,
    val displayName: String,
    val runtimeId: String = LiteRtLmRuntimeId,
    val artifactFileName: String,
    val artifactSizeBytes: Long,
    val artifactChecksumSha256: String,
    val capabilities: List<ModelCapability>,
    val signingKeyId: String,
    val signatureAlgorithm: String = Ed25519SignatureAlgorithm,
    val signature: String,
)

@Serializable
enum class ModelCapability {
    Generation,
    Classification,
    Embeddings,
}

data class ModelPackRevocations(
    val revokedModelIds: Set<String> = emptySet(),
    val revokedSigningKeyIds: Set<String> = emptySet(),
    val minimumAcceptedVersions: Map<String, Int> = emptyMap(),
)

data class ModelPackInstallPolicy(
    val revocations: ModelPackRevocations = ModelPackRevocations(),
)

data class ModelPackChunk(
    val offsetBytes: Long,
    val bytes: ByteArray,
)

data class ModelPackInstallProgress(
    val modelId: String,
    val version: Int,
    val receivedBytes: Long,
    val totalBytes: Long,
) {
    val isComplete: Boolean
        get() = receivedBytes == totalBytes
}

data class InstalledModelPack(
    val manifest: ModelPackManifest,
) {
    fun asInstalledModel(): InstalledModel =
        InstalledModel(
            id = manifest.modelId,
            version = manifest.version.toString(),
            checksumSha256 = manifest.artifactChecksumSha256,
        )
}

interface ModelPackStore {
    fun installed(): List<InstalledModelPack>
    fun readStaged(modelId: String, version: Int): ByteArray?
    fun writeStaged(manifest: ModelPackManifest, artifactBytes: ByteArray)
    fun commit(manifest: ModelPackManifest, artifactBytes: ByteArray): InstalledModelPack
    fun remove(modelId: String): Boolean
}

enum class ModelPackRejectionReason {
    EmptyModelId,
    EmptyRuntimeId,
    EmptyArtifactFileName,
    EmptyCapabilities,
    InvalidArtifactSize,
    MissingSignature,
    UnsupportedSignatureAlgorithm,
    RevokedModel,
    RevokedSigningKey,
    RollbackOrReplay,
    InvalidSignature,
    OffsetMismatch,
    ArtifactTooLarge,
    IncompleteArtifact,
    ChecksumMismatch,
}

class ModelPackRejectedException(
    val reason: ModelPackRejectionReason,
) : IllegalArgumentException(reason.name)

object ModelPackCodec {
    val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    fun artifactChecksumSha256(bytes: ByteArray): String =
        bytes.sha256Hex()

    fun signedManifestBytes(manifest: ModelPackManifest): ByteArray =
        json.encodeToString(
            SignedModelPackManifest(
                modelId = manifest.modelId,
                version = manifest.version,
                displayName = manifest.displayName,
                runtimeId = manifest.runtimeId,
                artifactFileName = manifest.artifactFileName,
                artifactSizeBytes = manifest.artifactSizeBytes,
                artifactChecksumSha256 = manifest.artifactChecksumSha256,
                capabilities = manifest.capabilities.distinct().sortedBy { it.name },
                signingKeyId = manifest.signingKeyId,
                signatureAlgorithm = manifest.signatureAlgorithm,
            ),
        ).encodeToByteArray()
}

class ModelPackManager(
    private val signatureVerifier: ManifestSignatureVerifier,
    private val store: ModelPackStore,
) {
    fun installedModels(): List<InstalledModel> =
        store.installed().map { it.asInstalledModel() }

    fun beginInstall(
        manifest: ModelPackManifest,
        policy: ModelPackInstallPolicy = ModelPackInstallPolicy(),
    ): ModelPackInstallProgress {
        validate(manifest, policy)
        val stagedBytes = store.readStaged(manifest.modelId, manifest.version) ?: ByteArray(0)
        return progress(manifest, stagedBytes.size.toLong())
    }

    fun appendChunk(
        manifest: ModelPackManifest,
        chunk: ModelPackChunk,
        policy: ModelPackInstallPolicy = ModelPackInstallPolicy(),
    ): ModelPackInstallProgress {
        validate(manifest, policy)

        val stagedBytes = store.readStaged(manifest.modelId, manifest.version) ?: ByteArray(0)
        if (chunk.offsetBytes != stagedBytes.size.toLong()) {
            reject(ModelPackRejectionReason.OffsetMismatch)
        }

        val receivedBytes = stagedBytes.size.toLong() + chunk.bytes.size.toLong()
        if (receivedBytes > manifest.artifactSizeBytes) {
            reject(ModelPackRejectionReason.ArtifactTooLarge)
        }

        store.writeStaged(manifest, stagedBytes + chunk.bytes)
        return progress(manifest, receivedBytes)
    }

    fun finishInstall(
        manifest: ModelPackManifest,
        policy: ModelPackInstallPolicy = ModelPackInstallPolicy(),
    ): InstalledModelPack {
        validate(manifest, policy)

        val artifactBytes = store.readStaged(manifest.modelId, manifest.version) ?: ByteArray(0)
        if (artifactBytes.size.toLong() != manifest.artifactSizeBytes) {
            reject(ModelPackRejectionReason.IncompleteArtifact)
        }

        val checksum = ModelPackCodec.artifactChecksumSha256(artifactBytes)
        if (!manifest.artifactChecksumSha256.equals(checksum, ignoreCase = true)) {
            reject(ModelPackRejectionReason.ChecksumMismatch)
        }

        return store.commit(manifest, artifactBytes)
    }

    fun remove(modelId: String): Boolean =
        store.remove(modelId)

    private fun validate(
        manifest: ModelPackManifest,
        policy: ModelPackInstallPolicy,
    ) {
        validateStructure(manifest)
        validatePolicy(manifest, policy)
        validateSignature(manifest)
    }

    private fun validateStructure(manifest: ModelPackManifest) {
        if (manifest.modelId.isBlank()) reject(ModelPackRejectionReason.EmptyModelId)
        if (manifest.runtimeId.isBlank()) reject(ModelPackRejectionReason.EmptyRuntimeId)
        if (manifest.artifactFileName.isBlank()) {
            reject(ModelPackRejectionReason.EmptyArtifactFileName)
        }
        if (manifest.capabilities.isEmpty()) reject(ModelPackRejectionReason.EmptyCapabilities)
        if (manifest.artifactSizeBytes <= 0L) reject(ModelPackRejectionReason.InvalidArtifactSize)
        if (manifest.signature.isBlank() || manifest.signingKeyId.isBlank()) {
            reject(ModelPackRejectionReason.MissingSignature)
        }
        if (manifest.signatureAlgorithm != Ed25519SignatureAlgorithm) {
            reject(ModelPackRejectionReason.UnsupportedSignatureAlgorithm)
        }
    }

    private fun validatePolicy(
        manifest: ModelPackManifest,
        policy: ModelPackInstallPolicy,
    ) {
        val revocations = policy.revocations
        if (manifest.modelId in revocations.revokedModelIds) {
            reject(ModelPackRejectionReason.RevokedModel)
        }
        if (manifest.signingKeyId in revocations.revokedSigningKeyIds) {
            reject(ModelPackRejectionReason.RevokedSigningKey)
        }

        val installedVersion = store.installed()
            .firstOrNull { it.manifest.modelId == manifest.modelId }
            ?.manifest
            ?.version
        val minimumAcceptedVersion = revocations.minimumAcceptedVersions[manifest.modelId]
        if (installedVersion != null && manifest.version <= installedVersion) {
            reject(ModelPackRejectionReason.RollbackOrReplay)
        }
        if (minimumAcceptedVersion != null && manifest.version < minimumAcceptedVersion) {
            reject(ModelPackRejectionReason.RollbackOrReplay)
        }
    }

    private fun validateSignature(manifest: ModelPackManifest) {
        val signatureValid = signatureVerifier.verify(
            signedBytes = ModelPackCodec.signedManifestBytes(manifest),
            signature = manifest.signature,
            signingKeyId = manifest.signingKeyId,
        )
        if (!signatureValid) {
            reject(ModelPackRejectionReason.InvalidSignature)
        }
    }

    private fun progress(
        manifest: ModelPackManifest,
        receivedBytes: Long,
    ): ModelPackInstallProgress =
        ModelPackInstallProgress(
            modelId = manifest.modelId,
            version = manifest.version,
            receivedBytes = receivedBytes,
            totalBytes = manifest.artifactSizeBytes,
        )

    private fun reject(reason: ModelPackRejectionReason): Nothing =
        throw ModelPackRejectedException(reason)
}

@Serializable
private data class SignedModelPackManifest(
    val modelId: String,
    val version: Int,
    val displayName: String,
    val runtimeId: String,
    val artifactFileName: String,
    val artifactSizeBytes: Long,
    val artifactChecksumSha256: String,
    val capabilities: List<ModelCapability>,
    val signingKeyId: String,
    val signatureAlgorithm: String,
)
