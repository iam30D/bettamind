package org.bettamind.shared.knowledge

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val Ed25519SignatureAlgorithm = "Ed25519"

@Serializable
data class KnowledgePackManifest(
    val packId: String,
    val version: Int,
    val locale: String,
    val title: String,
    val payloadChecksumSha256: String,
    val signingKeyId: String,
    val signatureAlgorithm: String = Ed25519SignatureAlgorithm,
    val signature: String,
)

@Serializable
data class KnowledgePackPayload(
    val items: List<KnowledgePackItem>,
)

@Serializable
data class KnowledgePackItem(
    val id: String,
    val title: String,
    val body: String,
    val tags: List<String> = emptyList(),
)

data class InstalledKnowledgePack(
    val manifest: KnowledgePackManifest,
    val payload: KnowledgePackPayload,
)

data class KnowledgePackRevocations(
    val revokedPackIds: Set<String> = emptySet(),
    val revokedSigningKeyIds: Set<String> = emptySet(),
    val minimumAcceptedVersions: Map<String, Int> = emptyMap(),
)

data class KnowledgePackInstallPolicy(
    val installedVersions: Map<String, Int> = emptyMap(),
    val revocations: KnowledgePackRevocations = KnowledgePackRevocations(),
)

enum class KnowledgePackRejectionReason {
    EmptyPackId,
    EmptyLocale,
    EmptyPayload,
    MissingSignature,
    UnsupportedSignatureAlgorithm,
    RevokedPack,
    RevokedSigningKey,
    RollbackOrReplay,
    ChecksumMismatch,
    InvalidSignature,
}

class KnowledgePackRejectedException(
    val reason: KnowledgePackRejectionReason,
) : IllegalArgumentException(reason.name)

interface KnowledgePackSignatureVerifier {
    fun verify(
        signedBytes: ByteArray,
        signature: String,
        signingKeyId: String,
    ): Boolean
}

object KnowledgePackCodec {
    val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    fun payloadBytes(payload: KnowledgePackPayload): ByteArray =
        json.encodeToString(payload).encodeToByteArray()

    fun payloadChecksumSha256(payload: KnowledgePackPayload): String =
        payloadBytes(payload).sha256Hex()

    fun signedManifestBytes(manifest: KnowledgePackManifest): ByteArray =
        json.encodeToString(
            SignedKnowledgePackManifest(
                packId = manifest.packId,
                version = manifest.version,
                locale = manifest.locale,
                title = manifest.title,
                payloadChecksumSha256 = manifest.payloadChecksumSha256,
                signingKeyId = manifest.signingKeyId,
                signatureAlgorithm = manifest.signatureAlgorithm,
            ),
        ).encodeToByteArray()
}

class KnowledgePackInstaller(
    private val signatureVerifier: KnowledgePackSignatureVerifier,
) {
    fun install(
        manifest: KnowledgePackManifest,
        payload: KnowledgePackPayload,
        policy: KnowledgePackInstallPolicy = KnowledgePackInstallPolicy(),
    ): InstalledKnowledgePack {
        validateStructure(manifest, payload)
        validatePolicy(manifest, policy)

        val expectedChecksum = KnowledgePackCodec.payloadChecksumSha256(payload)
        if (!manifest.payloadChecksumSha256.equals(expectedChecksum, ignoreCase = true)) {
            reject(KnowledgePackRejectionReason.ChecksumMismatch)
        }

        val signedBytes = KnowledgePackCodec.signedManifestBytes(manifest)
        val signatureValid = signatureVerifier.verify(
            signedBytes = signedBytes,
            signature = manifest.signature,
            signingKeyId = manifest.signingKeyId,
        )
        if (!signatureValid) {
            reject(KnowledgePackRejectionReason.InvalidSignature)
        }

        return InstalledKnowledgePack(manifest = manifest, payload = payload)
    }

    private fun validateStructure(
        manifest: KnowledgePackManifest,
        payload: KnowledgePackPayload,
    ) {
        if (manifest.packId.isBlank()) reject(KnowledgePackRejectionReason.EmptyPackId)
        if (manifest.locale.isBlank()) reject(KnowledgePackRejectionReason.EmptyLocale)
        if (payload.items.isEmpty()) reject(KnowledgePackRejectionReason.EmptyPayload)
        if (manifest.signature.isBlank() || manifest.signingKeyId.isBlank()) {
            reject(KnowledgePackRejectionReason.MissingSignature)
        }
        if (manifest.signatureAlgorithm != Ed25519SignatureAlgorithm) {
            reject(KnowledgePackRejectionReason.UnsupportedSignatureAlgorithm)
        }
    }

    private fun validatePolicy(
        manifest: KnowledgePackManifest,
        policy: KnowledgePackInstallPolicy,
    ) {
        val revocations = policy.revocations
        if (manifest.packId in revocations.revokedPackIds) {
            reject(KnowledgePackRejectionReason.RevokedPack)
        }
        if (manifest.signingKeyId in revocations.revokedSigningKeyIds) {
            reject(KnowledgePackRejectionReason.RevokedSigningKey)
        }

        val installedVersion = policy.installedVersions[manifest.packId]
        val minimumAcceptedVersion = revocations.minimumAcceptedVersions[manifest.packId]
        if (installedVersion != null && manifest.version <= installedVersion) {
            reject(KnowledgePackRejectionReason.RollbackOrReplay)
        }
        if (minimumAcceptedVersion != null && manifest.version < minimumAcceptedVersion) {
            reject(KnowledgePackRejectionReason.RollbackOrReplay)
        }
    }

    private fun reject(reason: KnowledgePackRejectionReason): Nothing =
        throw KnowledgePackRejectedException(reason)
}

data class KnowledgeSearchResult(
    val packId: String,
    val packVersion: Int,
    val locale: String,
    val item: KnowledgePackItem,
    val score: Int,
)

class LocalKnowledgeRetriever(
    private val packs: List<InstalledKnowledgePack>,
) {
    fun search(
        query: String,
        locale: String? = null,
        limit: Int = 10,
    ): List<KnowledgeSearchResult> {
        if (limit <= 0) return emptyList()

        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return emptyList()

        return packs
            .asSequence()
            .filter { pack -> locale == null || localeMatches(pack.manifest.locale, locale) }
            .flatMap { pack ->
                pack.payload.items.asSequence().mapNotNull { item ->
                    val score = item.scoreAgainst(queryTokens)
                    if (score <= 0) {
                        null
                    } else {
                        KnowledgeSearchResult(
                            packId = pack.manifest.packId,
                            packVersion = pack.manifest.version,
                            locale = pack.manifest.locale,
                            item = item,
                            score = score,
                        )
                    }
                }
            }
            .sortedWith(
                compareByDescending<KnowledgeSearchResult> { it.score }
                    .thenBy { it.packId }
                    .thenBy { it.item.id },
            )
            .take(limit)
            .toList()
    }

    private fun KnowledgePackItem.scoreAgainst(queryTokens: Set<String>): Int {
        val titleTokens = tokenize(title)
        val tagTokens = tags.flatMap { tokenize(it) }.toSet()
        val bodyTokens = tokenize(body)

        return queryTokens.sumOf { token ->
            var score = 0
            if (token in titleTokens) score += 4
            if (token in tagTokens) score += 3
            if (token in bodyTokens) score += 1
            score
        }
    }

    private fun localeMatches(packLocale: String, requestedLocale: String): Boolean {
        val pack = packLocale.trim().lowercase()
        val requested = requestedLocale.trim().lowercase()
        return pack == requested ||
            pack.startsWith("$requested-") ||
            requested.startsWith("$pack-")
    }
}

private fun tokenize(text: String): Set<String> {
    val tokens = mutableSetOf<String>()
    val current = StringBuilder()

    fun flush() {
        if (current.isNotEmpty()) {
            tokens += current.toString()
            current.clear()
        }
    }

    for (char in text.lowercase()) {
        if (char.isLetterOrDigit()) {
            current.append(char)
        } else {
            flush()
        }
    }
    flush()

    return tokens
}

@Serializable
private data class SignedKnowledgePackManifest(
    val packId: String,
    val version: Int,
    val locale: String,
    val title: String,
    val payloadChecksumSha256: String,
    val signingKeyId: String,
    val signatureAlgorithm: String,
)
