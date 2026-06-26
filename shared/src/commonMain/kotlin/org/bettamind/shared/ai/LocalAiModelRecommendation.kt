package org.bettamind.shared.ai

enum class LocalAiDeviceTier {
    Low,
    Standard,
    High,
}

enum class LocalAiModelInstallState {
    NotInstalled,
    Installed,
    Declined,
    Removed,
}

enum class LocalAiModelRecommendationStatus {
    DeterministicFallbackOnly,
    RecommendInstall,
    AlreadyInstalled,
}

data class LocalAiModelRecommendationInput(
    val deviceTier: LocalAiDeviceTier,
    val availableStorageBytes: Long,
    val installState: LocalAiModelInstallState = LocalAiModelInstallState.NotInstalled,
)

data class RecommendedLocalAiModelPack(
    val modelId: String,
    val runtimeId: String,
    val nameLocalizationKey: String,
    val artifactFileName: String,
    val artifactSizeEstimateBytes: Long,
    val recommendedFreeStorageBytes: Long,
    val capabilities: Set<ModelCapability>,
    val licenseSpdxId: String,
    val modelCardUrl: String,
    val licenseUrl: String,
    val ownerLicenseAcceptanceRequired: Boolean,
)

data class LocalAiModelRecommendation(
    val status: LocalAiModelRecommendationStatus,
    val recommendedPack: RecommendedLocalAiModelPack?,
    val userApprovalRequiredBeforeInstall: Boolean,
    val autoInstallAllowed: Boolean,
    val deterministicFallbackAvailable: Boolean,
    val fallbackLocalizationKey: String,
)

object BettamindLocalAiModelCatalog {
    val gemma4E2B: RecommendedLocalAiModelPack =
        RecommendedLocalAiModelPack(
            modelId = "google/gemma-4-E2B-it",
            runtimeId = LiteRtLmRuntimeId,
            nameLocalizationKey = "ai_model_pack_gemma_4_e2b_name",
            artifactFileName = "gemma-4-e2b-it.litertlm",
            artifactSizeEstimateBytes = 2_580_000_000L,
            recommendedFreeStorageBytes = 4_000_000_000L,
            capabilities = setOf(ModelCapability.Generation),
            licenseSpdxId = "Apache-2.0",
            modelCardUrl = "https://ai.google.dev/gemma/docs/core/model_card_4",
            licenseUrl = "https://ai.google.dev/gemma/docs/gemma_4_license",
            ownerLicenseAcceptanceRequired = true,
        )

    val qwen25OnePoint5B: RecommendedLocalAiModelPack =
        RecommendedLocalAiModelPack(
            modelId = "Qwen/Qwen2.5-1.5B-Instruct",
            runtimeId = LiteRtLmRuntimeId,
            nameLocalizationKey = "ai_model_pack_qwen_2_5_1_5b_name",
            artifactFileName = "qwen2.5-1.5b-instruct.litertlm",
            artifactSizeEstimateBytes = 1_600_000_000L,
            recommendedFreeStorageBytes = 2_500_000_000L,
            capabilities = setOf(ModelCapability.Generation),
            licenseSpdxId = "Apache-2.0",
            modelCardUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
            ownerLicenseAcceptanceRequired = true,
        )

    val all: List<RecommendedLocalAiModelPack> =
        listOf(gemma4E2B, qwen25OnePoint5B)
}

object BettamindLocalAiModelPolicy {
    private const val fallbackLocalizationKey = "ai_model_pack_fallback_no_model"

    fun recommendationFor(input: LocalAiModelRecommendationInput): LocalAiModelRecommendation {
        val status = input.installState
        if (status == LocalAiModelInstallState.Installed) {
            return fallbackDecision(LocalAiModelRecommendationStatus.AlreadyInstalled)
        }
        if (status == LocalAiModelInstallState.Declined || status == LocalAiModelInstallState.Removed) {
            return fallbackDecision(LocalAiModelRecommendationStatus.DeterministicFallbackOnly)
        }

        val pack = selectPack(input.availableStorageBytes)
        return if (pack == null) {
            fallbackDecision(LocalAiModelRecommendationStatus.DeterministicFallbackOnly)
        } else {
            LocalAiModelRecommendation(
                status = LocalAiModelRecommendationStatus.RecommendInstall,
                recommendedPack = pack,
                userApprovalRequiredBeforeInstall = true,
                autoInstallAllowed = false,
                deterministicFallbackAvailable = true,
                fallbackLocalizationKey = fallbackLocalizationKey,
            )
        }
    }

    private fun selectPack(availableStorageBytes: Long): RecommendedLocalAiModelPack? {
        val catalog = BettamindLocalAiModelCatalog
        return catalog.qwen25OnePoint5B
            .takeIf { availableStorageBytes >= it.recommendedFreeStorageBytes }
    }

    private fun fallbackDecision(status: LocalAiModelRecommendationStatus): LocalAiModelRecommendation =
        LocalAiModelRecommendation(
            status = status,
            recommendedPack = null,
            userApprovalRequiredBeforeInstall = false,
            autoInstallAllowed = false,
            deterministicFallbackAvailable = true,
            fallbackLocalizationKey = fallbackLocalizationKey,
        )
}
