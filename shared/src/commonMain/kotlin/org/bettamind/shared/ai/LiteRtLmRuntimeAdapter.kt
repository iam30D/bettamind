package org.bettamind.shared.ai

import kotlinx.coroutines.flow.Flow

const val LiteRtLmRuntimeId = "litert-lm"

interface LiteRtLmBridge {
    suspend fun capabilities(): AiCapabilities
    suspend fun load(model: InstalledModel): LoadResult
    fun generate(request: AiRequest): Flow<AiToken>
    suspend fun classify(request: ClassificationRequest): ClassificationResult
    suspend fun embed(texts: List<String>): List<FloatArray>
    suspend fun unload()
}

class LiteRtLmRuntimeAdapter(
    private val bridge: LiteRtLmBridge,
) : LocalAiRuntime {
    override suspend fun capabilities(): AiCapabilities = bridge.capabilities()

    override suspend fun load(model: InstalledModel): LoadResult = bridge.load(model)

    override fun generate(request: AiRequest): Flow<AiToken> =
        bridge.generate(request)

    override suspend fun classify(request: ClassificationRequest): ClassificationResult =
        bridge.classify(request)

    override suspend fun embed(texts: List<String>): List<FloatArray> =
        bridge.embed(texts)

    override suspend fun unload() = bridge.unload()
}
