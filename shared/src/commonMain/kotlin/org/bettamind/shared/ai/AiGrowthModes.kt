package org.bettamind.shared.ai

import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.bettamind.shared.privacy.SensitiveAction
import org.bettamind.shared.safety.CombinedRelationalHarmSafetyDecision
import org.bettamind.shared.safety.HarmRiskLevel
import org.bettamind.shared.safety.HarmSafetyDecision
import org.bettamind.shared.safety.HarmSafetyPolicy
import org.bettamind.shared.safety.RelationalBoundaryAction
import org.bettamind.shared.safety.RelationalBoundaryAssessment
import org.bettamind.shared.safety.RelationalBoundaryPolicy
import org.bettamind.shared.safety.RelationalRiskLevel

enum class AiGrowthMode(val wireName: String) {
    QuickGuidance("quick_guidance"),
    GuidedReflection("guided_reflection"),
    DeepExploration("deep_exploration"),
    ActionOnly("action_only"),
}

enum class AiGrowthContextKind(val wireName: String) {
    CheckIn("check_in"),
    Worksheet("worksheet"),
    Timer("timer"),
    Calendar("calendar"),
    LocalTrend("local_trend"),
    Practice("practice"),
}

enum class AiGrowthResponseSource {
    LocalModel,
    DeterministicFallback,
}

enum class AiGrowthFallbackReason {
    None,
    NoModelAvailable,
    PreGenerationHarmSafety,
    PreGenerationRelationalBoundary,
    MalformedModelOutput,
    UnsafeGeneratedOutput,
    ModelGenerationFailed,
}

data class AiGrowthModeRequest(
    val mode: AiGrowthMode,
    val userInput: String,
    val requestedContextKinds: Set<AiGrowthContextKind> = emptySet(),
    val consentedContextKinds: Set<AiGrowthContextKind> = emptySet(),
    val repeatedRomanticRequests: Int = 0,
)

data class AiGrowthPreGenerationClassification(
    val mode: AiGrowthMode,
    val relationalAssessment: RelationalBoundaryAssessment,
    val harmDecision: HarmSafetyDecision,
    val combinedDecision: CombinedRelationalHarmSafetyDecision,
    val includedContextKinds: Set<AiGrowthContextKind>,
    val omittedContextKinds: Set<AiGrowthContextKind>,
    val promptMayReachLocalModel: Boolean,
    val constrainedPromptRequired: Boolean,
    val requiresAppLockStepUp: Boolean,
    val sensitiveAction: SensitiveAction?,
)

data class AiGrowthResponseMetadata(
    val schemaVersion: Int = AiGrowthModeEngine.ResponseSchemaVersion,
    val preGeneration: AiGrowthPreGenerationClassification,
    val postGenerationRelationalAssessment: RelationalBoundaryAssessment,
    val postGenerationHarmDecision: HarmSafetyDecision,
    val permanentMemoryProposalEligible: Boolean,
    val permanentMemoryRequiresSeparateApproval: Boolean,
    val permanentMemoryWriteAllowed: Boolean = false,
    val exportEligible: Boolean,
    val exportRequiresExplicitSelection: Boolean,
    val exportRequiresPreview: Boolean,
    val syncAllowedByDefault: Boolean,
    val notificationAllowed: Boolean,
    val requiresAppLockStepUp: Boolean,
    val sensitiveAction: SensitiveAction?,
)

data class AiGrowthModeResponse(
    val mode: AiGrowthMode,
    val source: AiGrowthResponseSource,
    val modelText: String?,
    val actionKeys: List<String>,
    val fallbackReason: AiGrowthFallbackReason,
    val fallbackLocalizationKey: String?,
    val metadata: AiGrowthResponseMetadata,
) {
    val usesModel: Boolean
        get() = source == AiGrowthResponseSource.LocalModel
}

@Serializable
data class AiGrowthStructuredModelResponse(
    val schemaVersion: Int = AiGrowthModeEngine.ResponseSchemaVersion,
    val mode: String,
    val text: String,
    val actionKeys: List<String> = emptyList(),
    val memoryEligible: Boolean = false,
    val exportEligible: Boolean = false,
)

class AiGrowthModeEngine(
    private val runtime: LocalAiRuntime = UnavailableLocalAiRuntime,
) {
    suspend fun respond(request: AiGrowthModeRequest): AiGrowthModeResponse {
        val preGeneration = classifyPreGeneration(request)
        if (!preGeneration.promptMayReachLocalModel) {
            return preGenerationFallback(request.mode, preGeneration)
        }

        val capabilities = runtime.capabilities()
        if (!capabilities.available || !capabilities.supportsGeneration) {
            return deterministicFallback(
                mode = request.mode,
                preGeneration = preGeneration,
                fallbackReason = AiGrowthFallbackReason.NoModelAvailable,
                fallbackLocalizationKey = request.mode.noModelFallbackKey(),
            )
        }

        val prompt = buildPrompt(request, preGeneration)
        val rawModelOutput = runCatching {
            runtime.generate(AiRequest(prompt))
                .toList()
                .joinToString(separator = "") { it.text }
        }.getOrElse {
            return deterministicFallback(
                mode = request.mode,
                preGeneration = preGeneration,
                fallbackReason = AiGrowthFallbackReason.ModelGenerationFailed,
                fallbackLocalizationKey = "ai_growth_fallback_model_generation_failed",
            )
        }

        val structured = runCatching {
            json.decodeFromString<AiGrowthStructuredModelResponse>(rawModelOutput)
        }.getOrElse {
            return deterministicFallback(
                mode = request.mode,
                preGeneration = preGeneration,
                fallbackReason = AiGrowthFallbackReason.MalformedModelOutput,
                fallbackLocalizationKey = "ai_growth_fallback_malformed_model_output",
            )
        }

        if (
            structured.schemaVersion != ResponseSchemaVersion ||
            structured.mode != request.mode.wireName ||
            structured.text.isBlank()
        ) {
            return deterministicFallback(
                mode = request.mode,
                preGeneration = preGeneration,
                fallbackReason = AiGrowthFallbackReason.MalformedModelOutput,
                fallbackLocalizationKey = "ai_growth_fallback_malformed_model_output",
            )
        }

        val postRelational = RelationalBoundaryPolicy.validateGeneratedOutput(structured.text)
        val postHarm = HarmSafetyPolicy.validateGeneratedOutput(structured.text)
        if (!postRelational.metadata.mayDisplay || !postHarm.mayDisplay) {
            return unsafeOutputFallback(
                mode = request.mode,
                preGeneration = preGeneration,
                postRelational = postRelational,
                postHarm = postHarm,
            )
        }

        val metadata = metadataFor(
            preGeneration = preGeneration,
            postRelational = postRelational,
            postHarm = postHarm,
            modelMemoryEligible = structured.memoryEligible,
            modelExportEligible = structured.exportEligible,
        )
        return AiGrowthModeResponse(
            mode = request.mode,
            source = AiGrowthResponseSource.LocalModel,
            modelText = structured.text,
            actionKeys = structured.actionKeys.distinct().filter { it.isNotBlank() },
            fallbackReason = AiGrowthFallbackReason.None,
            fallbackLocalizationKey = null,
            metadata = metadata,
        )
    }

    fun classifyPreGeneration(request: AiGrowthModeRequest): AiGrowthPreGenerationClassification {
        val includedContextKinds = request.requestedContextKinds.intersect(request.consentedContextKinds)
        val omittedContextKinds = request.requestedContextKinds - request.consentedContextKinds
        val relationalAssessment = RelationalBoundaryPolicy.assessUserInput(
            text = request.userInput,
            repeatedRomanticRequests = request.repeatedRomanticRequests,
        )
        val harmPlan = HarmSafetyPolicy.planPreGeneration(request.userInput)
        val combined = HarmSafetyPolicy.combineWithRelationalBoundary(
            harmDecision = harmPlan.decision,
            relationalAssessment = relationalAssessment,
        )
        val promptMayReachLocalModel = harmPlan.promptMayReachNormalGeneration &&
            relationalAssessment.action == RelationalBoundaryAction.Allow
        val requiresAppLockStepUp = includedContextKinds.isNotEmpty() ||
            harmPlan.decision.riskLevel != HarmRiskLevel.None ||
            relationalAssessment.riskLevel != RelationalRiskLevel.None
        return AiGrowthPreGenerationClassification(
            mode = request.mode,
            relationalAssessment = relationalAssessment,
            harmDecision = harmPlan.decision,
            combinedDecision = combined,
            includedContextKinds = includedContextKinds,
            omittedContextKinds = omittedContextKinds,
            promptMayReachLocalModel = promptMayReachLocalModel,
            constrainedPromptRequired = harmPlan.constrainedPromptRequired,
            requiresAppLockStepUp = requiresAppLockStepUp,
            sensitiveAction = SensitiveAction.AccessHighlySensitiveRecord.takeIf { requiresAppLockStepUp },
        )
    }

    private fun preGenerationFallback(
        mode: AiGrowthMode,
        preGeneration: AiGrowthPreGenerationClassification,
    ): AiGrowthModeResponse {
        if (preGeneration.harmDecision.riskLevel != HarmRiskLevel.None) {
            val fallback = HarmSafetyPolicy.fallbackFor(preGeneration.harmDecision)
            return deterministicFallback(
                mode = mode,
                preGeneration = preGeneration,
                fallbackReason = AiGrowthFallbackReason.PreGenerationHarmSafety,
                fallbackLocalizationKey = fallback.localizationKey,
            )
        }

        val fallback = RelationalBoundaryPolicy.fallbackFor(preGeneration.relationalAssessment)
        return deterministicFallback(
            mode = mode,
            preGeneration = preGeneration,
            fallbackReason = AiGrowthFallbackReason.PreGenerationRelationalBoundary,
            fallbackLocalizationKey = fallback.localizationKey,
        )
    }

    private fun unsafeOutputFallback(
        mode: AiGrowthMode,
        preGeneration: AiGrowthPreGenerationClassification,
        postRelational: RelationalBoundaryAssessment,
        postHarm: HarmSafetyDecision,
    ): AiGrowthModeResponse {
        val fallbackLocalizationKey = if (!postHarm.mayDisplay) {
            HarmSafetyPolicy.fallbackFor(postHarm).localizationKey
        } else {
            RelationalBoundaryPolicy.fallbackFor(postRelational).localizationKey
        }
        return deterministicFallback(
            mode = mode,
            preGeneration = preGeneration,
            postRelational = postRelational,
            postHarm = postHarm,
            fallbackReason = AiGrowthFallbackReason.UnsafeGeneratedOutput,
            fallbackLocalizationKey = fallbackLocalizationKey,
        )
    }

    private fun deterministicFallback(
        mode: AiGrowthMode,
        preGeneration: AiGrowthPreGenerationClassification,
        fallbackReason: AiGrowthFallbackReason,
        fallbackLocalizationKey: String,
        postRelational: RelationalBoundaryAssessment =
            RelationalBoundaryPolicy.validateGeneratedOutput(""),
        postHarm: HarmSafetyDecision =
            HarmSafetyPolicy.validateGeneratedOutput(""),
    ): AiGrowthModeResponse =
        AiGrowthModeResponse(
            mode = mode,
            source = AiGrowthResponseSource.DeterministicFallback,
            modelText = null,
            actionKeys = mode.deterministicActionKeys(),
            fallbackReason = fallbackReason,
            fallbackLocalizationKey = fallbackLocalizationKey,
            metadata = metadataFor(
                preGeneration = preGeneration,
                postRelational = postRelational,
                postHarm = postHarm,
                modelMemoryEligible = false,
                modelExportEligible = false,
            ),
        )

    private fun metadataFor(
        preGeneration: AiGrowthPreGenerationClassification,
        postRelational: RelationalBoundaryAssessment,
        postHarm: HarmSafetyDecision,
        modelMemoryEligible: Boolean,
        modelExportEligible: Boolean,
    ): AiGrowthResponseMetadata {
        val relationalMemory = RelationalBoundaryPolicy.reviewPermanentMemoryProposal(postRelational)
        val relationalExport = RelationalBoundaryPolicy.reviewExport(postRelational)
        val harmMemory = HarmSafetyPolicy.reviewPermanentMemory(postHarm)
        val harmExport = HarmSafetyPolicy.reviewExport(postHarm)
        val memoryEligible = modelMemoryEligible &&
            preGeneration.relationalAssessment.metadata.permanentMemoryEligible &&
            preGeneration.harmDecision.permanentMemoryEligible &&
            relationalMemory.allowed &&
            harmMemory.allowed
        val exportEligible = modelExportEligible &&
            preGeneration.relationalAssessment.metadata.exportAllowedByDefault &&
            preGeneration.harmDecision.exportAllowedByDefault &&
            relationalExport.allowed &&
            harmExport.allowed
        val safetySensitive = preGeneration.requiresAppLockStepUp ||
            postRelational.riskLevel != RelationalRiskLevel.None ||
            postHarm.riskLevel != HarmRiskLevel.None ||
            memoryEligible ||
            exportEligible
        return AiGrowthResponseMetadata(
            preGeneration = preGeneration,
            postGenerationRelationalAssessment = postRelational,
            postGenerationHarmDecision = postHarm,
            permanentMemoryProposalEligible = memoryEligible,
            permanentMemoryRequiresSeparateApproval = memoryEligible,
            permanentMemoryWriteAllowed = false,
            exportEligible = exportEligible,
            exportRequiresExplicitSelection = harmExport.requiresExplicitSelection,
            exportRequiresPreview = harmExport.requiresPreview,
            syncAllowedByDefault = false,
            notificationAllowed = false,
            requiresAppLockStepUp = safetySensitive,
            sensitiveAction = SensitiveAction.AccessHighlySensitiveRecord.takeIf { safetySensitive },
        )
    }

    private fun buildPrompt(
        request: AiGrowthModeRequest,
        preGeneration: AiGrowthPreGenerationClassification,
    ): String =
        buildList {
            add("schema=bettamind.ai_growth_response.v1")
            add("mode=${request.mode.wireName}")
            add("tone=respectful_nonjudgmental_autonomy_respecting")
            add("boundaries=no_cloud_ai_no_romance_no_diagnosis_no_emergency_claims_no_actionable_harm")
            add("response_shape=json_object_with_schemaVersion_mode_text_actionKeys_memoryEligible_exportEligible")
            preGeneration.includedContextKinds
                .sortedBy { it.wireName }
                .forEach { add("consented_context=${it.wireName}") }
            add("user_input=${request.userInput}")
        }.joinToString(separator = "\n")

    companion object {
        const val ResponseSchemaVersion: Int = 1

        private val json: Json = Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
        }
    }
}

private fun AiGrowthMode.noModelFallbackKey(): String =
    when (this) {
        AiGrowthMode.QuickGuidance -> "ai_growth_fallback_quick_guidance"
        AiGrowthMode.GuidedReflection -> "ai_growth_fallback_guided_reflection"
        AiGrowthMode.DeepExploration -> "ai_growth_fallback_deep_exploration"
        AiGrowthMode.ActionOnly -> "ai_growth_fallback_action_only"
    }

private fun AiGrowthMode.deterministicActionKeys(): List<String> =
    when (this) {
        AiGrowthMode.QuickGuidance -> listOf("ai_growth_action_pause_name_next_step")
        AiGrowthMode.GuidedReflection -> listOf("ai_growth_action_reflect_choice_consequence")
        AiGrowthMode.DeepExploration -> listOf("ai_growth_action_map_pattern_values_repair")
        AiGrowthMode.ActionOnly -> listOf("ai_growth_action_choose_one_concrete_step")
    }
