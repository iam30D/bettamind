package org.bettamind.shared.ai

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.Flow

interface LocalAiRuntime {
    suspend fun capabilities(): AiCapabilities
    suspend fun load(model: InstalledModel): LoadResult
    fun generate(request: AiRequest): Flow<AiToken>
    suspend fun classify(request: ClassificationRequest): ClassificationResult
    suspend fun embed(texts: List<String>): List<FloatArray>
    suspend fun unload()
}

data class AiCapabilities(
    val available: Boolean,
    val supportsGeneration: Boolean,
    val supportsClassification: Boolean,
    val supportsEmbeddings: Boolean,
)

data class InstalledModel(
    val id: String,
    val version: String,
    val checksumSha256: String,
)

data class AiRequest(val prompt: String)
data class AiToken(val text: String)
data class ClassificationRequest(val text: String)
data class ClassificationResult(val label: String, val confidence: Double)

sealed interface LoadResult {
    data object Loaded : LoadResult
    data class Unavailable(val reason: String) : LoadResult
}

object UnavailableLocalAiRuntime : LocalAiRuntime {
    override suspend fun capabilities(): AiCapabilities =
        AiCapabilities(
            available = false,
            supportsGeneration = false,
            supportsClassification = false,
            supportsEmbeddings = false,
        )

    override suspend fun load(model: InstalledModel): LoadResult =
        LoadResult.Unavailable("local_ai_runtime_unavailable")

    override fun generate(request: AiRequest): Flow<AiToken> = emptyFlow()

    override suspend fun classify(request: ClassificationRequest): ClassificationResult =
        ClassificationResult(label = "unavailable", confidence = 0.0)

    override suspend fun embed(texts: List<String>): List<FloatArray> = emptyList()

    override suspend fun unload() = Unit
}
