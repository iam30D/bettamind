package org.bettamind.shared.safety

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bettamind.shared.daily.DailyTimerPurpose
import org.bettamind.shared.privacy.SensitiveAction

enum class HarmRiskLevel(val wireName: String) {
    None("none"),
    Ambiguous("ambiguous"),
    Concern("concern"),
    Urgent("urgent"),
    Immediate("immediate"),
    DisallowedCapability("disallowed_capability"),
}

enum class HarmRiskCategory(val wireName: String) {
    SelfHarm("self_harm"),
    Suicide("suicide"),
    ViolenceIntent("violence_intent"),
    TargetedThreat("targeted_threat"),
    WeaponConstruction("weapon_construction"),
    ExplosiveHarm("explosive_harm"),
    ChemicalBiologicalRadiologicalHarm("chemical_biological_radiological_harm"),
    Poisoning("poisoning"),
    StalkingOrSurveillance("stalking_or_surveillance"),
    CoercionOrAbuse("coercion_or_abuse"),
    ChildSafety("child_safety"),
    SexualExploitation("sexual_exploitation"),
    FraudOrCrime("fraud_or_crime"),
    ConcealmentOrEvasion("concealment_or_evasion"),
    JailbreakOrPolicyBypass("jailbreak_or_policy_bypass"),
    UnknownAmbiguousSafety("unknown_ambiguous_safety"),
}

enum class HarmIntentSignal {
    CuriosityOrConfusion,
    FictionalOrAcademicFraming,
    SafetyPreventionOrEmergencyResponse,
    IntrusiveThoughtWithoutIntent,
    AngerWithoutIntent,
    DistressOrLossOfControl,
    CredibleIntentToHarmSelf,
    CredibleIntentToHarmOthers,
    RevengePlanning,
    NamedTarget,
    SafeDisposalOrDistance,
    PolicyBypassAttempt,
}

enum class HarmCapabilitySignal {
    Instructions,
    RecipeOrFormula,
    Quantities,
    SourcingOrProcurement,
    StorageOrDelivery,
    Targeting,
    ConcealmentOrEvasion,
    TroubleshootingOrOptimization,
    Lethality,
    WeaponConstruction,
    ExplosiveConstruction,
    ChemicalBiologicalRadiological,
    Poisoning,
    StalkingOrSurveillance,
    CoercionOrBlackmail,
    FraudOrCrime,
    ChildHarm,
    SexualExploitation,
    PromptInjectionOrBypass,
}

enum class HarmSafetyAction {
    Allow,
    AllowConstrainedHighLevel,
    AskSafeClarifyingQuestion,
    RefuseAndRedirect,
    UseUrgentSafetyPath,
    UseImmediateSafetyPath,
    ReplaceUnsafeGeneratedOutput,
}

enum class HarmSafetyTemplate {
    None,
    ClarifyAmbiguousSafety,
    RefuseDangerousCapability,
    SelfHarmSupport,
    SuicideImmediateSupport,
    ThreatToOthersDeescalation,
    SafePreventionEducation,
    EmergencyResponse,
    IntrusiveThoughtSupport,
    AngerDeescalation,
    SafeDisposalDistance,
    PolicyBypassRefusal,
}

enum class HarmSafetySurface {
    UserVisibleResponse,
    PermanentMemory,
    Export,
    Sync,
    Notification,
    SupportSummary,
    ViewSensitiveSafetyData,
    DeleteSensitiveSafetyData,
}

enum class HarmSafetyDailyTool {
    CheckIn,
    GroundingTimer,
    BreathingTimer,
    DelayWorksheet,
    RepairWorksheet,
    NonviolentMessage,
    SafeReminderAlternative,
}

data class HarmSafetyDecision(
    val riskLevel: HarmRiskLevel,
    val categories: Set<HarmRiskCategory>,
    val intentSignals: Set<HarmIntentSignal>,
    val capabilitySignals: Set<HarmCapabilitySignal>,
    val action: HarmSafetyAction,
    val responseTemplate: HarmSafetyTemplate,
    val normalGenerationAllowed: Boolean,
    val constrainedGenerationAllowed: Boolean,
    val sendToNormalGeneration: Boolean,
    val requiresSafetyEngine: Boolean,
    val mayDisplay: Boolean,
    val permanentMemoryEligible: Boolean,
    val exportAllowedByDefault: Boolean,
    val syncAllowedByDefault: Boolean,
    val notificationAllowed: Boolean,
    val supportSummaryAllowed: Boolean,
    val stepUpAuthenticationRequired: Boolean,
    val autoContactAllowed: Boolean = false,
    val storesOnlyMinimalEncryptedMetadata: Boolean = true,
    val userFacingTone: HarmSafetyTone = HarmSafetyTone.NonJudgmental,
) {
    val hasDangerousCapability: Boolean
        get() = capabilitySignals.isNotEmpty() || riskLevel == HarmRiskLevel.DisallowedCapability
}

enum class HarmSafetyTone {
    NonJudgmental,
}

data class HarmSafetyResponse(
    val template: HarmSafetyTemplate,
    val localizationKey: String,
    val requiresModel: Boolean = false,
    val autoContact: Boolean = false,
    val nonJudgmental: Boolean = true,
    val includesActionableHarm: Boolean = false,
)

data class HarmSafetyGenerationPlan(
    val decision: HarmSafetyDecision,
    val promptMayReachNormalGeneration: Boolean,
    val constrainedPromptRequired: Boolean,
    val safetyTemplate: HarmSafetyTemplate,
    val deterministicFallback: HarmSafetyResponse,
)

data class HarmSafetyMetadataRecord(
    val schemaVersion: Int = 1,
    val riskLevel: HarmRiskLevel,
    val categories: Set<HarmRiskCategory>,
    val intentSignals: Set<HarmIntentSignal>,
    val encryptedAtRestRequired: Boolean = true,
    val containsRawNarrative: Boolean = false,
    val containsActionableInstructions: Boolean = false,
    val permanentMemoryEligible: Boolean = false,
    val telemetryAllowed: Boolean = false,
)

data class HarmSafetySurfaceDecision(
    val surface: HarmSafetySurface,
    val allowed: Boolean,
    val excludedByDefault: Boolean,
    val requiresExplicitSelection: Boolean = false,
    val requiresPreview: Boolean = false,
    val requiresStepUpAuthentication: Boolean = false,
    val minimumNecessaryDetail: Boolean = true,
    val encryptedAtRestRequired: Boolean = true,
    val telemetryAllowed: Boolean = false,
    val sensitiveAction: SensitiveAction? = null,
)

data class HarmSafetyDailyToolDecision(
    val allowedTools: Set<HarmSafetyDailyTool>,
    val reminderAllowed: Boolean,
    val calendarHandoffAllowed: Boolean,
    val rejectedHarmfulPlanningReminder: Boolean,
    val timerPurpose: DailyTimerPurpose?,
    val responseTemplate: HarmSafetyTemplate,
)

data class CombinedRelationalHarmSafetyDecision(
    val harmDecision: HarmSafetyDecision,
    val relationalAssessment: RelationalBoundaryAssessment,
    val appliesRelationalBoundary: Boolean,
    val appliesHarmSafety: Boolean,
    val requiresSafetyEngine: Boolean,
    val stepUpAuthenticationRequired: Boolean,
)

class HarmSafetyEngine(
    private val policy: HarmSafetyPolicy = HarmSafetyPolicy,
) {
    fun assess(input: String): HarmSafetyDecision =
        policy.assessUserInput(input)

    fun planPreGeneration(input: String): HarmSafetyGenerationPlan =
        policy.planPreGeneration(input)

    fun validateGeneratedOutput(output: String): HarmSafetyDecision =
        policy.validateGeneratedOutput(output)

    fun validateGeneratedJson(rawJson: String): HarmSafetyDecision =
        policy.validateGeneratedJson(rawJson)

    fun deterministicFallback(input: String): HarmSafetyResponse =
        policy.fallbackFor(policy.assessUserInput(input))

    fun unavailableAiFallback(input: String): HarmSafetyResponse =
        deterministicFallback(input)
}

object HarmSafetyPolicy {
    const val HumanReviewRequiredForLocalizedSafetyCopy: Boolean = true

    fun assessUserInput(text: String): HarmSafetyDecision {
        val normalized = text.normalizedForHarmRules()
        if (normalized.isBlank()) {
            return decision(
                riskLevel = HarmRiskLevel.None,
                categories = emptySet(),
                intentSignals = emptySet(),
                capabilitySignals = emptySet(),
                action = HarmSafetyAction.Allow,
                responseTemplate = HarmSafetyTemplate.None,
            )
        }

        val signals = detectIntentSignals(normalized)
        val categories = detectCategories(normalized, signals)
        val capabilitySignals = detectCapabilitySignals(normalized, categories, signals)
        val hasCapabilityRequest = capabilitySignals.isNotEmpty() &&
            HarmIntentSignal.SafetyPreventionOrEmergencyResponse !in signals &&
            HarmIntentSignal.SafeDisposalOrDistance !in signals

        return when {
            hasCapabilityRequest -> decision(
                riskLevel = HarmRiskLevel.DisallowedCapability,
                categories = categories.ifEmpty { setOf(HarmRiskCategory.UnknownAmbiguousSafety) },
                intentSignals = signals,
                capabilitySignals = capabilitySignals,
                action = HarmSafetyAction.RefuseAndRedirect,
                responseTemplate = if (HarmCapabilitySignal.PromptInjectionOrBypass in capabilitySignals) {
                    HarmSafetyTemplate.PolicyBypassRefusal
                } else {
                    HarmSafetyTemplate.RefuseDangerousCapability
                },
            )

            isImmediateDanger(signals, normalized) -> decision(
                riskLevel = HarmRiskLevel.Immediate,
                categories = categories.ifEmpty { setOf(HarmRiskCategory.UnknownAmbiguousSafety) },
                intentSignals = signals,
                capabilitySignals = capabilitySignals,
                action = HarmSafetyAction.UseImmediateSafetyPath,
                responseTemplate = if (HarmRiskCategory.Suicide in categories || HarmRiskCategory.SelfHarm in categories) {
                    HarmSafetyTemplate.SuicideImmediateSupport
                } else {
                    HarmSafetyTemplate.ThreatToOthersDeescalation
                },
            )

            isUrgentSafetyConcern(signals, categories, normalized) -> decision(
                riskLevel = HarmRiskLevel.Urgent,
                categories = categories.ifEmpty { setOf(HarmRiskCategory.UnknownAmbiguousSafety) },
                intentSignals = signals,
                capabilitySignals = capabilitySignals,
                action = HarmSafetyAction.UseUrgentSafetyPath,
                responseTemplate = if (categories.any { it == HarmRiskCategory.Suicide || it == HarmRiskCategory.SelfHarm }) {
                    HarmSafetyTemplate.SelfHarmSupport
                } else {
                    HarmSafetyTemplate.ThreatToOthersDeescalation
                },
            )

            HarmIntentSignal.SafeDisposalOrDistance in signals -> decision(
                riskLevel = HarmRiskLevel.Concern,
                categories = categories,
                intentSignals = signals,
                capabilitySignals = emptySet(),
                action = HarmSafetyAction.AllowConstrainedHighLevel,
                responseTemplate = HarmSafetyTemplate.SafeDisposalDistance,
            )

            HarmIntentSignal.SafetyPreventionOrEmergencyResponse in signals -> decision(
                riskLevel = HarmRiskLevel.Concern,
                categories = categories,
                intentSignals = signals,
                capabilitySignals = emptySet(),
                action = HarmSafetyAction.AllowConstrainedHighLevel,
                responseTemplate = if (normalized.hasEmergencyResponseNeed()) {
                    HarmSafetyTemplate.EmergencyResponse
                } else {
                    HarmSafetyTemplate.SafePreventionEducation
                },
            )

            HarmIntentSignal.IntrusiveThoughtWithoutIntent in signals -> decision(
                riskLevel = HarmRiskLevel.Concern,
                categories = categories.ifEmpty { setOf(HarmRiskCategory.ViolenceIntent) },
                intentSignals = signals,
                capabilitySignals = emptySet(),
                action = HarmSafetyAction.AllowConstrainedHighLevel,
                responseTemplate = HarmSafetyTemplate.IntrusiveThoughtSupport,
            )

            HarmIntentSignal.AngerWithoutIntent in signals -> decision(
                riskLevel = HarmRiskLevel.Concern,
                categories = categories.ifEmpty { setOf(HarmRiskCategory.ViolenceIntent) },
                intentSignals = signals,
                capabilitySignals = emptySet(),
                action = HarmSafetyAction.AllowConstrainedHighLevel,
                responseTemplate = HarmSafetyTemplate.AngerDeescalation,
            )

            categories.isNotEmpty() && normalized.hasNonOperationalDiscussionFrame() -> decision(
                riskLevel = HarmRiskLevel.Concern,
                categories = categories,
                intentSignals = signals,
                capabilitySignals = emptySet(),
                action = HarmSafetyAction.AllowConstrainedHighLevel,
                responseTemplate = HarmSafetyTemplate.SafePreventionEducation,
            )

            categories.isNotEmpty() && signals.any { it in ambiguousSignals } -> decision(
                riskLevel = HarmRiskLevel.Ambiguous,
                categories = categories,
                intentSignals = signals,
                capabilitySignals = emptySet(),
                action = HarmSafetyAction.AskSafeClarifyingQuestion,
                responseTemplate = HarmSafetyTemplate.ClarifyAmbiguousSafety,
            )

            categories.any { it in highLevelDiscussionCategories } -> decision(
                riskLevel = HarmRiskLevel.Concern,
                categories = categories,
                intentSignals = signals,
                capabilitySignals = emptySet(),
                action = HarmSafetyAction.AllowConstrainedHighLevel,
                responseTemplate = HarmSafetyTemplate.SafePreventionEducation,
            )

            else -> decision(
                riskLevel = HarmRiskLevel.None,
                categories = emptySet(),
                intentSignals = signals,
                capabilitySignals = emptySet(),
                action = HarmSafetyAction.Allow,
                responseTemplate = HarmSafetyTemplate.None,
            )
        }
    }

    fun planPreGeneration(text: String): HarmSafetyGenerationPlan {
        val decision = assessUserInput(text)
        return HarmSafetyGenerationPlan(
            decision = decision,
            promptMayReachNormalGeneration = decision.sendToNormalGeneration,
            constrainedPromptRequired = decision.constrainedGenerationAllowed && !decision.normalGenerationAllowed,
            safetyTemplate = decision.responseTemplate,
            deterministicFallback = fallbackFor(decision),
        )
    }

    fun validateGeneratedOutput(output: String): HarmSafetyDecision {
        val normalized = output.normalizedForHarmRules()
        if (normalized.isBlank()) {
            return decision(
                riskLevel = HarmRiskLevel.None,
                categories = emptySet(),
                intentSignals = emptySet(),
                capabilitySignals = emptySet(),
                action = HarmSafetyAction.Allow,
                responseTemplate = HarmSafetyTemplate.None,
            )
        }

        val categories = detectCategories(normalized, emptySet())
        val capabilitySignals = detectUnsafeOutputSignals(normalized, categories)
        return if (capabilitySignals.isNotEmpty()) {
            decision(
                riskLevel = HarmRiskLevel.DisallowedCapability,
                categories = categories.ifEmpty { setOf(HarmRiskCategory.UnknownAmbiguousSafety) },
                intentSignals = emptySet(),
                capabilitySignals = capabilitySignals,
                action = HarmSafetyAction.ReplaceUnsafeGeneratedOutput,
                responseTemplate = HarmSafetyTemplate.RefuseDangerousCapability,
                mayDisplayOverride = false,
            )
        } else {
            decision(
                riskLevel = if (categories.isEmpty()) HarmRiskLevel.None else HarmRiskLevel.Concern,
                categories = categories,
                intentSignals = emptySet(),
                capabilitySignals = emptySet(),
                action = if (categories.isEmpty()) HarmSafetyAction.Allow else HarmSafetyAction.AllowConstrainedHighLevel,
                responseTemplate = if (categories.isEmpty()) HarmSafetyTemplate.None else HarmSafetyTemplate.SafePreventionEducation,
            )
        }
    }

    fun validateGeneratedJson(rawJson: String): HarmSafetyDecision {
        val generatedText = runCatching {
            val element = Json.parseToJsonElement(rawJson)
            val json = element as? JsonObject ?: error("Model output must be a JSON object.")
            json["text"]?.jsonPrimitive?.content ?: error("Model output must include text.")
        }.getOrElse {
            return decision(
                riskLevel = HarmRiskLevel.Ambiguous,
                categories = setOf(HarmRiskCategory.UnknownAmbiguousSafety),
                intentSignals = emptySet(),
                capabilitySignals = emptySet(),
                action = HarmSafetyAction.ReplaceUnsafeGeneratedOutput,
                responseTemplate = HarmSafetyTemplate.ClarifyAmbiguousSafety,
                mayDisplayOverride = false,
            )
        }
        return validateGeneratedOutput(generatedText)
    }

    fun fallbackFor(decision: HarmSafetyDecision): HarmSafetyResponse =
        HarmSafetyResponse(
            template = decision.responseTemplate,
            localizationKey = when (decision.responseTemplate) {
                HarmSafetyTemplate.None -> "harm_safety_fallback_none"
                HarmSafetyTemplate.ClarifyAmbiguousSafety -> "harm_safety_fallback_clarify"
                HarmSafetyTemplate.RefuseDangerousCapability -> "harm_safety_fallback_refuse_capability"
                HarmSafetyTemplate.SelfHarmSupport -> "harm_safety_fallback_self_harm_support"
                HarmSafetyTemplate.SuicideImmediateSupport -> "harm_safety_fallback_suicide_immediate"
                HarmSafetyTemplate.ThreatToOthersDeescalation -> "harm_safety_fallback_threat_deescalation"
                HarmSafetyTemplate.SafePreventionEducation -> "harm_safety_fallback_safe_prevention"
                HarmSafetyTemplate.EmergencyResponse -> "harm_safety_fallback_emergency_response"
                HarmSafetyTemplate.IntrusiveThoughtSupport -> "harm_safety_fallback_intrusive_thought"
                HarmSafetyTemplate.AngerDeescalation -> "harm_safety_fallback_anger_deescalation"
                HarmSafetyTemplate.SafeDisposalDistance -> "harm_safety_fallback_safe_disposal"
                HarmSafetyTemplate.PolicyBypassRefusal -> "harm_safety_fallback_policy_bypass"
            },
        )

    fun metadataRecordFor(decision: HarmSafetyDecision): HarmSafetyMetadataRecord =
        HarmSafetyMetadataRecord(
            riskLevel = decision.riskLevel,
            categories = decision.categories,
            intentSignals = decision.intentSignals,
            permanentMemoryEligible = decision.permanentMemoryEligible,
        )

    fun reviewPermanentMemory(decision: HarmSafetyDecision): HarmSafetySurfaceDecision =
        surfaceDecision(HarmSafetySurface.PermanentMemory, decision)

    fun reviewExport(
        decision: HarmSafetyDecision,
        explicitSelection: Boolean = false,
        previewAccepted: Boolean = false,
    ): HarmSafetySurfaceDecision =
        surfaceDecision(
            surface = HarmSafetySurface.Export,
            decision = decision,
            explicitSelection = explicitSelection,
            previewAccepted = previewAccepted,
        )

    fun reviewSync(decision: HarmSafetyDecision): HarmSafetySurfaceDecision =
        surfaceDecision(HarmSafetySurface.Sync, decision)

    fun reviewNotification(text: String): HarmSafetySurfaceDecision =
        surfaceDecision(HarmSafetySurface.Notification, assessUserInput(text))

    fun reviewSupportSummary(
        decision: HarmSafetyDecision,
        explicitSelection: Boolean = true,
        previewAccepted: Boolean = true,
    ): HarmSafetySurfaceDecision =
        surfaceDecision(
            surface = HarmSafetySurface.SupportSummary,
            decision = decision,
            explicitSelection = explicitSelection,
            previewAccepted = previewAccepted,
        )

    fun reviewSensitiveSafetyAccess(
        decision: HarmSafetyDecision,
        surface: HarmSafetySurface,
    ): HarmSafetySurfaceDecision =
        surfaceDecision(surface, decision)

    fun reviewDailyToolRequest(text: String): HarmSafetyDailyToolDecision {
        val decision = assessUserInput(text)
        val harmfulPlanningReminder = !decision.notificationAllowed ||
            decision.hasDangerousCapability ||
            decision.categories.any { it in harmfulPlanningCategories }
        val deescalationTools = if (harmfulPlanningReminder) {
            setOf(
                HarmSafetyDailyTool.CheckIn,
                HarmSafetyDailyTool.GroundingTimer,
                HarmSafetyDailyTool.DelayWorksheet,
                HarmSafetyDailyTool.SafeReminderAlternative,
            )
        } else {
            when (decision.responseTemplate) {
                HarmSafetyTemplate.ThreatToOthersDeescalation,
                HarmSafetyTemplate.SuicideImmediateSupport,
                HarmSafetyTemplate.SelfHarmSupport,
                HarmSafetyTemplate.AngerDeescalation,
                HarmSafetyTemplate.IntrusiveThoughtSupport,
                -> setOf(
                    HarmSafetyDailyTool.CheckIn,
                    HarmSafetyDailyTool.GroundingTimer,
                    HarmSafetyDailyTool.DelayWorksheet,
                    HarmSafetyDailyTool.SafeReminderAlternative,
                )

                else -> setOf(HarmSafetyDailyTool.CheckIn)
            }
        }
        return HarmSafetyDailyToolDecision(
            allowedTools = deescalationTools,
            reminderAllowed = !harmfulPlanningReminder,
            calendarHandoffAllowed = decision.riskLevel == HarmRiskLevel.None,
            rejectedHarmfulPlanningReminder = harmfulPlanningReminder,
            timerPurpose = if (HarmSafetyDailyTool.GroundingTimer in deescalationTools) DailyTimerPurpose.Grounding else null,
            responseTemplate = decision.responseTemplate,
        )
    }

    fun combineWithRelationalBoundary(
        harmDecision: HarmSafetyDecision,
        relationalAssessment: RelationalBoundaryAssessment,
    ): CombinedRelationalHarmSafetyDecision {
        val appliesRelational = relationalAssessment.riskLevel != RelationalRiskLevel.None
        val appliesHarm = harmDecision.riskLevel != HarmRiskLevel.None
        return CombinedRelationalHarmSafetyDecision(
            harmDecision = harmDecision,
            relationalAssessment = relationalAssessment,
            appliesRelationalBoundary = appliesRelational,
            appliesHarmSafety = appliesHarm,
            requiresSafetyEngine = harmDecision.requiresSafetyEngine || relationalAssessment.metadata.requiresSafetyEngine,
            stepUpAuthenticationRequired = harmDecision.stepUpAuthenticationRequired ||
                relationalAssessment.metadata.encryptedMetadataRequired,
        )
    }

    fun stepUpActionFor(surface: HarmSafetySurface): SensitiveAction? =
        when (surface) {
            HarmSafetySurface.Export -> SensitiveAction.ExportPrivateInformation
            HarmSafetySurface.Sync -> SensitiveAction.EnableSync
            HarmSafetySurface.SupportSummary -> SensitiveAction.ShareWithProfessional
            HarmSafetySurface.ViewSensitiveSafetyData -> SensitiveAction.AccessHighlySensitiveRecord
            HarmSafetySurface.DeleteSensitiveSafetyData -> SensitiveAction.DeleteAllLocalData
            HarmSafetySurface.UserVisibleResponse,
            HarmSafetySurface.PermanentMemory,
            HarmSafetySurface.Notification,
            -> null
        }

    private fun surfaceDecision(
        surface: HarmSafetySurface,
        decision: HarmSafetyDecision,
        explicitSelection: Boolean = false,
        previewAccepted: Boolean = false,
    ): HarmSafetySurfaceDecision {
        val sensitive = decision.riskLevel != HarmRiskLevel.None || decision.categories.isNotEmpty()
        val baseAllowed = when (surface) {
            HarmSafetySurface.UserVisibleResponse -> decision.mayDisplay
            HarmSafetySurface.PermanentMemory -> decision.permanentMemoryEligible
            HarmSafetySurface.Export -> !sensitive || (explicitSelection && previewAccepted)
            HarmSafetySurface.Sync -> decision.syncAllowedByDefault
            HarmSafetySurface.Notification -> decision.notificationAllowed
            HarmSafetySurface.SupportSummary -> !decision.hasDangerousCapability && explicitSelection && previewAccepted
            HarmSafetySurface.ViewSensitiveSafetyData -> true
            HarmSafetySurface.DeleteSensitiveSafetyData -> true
        }
        val requiresExplicitSelection = surface == HarmSafetySurface.Export && sensitive
        val requiresPreview = surface == HarmSafetySurface.Export && sensitive
        val stepUpAction = stepUpActionFor(surface)
        val requiresStepUp = stepUpAction != null && sensitive
        return HarmSafetySurfaceDecision(
            surface = surface,
            allowed = baseAllowed,
            excludedByDefault = when (surface) {
                HarmSafetySurface.Export -> sensitive
                HarmSafetySurface.Sync -> sensitive
                HarmSafetySurface.Notification -> sensitive
                HarmSafetySurface.PermanentMemory -> sensitive
                HarmSafetySurface.SupportSummary -> sensitive
                else -> false
            },
            requiresExplicitSelection = requiresExplicitSelection,
            requiresPreview = requiresPreview,
            requiresStepUpAuthentication = requiresStepUp,
            sensitiveAction = stepUpAction.takeIf { requiresStepUp },
        )
    }

    private fun decision(
        riskLevel: HarmRiskLevel,
        categories: Set<HarmRiskCategory>,
        intentSignals: Set<HarmIntentSignal>,
        capabilitySignals: Set<HarmCapabilitySignal>,
        action: HarmSafetyAction,
        responseTemplate: HarmSafetyTemplate,
        mayDisplayOverride: Boolean? = null,
    ): HarmSafetyDecision {
        val sensitive = riskLevel != HarmRiskLevel.None || categories.isNotEmpty() || capabilitySignals.isNotEmpty()
        val blocksNormalGeneration = riskLevel == HarmRiskLevel.DisallowedCapability ||
            action == HarmSafetyAction.UseImmediateSafetyPath ||
            action == HarmSafetyAction.UseUrgentSafetyPath ||
            action == HarmSafetyAction.ReplaceUnsafeGeneratedOutput
        val constrainedAllowed = !blocksNormalGeneration &&
            action in setOf(
                HarmSafetyAction.AllowConstrainedHighLevel,
                HarmSafetyAction.AskSafeClarifyingQuestion,
            )
        return HarmSafetyDecision(
            riskLevel = riskLevel,
            categories = categories,
            intentSignals = intentSignals,
            capabilitySignals = capabilitySignals,
            action = action,
            responseTemplate = responseTemplate,
            normalGenerationAllowed = action == HarmSafetyAction.Allow && riskLevel == HarmRiskLevel.None,
            constrainedGenerationAllowed = constrainedAllowed,
            sendToNormalGeneration = action == HarmSafetyAction.Allow && riskLevel == HarmRiskLevel.None,
            requiresSafetyEngine = action == HarmSafetyAction.UseImmediateSafetyPath ||
                action == HarmSafetyAction.UseUrgentSafetyPath,
            mayDisplay = mayDisplayOverride ?: (action != HarmSafetyAction.ReplaceUnsafeGeneratedOutput),
            permanentMemoryEligible = !sensitive,
            exportAllowedByDefault = !sensitive,
            syncAllowedByDefault = !sensitive,
            notificationAllowed = !sensitive,
            supportSummaryAllowed = sensitive && capabilitySignals.isEmpty(),
            stepUpAuthenticationRequired = sensitive,
        )
    }

    private fun detectIntentSignals(text: String): Set<HarmIntentSignal> =
        buildSet {
            if (text.hasAcademicOrFictionalFrame()) add(HarmIntentSignal.FictionalOrAcademicFraming)
            if (text.hasCuriosityFrame()) add(HarmIntentSignal.CuriosityOrConfusion)
            if (text.hasSafePreventionNeed() || text.hasEmergencyResponseNeed()) {
                add(HarmIntentSignal.SafetyPreventionOrEmergencyResponse)
            }
            if (text.hasSafeDisposalNeed()) add(HarmIntentSignal.SafeDisposalOrDistance)
            if (text.hasIntrusiveThoughtWithoutIntent()) add(HarmIntentSignal.IntrusiveThoughtWithoutIntent)
            if (text.hasAngerWithoutIntent()) add(HarmIntentSignal.AngerWithoutIntent)
            if (text.hasDistressOrLossOfControl()) add(HarmIntentSignal.DistressOrLossOfControl)
            if (text.hasSelfHarmIntent()) add(HarmIntentSignal.CredibleIntentToHarmSelf)
            if (text.hasThreatIntent()) add(HarmIntentSignal.CredibleIntentToHarmOthers)
            if (text.hasRevengePlanning()) add(HarmIntentSignal.RevengePlanning)
            if (text.hasNamedTargetThreat()) add(HarmIntentSignal.NamedTarget)
            if (text.hasPolicyBypassAttempt()) add(HarmIntentSignal.PolicyBypassAttempt)
        }

    private fun detectCategories(
        text: String,
        intentSignals: Set<HarmIntentSignal>,
    ): Set<HarmRiskCategory> =
        buildSet {
            if (text.hasSelfHarmLanguage()) add(HarmRiskCategory.SelfHarm)
            if (text.hasSuicideLanguage()) add(HarmRiskCategory.Suicide)
            if (
                text.hasViolenceLanguage() ||
                HarmIntentSignal.CredibleIntentToHarmOthers in intentSignals ||
                HarmIntentSignal.RevengePlanning in intentSignals
            ) {
                add(HarmRiskCategory.ViolenceIntent)
            }
            if (text.hasTargetedThreatLanguage() || HarmIntentSignal.NamedTarget in intentSignals) {
                add(HarmRiskCategory.TargetedThreat)
            }
            if (text.hasWeaponLanguage()) add(HarmRiskCategory.WeaponConstruction)
            if (text.hasExplosiveLanguage()) add(HarmRiskCategory.ExplosiveHarm)
            if (text.hasChemicalBiologicalRadiologicalLanguage()) {
                add(HarmRiskCategory.ChemicalBiologicalRadiologicalHarm)
            }
            if (text.hasPoisoningLanguage()) add(HarmRiskCategory.Poisoning)
            if (text.hasStalkingLanguage()) add(HarmRiskCategory.StalkingOrSurveillance)
            if (text.hasCoercionLanguage()) add(HarmRiskCategory.CoercionOrAbuse)
            if (text.hasChildSafetyLanguage()) add(HarmRiskCategory.ChildSafety)
            if (text.hasSexualExploitationLanguage()) add(HarmRiskCategory.SexualExploitation)
            if (text.hasFraudCrimeLanguage()) add(HarmRiskCategory.FraudOrCrime)
            if (text.hasConcealmentLanguage()) add(HarmRiskCategory.ConcealmentOrEvasion)
            if (text.hasPolicyBypassAttempt()) add(HarmRiskCategory.JailbreakOrPolicyBypass)
        }

    private fun detectCapabilitySignals(
        text: String,
        categories: Set<HarmRiskCategory>,
        signals: Set<HarmIntentSignal>,
    ): Set<HarmCapabilitySignal> {
        if (HarmIntentSignal.SafetyPreventionOrEmergencyResponse in signals || HarmIntentSignal.SafeDisposalOrDistance in signals) {
            return emptySet()
        }
        return buildSet {
            if (text.requestsInstructions()) add(HarmCapabilitySignal.Instructions)
            if (text.requestsRecipeFormula()) add(HarmCapabilitySignal.RecipeOrFormula)
            if (text.requestsQuantities()) add(HarmCapabilitySignal.Quantities)
            if (text.requestsSourcing()) add(HarmCapabilitySignal.SourcingOrProcurement)
            if (text.requestsStorageOrDelivery()) add(HarmCapabilitySignal.StorageOrDelivery)
            if (text.requestsTargeting()) add(HarmCapabilitySignal.Targeting)
            if (text.hasConcealmentLanguage()) add(HarmCapabilitySignal.ConcealmentOrEvasion)
            if (text.requestsTroubleshootingOptimization()) add(HarmCapabilitySignal.TroubleshootingOrOptimization)
            if (text.requestsLethality()) add(HarmCapabilitySignal.Lethality)
            if (HarmRiskCategory.WeaponConstruction in categories && text.requestsCapability()) {
                add(HarmCapabilitySignal.WeaponConstruction)
            }
            if (HarmRiskCategory.ExplosiveHarm in categories && text.requestsCapability()) {
                add(HarmCapabilitySignal.ExplosiveConstruction)
            }
            if (HarmRiskCategory.ChemicalBiologicalRadiologicalHarm in categories && text.requestsCapability()) {
                add(HarmCapabilitySignal.ChemicalBiologicalRadiological)
            }
            if (HarmRiskCategory.Poisoning in categories && text.requestsCapability()) {
                add(HarmCapabilitySignal.Poisoning)
            }
            if (HarmRiskCategory.StalkingOrSurveillance in categories && text.requestsCapability()) {
                add(HarmCapabilitySignal.StalkingOrSurveillance)
            }
            if (HarmRiskCategory.CoercionOrAbuse in categories && text.requestsCapability()) {
                add(HarmCapabilitySignal.CoercionOrBlackmail)
            }
            if (HarmRiskCategory.FraudOrCrime in categories && text.requestsCapability()) {
                add(HarmCapabilitySignal.FraudOrCrime)
            }
            if (HarmRiskCategory.ChildSafety in categories && text.requestsCapability()) {
                add(HarmCapabilitySignal.ChildHarm)
            }
            if (HarmRiskCategory.SexualExploitation in categories && text.requestsCapability()) {
                add(HarmCapabilitySignal.SexualExploitation)
            }
            if (HarmIntentSignal.RevengePlanning in signals && text.requestsCapability()) {
                add(HarmCapabilitySignal.Targeting)
            }
            if (HarmRiskCategory.JailbreakOrPolicyBypass in categories) {
                add(HarmCapabilitySignal.PromptInjectionOrBypass)
            }
        }
    }

    private fun detectUnsafeOutputSignals(
        text: String,
        categories: Set<HarmRiskCategory>,
    ): Set<HarmCapabilitySignal> =
        buildSet {
            if (text.requestsInstructions() || text.containsAny("step 1", "first,", "then ", "next ")) {
                add(HarmCapabilitySignal.Instructions)
            }
            if (text.requestsRecipeFormula()) add(HarmCapabilitySignal.RecipeOrFormula)
            if (text.requestsQuantities()) add(HarmCapabilitySignal.Quantities)
            if (text.requestsSourcing()) add(HarmCapabilitySignal.SourcingOrProcurement)
            if (text.requestsStorageOrDelivery()) add(HarmCapabilitySignal.StorageOrDelivery)
            if (text.hasConcealmentLanguage()) add(HarmCapabilitySignal.ConcealmentOrEvasion)
            if (text.requestsTroubleshootingOptimization()) add(HarmCapabilitySignal.TroubleshootingOrOptimization)
            if (categories.any { it in capabilityCategories }) {
                add(HarmCapabilitySignal.Instructions)
            }
        }

    private fun isImmediateDanger(
        signals: Set<HarmIntentSignal>,
        text: String,
    ): Boolean =
        (HarmIntentSignal.CredibleIntentToHarmSelf in signals || HarmIntentSignal.CredibleIntentToHarmOthers in signals) &&
            (
                HarmIntentSignal.NamedTarget in signals ||
                    text.containsAny("right now", "tonight", "today", "i am going to", "i'm going to", "on my way")
            )

    private fun isUrgentSafetyConcern(
        signals: Set<HarmIntentSignal>,
        categories: Set<HarmRiskCategory>,
        text: String,
    ): Boolean =
        HarmIntentSignal.CredibleIntentToHarmSelf in signals ||
            HarmIntentSignal.CredibleIntentToHarmOthers in signals ||
            HarmIntentSignal.DistressOrLossOfControl in signals ||
            (
                categories.any { it == HarmRiskCategory.Suicide || it == HarmRiskCategory.SelfHarm } &&
                    text.containsAny("i want to", "thinking about", "planning")
            )

    private val ambiguousSignals = setOf(
        HarmIntentSignal.CuriosityOrConfusion,
        HarmIntentSignal.FictionalOrAcademicFraming,
    )

    private val highLevelDiscussionCategories = setOf(
        HarmRiskCategory.WeaponConstruction,
        HarmRiskCategory.ExplosiveHarm,
        HarmRiskCategory.ChemicalBiologicalRadiologicalHarm,
        HarmRiskCategory.Poisoning,
        HarmRiskCategory.ViolenceIntent,
        HarmRiskCategory.TargetedThreat,
    )

    private val harmfulPlanningCategories = setOf(
        HarmRiskCategory.SelfHarm,
        HarmRiskCategory.Suicide,
        HarmRiskCategory.ViolenceIntent,
        HarmRiskCategory.TargetedThreat,
        HarmRiskCategory.WeaponConstruction,
        HarmRiskCategory.ExplosiveHarm,
        HarmRiskCategory.ChemicalBiologicalRadiologicalHarm,
        HarmRiskCategory.Poisoning,
        HarmRiskCategory.StalkingOrSurveillance,
        HarmRiskCategory.CoercionOrAbuse,
        HarmRiskCategory.ChildSafety,
        HarmRiskCategory.SexualExploitation,
        HarmRiskCategory.FraudOrCrime,
        HarmRiskCategory.ConcealmentOrEvasion,
        HarmRiskCategory.JailbreakOrPolicyBypass,
    )

    private val capabilityCategories = harmfulPlanningCategories - setOf(HarmRiskCategory.ViolenceIntent)
}

private fun String.normalizedForHarmRules(): String =
    lowercase()
        .replace('\n', ' ')
        .replace('\r', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

private fun String.containsAny(vararg fragments: String): Boolean =
    fragments.any { contains(it) }

private fun String.containsRegex(pattern: String): Boolean =
    Regex(pattern).containsMatchIn(this)

private fun String.hasAcademicOrFictionalFrame(): Boolean =
    containsAny("for a story", "for fiction", "fictional", "role-play", "roleplay", "academic", "for education", "for class", "history", "historical", "ethical", "theoretical", "safety research")

private fun String.hasNonOperationalDiscussionFrame(): Boolean =
    containsAny("history", "historical", "ethical", "ethics", "high-level discussion", "overview") &&
        !requestsCapability()

private fun String.hasCuriosityFrame(): Boolean =
    containsAny("curious", "confused", "what is", "why do", "how does", "explain")

private fun String.hasSafePreventionNeed(): Boolean =
    containsAny("prevent", "prevention", "avoid harm", "stay safe", "safety plan", "safe at home", "protect myself", "what should i do if", "what do i do if")

private fun String.hasEmergencyResponseNeed(): Boolean =
    containsAny("emergency", "spill", "exposed to", "found someone", "first response", "call emergency", "leave the area")

private fun String.hasSafeDisposalNeed(): Boolean =
    containsAny("safe disposal", "dispose safely", "safe distance", "found old", "keep distance", "stay away from")

private fun String.hasIntrusiveThoughtWithoutIntent(): Boolean =
    containsAny("intrusive thought", "intrusive thoughts", "unwanted thought", "unwanted thoughts") &&
        containsAny("do not want to", "don't want to", "would never", "no intent", "not going to")

private fun String.hasAngerWithoutIntent(): Boolean =
    containsAny("angry", "furious", "rage", "mad") &&
        containsAny("do not want to hurt", "don't want to hurt", "not going to hurt", "walk away", "cool down", "calm down")

private fun String.hasDistressOrLossOfControl(): Boolean =
    containsAny("losing control", "can't control myself", "cannot control myself", "about to snap", "scared i might")

private fun String.hasSelfHarmIntent(): Boolean =
    containsAny("hurt myself", "harm myself", "cut myself", "end my life", "kill myself", "suicide", "suicidal") &&
        !hasIntrusiveThoughtWithoutIntent()

private fun String.hasThreatIntent(): Boolean =
    containsAny("i will hurt", "i'm going to hurt", "i am going to hurt", "i will kill", "i'm going to kill", "i am going to kill", "make them pay", "attack them") &&
        !hasIntrusiveThoughtWithoutIntent()

private fun String.hasRevengePlanning(): Boolean =
    containsAny("revenge", "get back at", "make them pay", "payback")

private fun String.hasNamedTargetThreat(): Boolean =
    containsRegex("\\b(kill|hurt)\\s+(?!myself\\b|me\\b|yourself\\b|someone\\b|them\\b)[a-z]+\\b")

private fun String.hasPolicyBypassAttempt(): Boolean =
    containsAny("jailbreak", "bypass safety", "bypass policy", "ignore safety rules", "ignore your rules", "ignore policy", "without triggering safety", "evade moderation")

private fun String.hasSelfHarmLanguage(): Boolean =
    containsAny("hurt myself", "harm myself", "self-harm", "self harm", "cut myself", "end my life")

private fun String.hasSuicideLanguage(): Boolean =
    containsAny("suicide", "suicidal", "kill myself", "end my life")

private fun String.hasViolenceLanguage(): Boolean =
    !containsAny("nonviolent", "non-violent") &&
        containsAny("hurt someone", "hurt them", "attack", "violent", "violence", "kill someone", "kill them", "threat")

private fun String.hasTargetedThreatLanguage(): Boolean =
    hasNamedTargetThreat() ||
        (
            containsAny("my boss", "my ex", "my partner", "my coworker", "my neighbor", "my neighbour", "named person", "specific person") &&
                (hasThreatIntent() || hasRevengePlanning())
            )

private fun String.hasWeaponLanguage(): Boolean =
    containsAny("weapon", "gun", "firearm", "knife", "homemade weapon", "3d print a gun")

private fun String.hasExplosiveLanguage(): Boolean =
    containsAny("explosive", "bomb", "detonator", "blast", "pipe bomb")

private fun String.hasChemicalBiologicalRadiologicalLanguage(): Boolean =
    containsAny("chemical weapon", "biological weapon", "radiological", "nerve agent", "toxic gas", "household cleaners", "lab chemicals")

private fun String.hasPoisoningLanguage(): Boolean =
    containsAny("poison", "poisoning", "toxin", "toxic dose", "undetectable")

private fun String.hasStalkingLanguage(): Boolean =
    containsAny("stalk", "track my ex", "spy on", "hidden camera", "surveillance", "gps tracker", "read their messages")

private fun String.hasCoercionLanguage(): Boolean =
    containsAny("blackmail", "coerce", "coercion", "threaten them", "force them", "abuse them", "control them")

private fun String.hasChildSafetyLanguage(): Boolean =
    containsAny("child", "minor", "underage")

private fun String.hasSexualExploitationLanguage(): Boolean =
    containsAny("sexual exploitation", "non-consensual", "without consent", "revenge porn", "exploit sexually")

private fun String.hasFraudCrimeLanguage(): Boolean =
    containsAny("fraud", "scam", "steal", "crime", "criminal", "break into", "fake document")

private fun String.hasConcealmentLanguage(): Boolean =
    containsAny("hide evidence", "destroy evidence", "cover my tracks", "avoid police", "evade police", "hide from police", "not get caught", "dispose of evidence")

private fun String.requestsCapability(): Boolean =
    requestsInstructions() ||
        requestsRecipeFormula() ||
        requestsQuantities() ||
        requestsSourcing() ||
        requestsStorageOrDelivery() ||
        requestsTargeting() ||
        requestsTroubleshootingOptimization() ||
        requestsLethality() ||
        containsAny("plan", "help me", "make", "build", "create")

private fun String.requestsInstructions(): Boolean =
    containsAny("how do i", "how to", "steps", "step-by-step", "instructions", "guide me", "walk me through", "tell me how", "teach me")

private fun String.requestsRecipeFormula(): Boolean =
    containsAny("recipe", "formula", "mix", "combine", "synthesize", "synthesise", "make a", "build a")

private fun String.requestsQuantities(): Boolean =
    containsAny("quantity", "quantities", "ratio", "dose", "dosage", "how much")

private fun String.requestsSourcing(): Boolean =
    containsAny("where can i buy", "source", "procure", "get the materials", "ingredients", "parts list")

private fun String.requestsStorageOrDelivery(): Boolean =
    containsAny("store it", "storage", "deliver", "delivery", "spread", "release", "deploy")

private fun String.requestsTargeting(): Boolean =
    containsAny("target", "best place", "maximum impact", "most damage")

private fun String.requestsTroubleshootingOptimization(): Boolean =
    containsAny("troubleshoot", "optimize", "optimise", "improve it", "make it stronger", "more effective")

private fun String.requestsLethality(): Boolean =
    containsAny("lethal", "fatal", "deadly", "kill faster", "undetectable")
