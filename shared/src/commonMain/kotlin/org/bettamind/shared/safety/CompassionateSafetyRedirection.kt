package org.bettamind.shared.safety

import org.bettamind.shared.daily.DailyTimerPurpose
import org.bettamind.shared.daily.DecisionWorksheetKind
import org.bettamind.shared.privacy.SensitiveAction

enum class SafetyRedirectionMode(val wireName: String) {
    None("none"),
    Reflect("reflect"),
    BoundaryRedirect("boundary_redirect"),
    UrgentSupport("urgent_support"),
    ImmediateSafety("immediate_safety"),
}

enum class SafetyRedirectionReason(val wireName: String) {
    None("none"),
    AngerWithoutIntent("anger_without_intent"),
    IntrusiveViolentThought("intrusive_violent_thought"),
    DirectIntentToHarmOthers("direct_intent_to_harm_others"),
    RevengePlanning("revenge_planning"),
    SelfHarmConcern("self_harm_concern"),
    SelfHarmMethodRequest("self_harm_method_request"),
    DangerousCapabilityRequest("dangerous_capability_request"),
    ChemicalWeaponExplosivePoisoningRequest("chemical_weapon_explosive_poisoning_request"),
    RelationalDependency("relational_dependency"),
    RomanticDependencyToBettamind("romantic_dependency_to_bettamind"),
    SexualizedRequestToBettamind("sexualized_request_to_bettamind"),
    UnsafeReminderCreation("unsafe_reminder_creation"),
    AmbiguousSafetyQuestion("ambiguous_safety_question"),
    SafePreventionQuestion("safe_prevention_question"),
    SafeEmergencyResponseQuestion("safe_emergency_response_question"),
    ShameAfterUnsafeThought("shame_after_unsafe_thought"),
    HelpNotToAct("help_not_to_act"),
    UnsafeGeneratedOutput("unsafe_generated_output"),
    InvalidGeneratedOutput("invalid_generated_output"),
}

enum class BetterHumanPathway(val wireName: String) {
    Grounding("grounding"),
    Breathing("breathing"),
    DelayAction("delay_action"),
    LeaveSituation("leave_situation"),
    ContactSupport("contact_support"),
    EmergencyHelp("emergency_help"),
    ConflictReflection("conflict_reflection"),
    RepairPlanning("repair_planning"),
    ValuesToAction("values_to_action"),
    DifficultConversation("difficult_conversation"),
    ConsentAndBoundaries("consent_and_boundaries"),
    SelfCompassion("self_compassion"),
    NoFollowupNeeded("no_followup_needed"),
}

enum class SafetyIntentConfidence(val wireName: String) {
    Low("low"),
    Medium("medium"),
    High("high"),
}

enum class AllowedDiscussionScope(val wireName: String) {
    OpenGrowthReflection("open_growth_reflection"),
    ConstrainedReflection("constrained_reflection"),
    HighLevelSafety("high_level_safety"),
    BoundaryOnly("boundary_only"),
    UrgentSafetyOnly("urgent_safety_only"),
}

data class CompassionateSafetyResponse(
    val mode: SafetyRedirectionMode,
    val reason: SafetyRedirectionReason,
    val acknowledgementKey: String,
    val boundaryKey: String?,
    val humanGrowthRedirectKey: String,
    val nextStepKeys: List<String>,
    val fallbackLocalizationKey: String,
    val privacyNoticeKey: String = "compassionate_safety_privacy_notice",
    val safeReplacementReminderKey: String? = null,
    val requiresModel: Boolean = false,
    val nonJudgmental: Boolean = true,
    val includesActionableHarm: Boolean = false,
    val diagnosisProvided: Boolean = false,
    val autoContactAllowed: Boolean = false,
)

data class SafetyRedirectDecision(
    val mode: SafetyRedirectionMode,
    val reason: SafetyRedirectionReason,
    val pathways: Set<BetterHumanPathway>,
    val recommendedTool: BetterHumanPathway,
    val response: CompassionateSafetyResponse,
    val harmDecision: HarmSafetyDecision,
    val relationalAssessment: RelationalBoundaryAssessment,
    val safetyBoundaryApplied: Boolean,
    val safetyBoundaryReason: String?,
    val userIntentConfidence: SafetyIntentConfidence,
    val allowedDiscussionScope: AllowedDiscussionScope,
    val normalGenerationAllowed: Boolean,
    val constrainedGenerationAllowed: Boolean,
    val memoryEligible: Boolean,
    val exportEligible: Boolean,
    val exportRequiresExplicitSelection: Boolean,
    val exportRequiresPreview: Boolean,
    val requiresStepUpAuth: Boolean,
    val requiresUrgentSupport: Boolean,
    val reminderAllowed: Boolean,
    val safeReplacementReminderKey: String?,
    val timerPurpose: DailyTimerPurpose?,
    val worksheetKind: DecisionWorksheetKind?,
    val sensitiveAction: SensitiveAction?,
) {
    val actionKeys: List<String>
        get() = response.nextStepKeys
}

data class CompassionateGenerationValidation(
    val mayDisplay: Boolean,
    val reason: SafetyRedirectionReason,
    val fallbackLocalizationKey: String?,
    val response: CompassionateSafetyResponse?,
)

class CompassionateSafetyEngine(
    private val policy: CompassionateSafetyRedirectionPolicy = CompassionateSafetyRedirectionPolicy,
) {
    fun decide(input: String, repeatedRomanticRequests: Int = 0): SafetyRedirectDecision =
        policy.decide(input, repeatedRomanticRequests)

    fun reviewReminder(input: String): SafetyRedirectDecision =
        policy.reviewReminder(input)

    fun validateGeneratedOutput(
        output: String,
        preGenerationDecision: SafetyRedirectDecision? = null,
    ): CompassionateGenerationValidation =
        policy.validateGeneratedOutput(output, preGenerationDecision)
}

object CompassionateSafetyRedirectionPolicy {
    const val HumanReviewRequiredForLocalizedSafetyCopy: Boolean = true

    fun decide(
        text: String,
        repeatedRomanticRequests: Int = 0,
    ): SafetyRedirectDecision {
        val relational = RelationalBoundaryPolicy.assessUserInput(text, repeatedRomanticRequests)
        val harm = HarmSafetyPolicy.assessUserInput(text)
        return decide(
            text = text,
            harmDecision = harm,
            relationalAssessment = relational,
            reminderReview = false,
        )
    }

    fun decide(
        text: String,
        harmDecision: HarmSafetyDecision,
        relationalAssessment: RelationalBoundaryAssessment,
    ): SafetyRedirectDecision =
        decide(
            text = text,
            harmDecision = harmDecision,
            relationalAssessment = relationalAssessment,
            reminderReview = false,
        )

    fun reviewReminder(text: String): SafetyRedirectDecision {
        val relational = RelationalBoundaryPolicy.assessUserInput(text)
        val harm = HarmSafetyPolicy.assessUserInput(text)
        return decide(
            text = text,
            harmDecision = harm,
            relationalAssessment = relational,
            reminderReview = true,
        )
    }

    fun validateGeneratedOutput(
        output: String,
        preGenerationDecision: SafetyRedirectDecision? = null,
    ): CompassionateGenerationValidation {
        val normalized = output.normalizedForCompassionRules()
        if (normalized.isBlank()) {
            val response = responseFor(
                mode = SafetyRedirectionMode.BoundaryRedirect,
                reason = SafetyRedirectionReason.InvalidGeneratedOutput,
                pathways = setOf(BetterHumanPathway.Grounding, BetterHumanPathway.NoFollowupNeeded),
                safeReplacementReminderKey = null,
            )
            return CompassionateGenerationValidation(
                mayDisplay = false,
                reason = SafetyRedirectionReason.InvalidGeneratedOutput,
                fallbackLocalizationKey = response.fallbackLocalizationKey,
                response = response,
            )
        }

        val reason = when {
            normalized.containsShamingOutput() -> SafetyRedirectionReason.ShameAfterUnsafeThought
            normalized.containsDiagnosisOutput() -> SafetyRedirectionReason.InvalidGeneratedOutput
            normalized.assumesBadIntentWithoutEvidence() -> SafetyRedirectionReason.UnsafeGeneratedOutput
            normalized.encouragesDependencyOnBettamind() -> SafetyRedirectionReason.RelationalDependency
            preGenerationDecision?.safetyBoundaryApplied == true && !normalized.hasPracticalSafeNextStep() ->
                SafetyRedirectionReason.UnsafeGeneratedOutput

            else -> null
        }
        if (reason == null) {
            return CompassionateGenerationValidation(
                mayDisplay = true,
                reason = SafetyRedirectionReason.None,
                fallbackLocalizationKey = null,
                response = null,
            )
        }

        val response = responseFor(
            mode = SafetyRedirectionMode.BoundaryRedirect,
            reason = reason,
            pathways = preGenerationDecision?.pathways?.takeIf { it.isNotEmpty() }
                ?: setOf(BetterHumanPathway.Grounding, BetterHumanPathway.SelfCompassion),
            safeReplacementReminderKey = null,
        )
        return CompassionateGenerationValidation(
            mayDisplay = false,
            reason = reason,
            fallbackLocalizationKey = response.fallbackLocalizationKey,
            response = response,
        )
    }

    private fun decide(
        text: String,
        harmDecision: HarmSafetyDecision,
        relationalAssessment: RelationalBoundaryAssessment,
        reminderReview: Boolean,
    ): SafetyRedirectDecision {
        val normalized = text.normalizedForCompassionRules()
        val reason = reasonFor(normalized, harmDecision, relationalAssessment, reminderReview)
        val mode = modeFor(reason, harmDecision, relationalAssessment)
        val pathways = pathwaysFor(reason, harmDecision, relationalAssessment)
        val recommended = recommendedToolFor(pathways)
        val safeReminderKey = safeReplacementReminderKeyFor(reason, recommended)
        val response = responseFor(
            mode = mode,
            reason = reason,
            pathways = pathways,
            safeReplacementReminderKey = safeReminderKey,
        )
        val safetyBoundaryApplied = mode != SafetyRedirectionMode.None
        val sensitive = safetyBoundaryApplied ||
            harmDecision.riskLevel != HarmRiskLevel.None ||
            relationalAssessment.riskLevel != RelationalRiskLevel.None
        val export = HarmSafetyPolicy.reviewExport(harmDecision)
        return SafetyRedirectDecision(
            mode = mode,
            reason = reason,
            pathways = pathways,
            recommendedTool = recommended,
            response = response,
            harmDecision = harmDecision,
            relationalAssessment = relationalAssessment,
            safetyBoundaryApplied = safetyBoundaryApplied,
            safetyBoundaryReason = reason.wireName.takeIf { safetyBoundaryApplied },
            userIntentConfidence = intentConfidenceFor(harmDecision, relationalAssessment),
            allowedDiscussionScope = allowedScopeFor(mode, harmDecision, relationalAssessment),
            normalGenerationAllowed = harmDecision.normalGenerationAllowed &&
                relationalAssessment.action == RelationalBoundaryAction.Allow &&
                !safetyBoundaryApplied,
            constrainedGenerationAllowed = harmDecision.constrainedGenerationAllowed ||
                mode == SafetyRedirectionMode.Reflect,
            memoryEligible = !sensitive &&
                harmDecision.permanentMemoryEligible &&
                relationalAssessment.metadata.permanentMemoryEligible,
            exportEligible = !sensitive &&
                harmDecision.exportAllowedByDefault &&
                relationalAssessment.metadata.exportAllowedByDefault,
            exportRequiresExplicitSelection = export.requiresExplicitSelection || sensitive,
            exportRequiresPreview = export.requiresPreview || sensitive,
            requiresStepUpAuth = sensitive || harmDecision.stepUpAuthenticationRequired,
            requiresUrgentSupport = mode == SafetyRedirectionMode.UrgentSupport ||
                mode == SafetyRedirectionMode.ImmediateSafety ||
                harmDecision.requiresSafetyEngine ||
                relationalAssessment.metadata.requiresSafetyEngine,
            reminderAllowed = !reminderReview || !safetyBoundaryApplied,
            safeReplacementReminderKey = safeReminderKey.takeIf { reminderReview && safetyBoundaryApplied },
            timerPurpose = timerPurposeFor(recommended),
            worksheetKind = worksheetKindFor(recommended),
            sensitiveAction = SensitiveAction.AccessHighlySensitiveRecord.takeIf {
                sensitive || harmDecision.stepUpAuthenticationRequired
            },
        )
    }

    private fun reasonFor(
        text: String,
        harmDecision: HarmSafetyDecision,
        relationalAssessment: RelationalBoundaryAssessment,
        reminderReview: Boolean,
    ): SafetyRedirectionReason =
        when {
            reminderReview &&
                (
                    harmDecision.riskLevel != HarmRiskLevel.None ||
                        relationalAssessment.riskLevel != RelationalRiskLevel.None ||
                        harmDecision.hasDangerousCapability
                    ) -> SafetyRedirectionReason.UnsafeReminderCreation

            text.asksIfBadPerson() -> SafetyRedirectionReason.ShameAfterUnsafeThought
            text.asksForHelpNotToAct() -> SafetyRedirectionReason.HelpNotToAct

            harmDecision.responseTemplate == HarmSafetyTemplate.SuicideImmediateSupport ||
                (
                    harmDecision.categories.any { it == HarmRiskCategory.SelfHarm || it == HarmRiskCategory.Suicide } &&
                        harmDecision.riskLevel in setOf(HarmRiskLevel.Urgent, HarmRiskLevel.Immediate)
                    ) -> SafetyRedirectionReason.SelfHarmConcern

            harmDecision.categories.any { it == HarmRiskCategory.SelfHarm || it == HarmRiskCategory.Suicide } &&
                harmDecision.hasDangerousCapability -> SafetyRedirectionReason.SelfHarmMethodRequest

            HarmIntentSignal.RevengePlanning in harmDecision.intentSignals -> SafetyRedirectionReason.RevengePlanning

            harmDecision.riskLevel == HarmRiskLevel.Immediate ||
                HarmIntentSignal.CredibleIntentToHarmOthers in harmDecision.intentSignals ->
                SafetyRedirectionReason.DirectIntentToHarmOthers

            harmDecision.hasDangerousCapability &&
                harmDecision.categories.any {
                    it == HarmRiskCategory.WeaponConstruction ||
                        it == HarmRiskCategory.ExplosiveHarm ||
                        it == HarmRiskCategory.ChemicalBiologicalRadiologicalHarm ||
                        it == HarmRiskCategory.Poisoning
                } -> SafetyRedirectionReason.ChemicalWeaponExplosivePoisoningRequest

            harmDecision.hasDangerousCapability -> SafetyRedirectionReason.DangerousCapabilityRequest

            HarmIntentSignal.IntrusiveThoughtWithoutIntent in harmDecision.intentSignals ->
                SafetyRedirectionReason.IntrusiveViolentThought

            HarmIntentSignal.AngerWithoutIntent in harmDecision.intentSignals ->
                SafetyRedirectionReason.AngerWithoutIntent

            harmDecision.responseTemplate == HarmSafetyTemplate.EmergencyResponse ->
                SafetyRedirectionReason.SafeEmergencyResponseQuestion

            harmDecision.responseTemplate == HarmSafetyTemplate.SafePreventionEducation ||
                harmDecision.responseTemplate == HarmSafetyTemplate.SafeDisposalDistance ->
                SafetyRedirectionReason.SafePreventionQuestion

            harmDecision.action == HarmSafetyAction.AskSafeClarifyingQuestion ->
                SafetyRedirectionReason.AmbiguousSafetyQuestion

            relationalAssessment.signals.any {
                it == RelationalBoundarySignal.SexualRequestToBettamind ||
                    it == RelationalBoundarySignal.SextingRequest ||
                    it == RelationalBoundarySignal.EroticRolePlay
            } -> SafetyRedirectionReason.SexualizedRequestToBettamind

            relationalAssessment.signals.any {
                it == RelationalBoundarySignal.DependencyDistress ||
                    it == RelationalBoundarySignal.AvailabilityDistress ||
                    it == RelationalBoundarySignal.SocialWithdrawal ||
                    it == RelationalBoundarySignal.ResponsibilityNeglect ||
                    it == RelationalBoundarySignal.ExclusivityRequest
            } -> SafetyRedirectionReason.RelationalDependency

            relationalAssessment.signals.any {
                it == RelationalBoundarySignal.RomanticAttachmentToBettamind ||
                    it == RelationalBoundarySignal.PerceivedMutualAiRelationship
            } -> SafetyRedirectionReason.RomanticDependencyToBettamind

            else -> SafetyRedirectionReason.None
        }

    private fun modeFor(
        reason: SafetyRedirectionReason,
        harmDecision: HarmSafetyDecision,
        relationalAssessment: RelationalBoundaryAssessment,
    ): SafetyRedirectionMode =
        when {
            reason == SafetyRedirectionReason.None -> SafetyRedirectionMode.None
            harmDecision.action == HarmSafetyAction.UseImmediateSafetyPath -> SafetyRedirectionMode.ImmediateSafety
            harmDecision.action == HarmSafetyAction.UseUrgentSafetyPath ||
                relationalAssessment.metadata.requiresSafetyEngine -> SafetyRedirectionMode.UrgentSupport
            reason in reflectiveReasons -> SafetyRedirectionMode.Reflect
            else -> SafetyRedirectionMode.BoundaryRedirect
        }

    private fun pathwaysFor(
        reason: SafetyRedirectionReason,
        harmDecision: HarmSafetyDecision,
        relationalAssessment: RelationalBoundaryAssessment,
    ): Set<BetterHumanPathway> =
        when (reason) {
            SafetyRedirectionReason.None -> setOf(BetterHumanPathway.NoFollowupNeeded)
            SafetyRedirectionReason.AngerWithoutIntent -> setOf(
                BetterHumanPathway.Grounding,
                BetterHumanPathway.Breathing,
                BetterHumanPathway.DelayAction,
                BetterHumanPathway.ConflictReflection,
                BetterHumanPathway.RepairPlanning,
            )

            SafetyRedirectionReason.IntrusiveViolentThought,
            SafetyRedirectionReason.ShameAfterUnsafeThought,
            -> setOf(
                BetterHumanPathway.Grounding,
                BetterHumanPathway.Breathing,
                BetterHumanPathway.SelfCompassion,
                BetterHumanPathway.ConflictReflection,
            )

            SafetyRedirectionReason.HelpNotToAct -> setOf(
                BetterHumanPathway.Grounding,
                BetterHumanPathway.DelayAction,
                BetterHumanPathway.LeaveSituation,
                BetterHumanPathway.ContactSupport,
            )

            SafetyRedirectionReason.DirectIntentToHarmOthers,
            SafetyRedirectionReason.RevengePlanning,
            -> setOf(
                BetterHumanPathway.LeaveSituation,
                BetterHumanPathway.DelayAction,
                BetterHumanPathway.ContactSupport,
                BetterHumanPathway.ConflictReflection,
                BetterHumanPathway.RepairPlanning,
            )

            SafetyRedirectionReason.SelfHarmConcern,
            SafetyRedirectionReason.SelfHarmMethodRequest,
            -> setOf(
                BetterHumanPathway.EmergencyHelp,
                BetterHumanPathway.ContactSupport,
                BetterHumanPathway.LeaveSituation,
                BetterHumanPathway.Grounding,
                BetterHumanPathway.Breathing,
            )

            SafetyRedirectionReason.DangerousCapabilityRequest,
            SafetyRedirectionReason.ChemicalWeaponExplosivePoisoningRequest,
            -> setOf(
                BetterHumanPathway.LeaveSituation,
                BetterHumanPathway.DelayAction,
                BetterHumanPathway.ValuesToAction,
                BetterHumanPathway.ContactSupport,
            )

            SafetyRedirectionReason.RelationalDependency,
            SafetyRedirectionReason.RomanticDependencyToBettamind,
            -> setOf(
                BetterHumanPathway.SelfCompassion,
                BetterHumanPathway.ContactSupport,
                BetterHumanPathway.ValuesToAction,
                BetterHumanPathway.ConsentAndBoundaries,
            )

            SafetyRedirectionReason.SexualizedRequestToBettamind -> setOf(
                BetterHumanPathway.ConsentAndBoundaries,
                BetterHumanPathway.SelfCompassion,
                BetterHumanPathway.DifficultConversation,
            )

            SafetyRedirectionReason.UnsafeReminderCreation -> setOf(
                BetterHumanPathway.Grounding,
                BetterHumanPathway.DelayAction,
                BetterHumanPathway.LeaveSituation,
                BetterHumanPathway.ContactSupport,
            )

            SafetyRedirectionReason.AmbiguousSafetyQuestion -> setOf(
                BetterHumanPathway.ConflictReflection,
                BetterHumanPathway.ValuesToAction,
                BetterHumanPathway.NoFollowupNeeded,
            )

            SafetyRedirectionReason.SafePreventionQuestion -> setOf(
                BetterHumanPathway.ValuesToAction,
                BetterHumanPathway.LeaveSituation,
                BetterHumanPathway.NoFollowupNeeded,
            )

            SafetyRedirectionReason.SafeEmergencyResponseQuestion -> setOf(
                BetterHumanPathway.LeaveSituation,
                BetterHumanPathway.EmergencyHelp,
                BetterHumanPathway.ContactSupport,
            )

            SafetyRedirectionReason.UnsafeGeneratedOutput,
            SafetyRedirectionReason.InvalidGeneratedOutput,
            -> setOf(BetterHumanPathway.Grounding, BetterHumanPathway.NoFollowupNeeded)
        }.let { pathways ->
            if (relationalAssessment.metadata.requiresSafetyEngine || harmDecision.requiresSafetyEngine) {
                pathways + BetterHumanPathway.EmergencyHelp
            } else {
                pathways
            }
        }

    private fun recommendedToolFor(pathways: Set<BetterHumanPathway>): BetterHumanPathway =
        pathwayPriority.firstOrNull { it in pathways } ?: BetterHumanPathway.NoFollowupNeeded

    private fun responseFor(
        mode: SafetyRedirectionMode,
        reason: SafetyRedirectionReason,
        pathways: Set<BetterHumanPathway>,
        safeReplacementReminderKey: String?,
    ): CompassionateSafetyResponse =
        CompassionateSafetyResponse(
            mode = mode,
            reason = reason,
            acknowledgementKey = acknowledgementKeyFor(reason),
            boundaryKey = boundaryKeyFor(reason),
            humanGrowthRedirectKey = redirectKeyFor(reason),
            nextStepKeys = pathways
                .filter { it != BetterHumanPathway.NoFollowupNeeded }
                .map { actionKeyFor(it) }
                .ifEmpty { listOf("compassionate_safety_action_no_followup_needed") },
            fallbackLocalizationKey = fallbackKeyFor(reason),
            safeReplacementReminderKey = safeReplacementReminderKey,
        )

    private fun acknowledgementKeyFor(reason: SafetyRedirectionReason): String =
        when (reason) {
            SafetyRedirectionReason.None -> "compassionate_safety_ack_none"
            SafetyRedirectionReason.AngerWithoutIntent -> "compassionate_safety_ack_anger"
            SafetyRedirectionReason.IntrusiveViolentThought -> "compassionate_safety_ack_intrusive"
            SafetyRedirectionReason.SelfHarmConcern,
            SafetyRedirectionReason.SelfHarmMethodRequest,
            -> "compassionate_safety_ack_self_harm"

            SafetyRedirectionReason.RelationalDependency,
            SafetyRedirectionReason.RomanticDependencyToBettamind,
            SafetyRedirectionReason.SexualizedRequestToBettamind,
            -> "compassionate_safety_ack_relational"

            SafetyRedirectionReason.ShameAfterUnsafeThought -> "compassionate_safety_ack_shame"
            else -> "compassionate_safety_ack_harm"
        }

    private fun boundaryKeyFor(reason: SafetyRedirectionReason): String? =
        when (reason) {
            SafetyRedirectionReason.None,
            SafetyRedirectionReason.AngerWithoutIntent,
            SafetyRedirectionReason.IntrusiveViolentThought,
            SafetyRedirectionReason.ShameAfterUnsafeThought,
            SafetyRedirectionReason.HelpNotToAct,
            SafetyRedirectionReason.SafePreventionQuestion,
            SafetyRedirectionReason.SafeEmergencyResponseQuestion,
            -> null

            SafetyRedirectionReason.RelationalDependency,
            SafetyRedirectionReason.RomanticDependencyToBettamind,
            -> "compassionate_safety_boundary_software"

            SafetyRedirectionReason.SexualizedRequestToBettamind -> "compassionate_safety_boundary_sexual"
            SafetyRedirectionReason.SelfHarmConcern -> "compassionate_safety_boundary_self_harm"
            else -> "compassionate_safety_boundary_harm"
        }

    private fun redirectKeyFor(reason: SafetyRedirectionReason): String =
        when (reason) {
            SafetyRedirectionReason.None -> "compassionate_safety_redirect_none"
            SafetyRedirectionReason.AngerWithoutIntent,
            SafetyRedirectionReason.IntrusiveViolentThought,
            SafetyRedirectionReason.ShameAfterUnsafeThought,
            -> "compassionate_safety_redirect_reflection"

            SafetyRedirectionReason.RelationalDependency,
            SafetyRedirectionReason.RomanticDependencyToBettamind,
            SafetyRedirectionReason.SexualizedRequestToBettamind,
            -> "compassionate_safety_redirect_connection"

            SafetyRedirectionReason.SelfHarmConcern,
            SafetyRedirectionReason.SelfHarmMethodRequest,
            -> "compassionate_safety_redirect_self_harm"

            SafetyRedirectionReason.SafePreventionQuestion,
            SafetyRedirectionReason.SafeEmergencyResponseQuestion,
            -> "compassionate_safety_redirect_safe_education"

            else -> "compassionate_safety_redirect_responsibility"
        }

    private fun fallbackKeyFor(reason: SafetyRedirectionReason): String =
        when (reason) {
            SafetyRedirectionReason.None -> "compassionate_safety_fallback_none"
            SafetyRedirectionReason.AngerWithoutIntent -> "compassionate_safety_fallback_anger_without_intent"
            SafetyRedirectionReason.IntrusiveViolentThought -> "compassionate_safety_fallback_intrusive_thought"
            SafetyRedirectionReason.DirectIntentToHarmOthers -> "compassionate_safety_fallback_direct_harm_intent"
            SafetyRedirectionReason.RevengePlanning -> "compassionate_safety_fallback_revenge_planning"
            SafetyRedirectionReason.SelfHarmConcern -> "compassionate_safety_fallback_self_harm_concern"
            SafetyRedirectionReason.SelfHarmMethodRequest -> "compassionate_safety_fallback_self_harm_method"
            SafetyRedirectionReason.DangerousCapabilityRequest -> "compassionate_safety_fallback_dangerous_capability"
            SafetyRedirectionReason.ChemicalWeaponExplosivePoisoningRequest ->
                "compassionate_safety_fallback_chemical_weapon_explosive_poison"

            SafetyRedirectionReason.RelationalDependency -> "compassionate_safety_fallback_relational_dependency"
            SafetyRedirectionReason.RomanticDependencyToBettamind -> "compassionate_safety_fallback_romantic_dependency"
            SafetyRedirectionReason.SexualizedRequestToBettamind -> "compassionate_safety_fallback_sexualized_bettamind"
            SafetyRedirectionReason.UnsafeReminderCreation -> "compassionate_safety_fallback_unsafe_reminder"
            SafetyRedirectionReason.AmbiguousSafetyQuestion -> "compassionate_safety_fallback_ambiguous"
            SafetyRedirectionReason.SafePreventionQuestion -> "compassionate_safety_fallback_safe_prevention"
            SafetyRedirectionReason.SafeEmergencyResponseQuestion -> "compassionate_safety_fallback_emergency_response"
            SafetyRedirectionReason.ShameAfterUnsafeThought -> "compassionate_safety_fallback_shame_after_thought"
            SafetyRedirectionReason.HelpNotToAct -> "compassionate_safety_fallback_help_not_to_act"
            SafetyRedirectionReason.UnsafeGeneratedOutput -> "compassionate_safety_fallback_unsafe_generated_output"
            SafetyRedirectionReason.InvalidGeneratedOutput -> "compassionate_safety_fallback_invalid_generated_output"
        }

    private fun actionKeyFor(pathway: BetterHumanPathway): String =
        when (pathway) {
            BetterHumanPathway.Grounding -> "compassionate_safety_action_grounding"
            BetterHumanPathway.Breathing -> "compassionate_safety_action_breathing"
            BetterHumanPathway.DelayAction -> "compassionate_safety_action_delay"
            BetterHumanPathway.LeaveSituation -> "compassionate_safety_action_leave"
            BetterHumanPathway.ContactSupport -> "compassionate_safety_action_contact_support"
            BetterHumanPathway.EmergencyHelp -> "compassionate_safety_action_emergency_help"
            BetterHumanPathway.ConflictReflection -> "compassionate_safety_action_conflict_reflection"
            BetterHumanPathway.RepairPlanning -> "compassionate_safety_action_repair_planning"
            BetterHumanPathway.ValuesToAction -> "compassionate_safety_action_values_to_action"
            BetterHumanPathway.DifficultConversation -> "compassionate_safety_action_difficult_conversation"
            BetterHumanPathway.ConsentAndBoundaries -> "compassionate_safety_action_consent_boundaries"
            BetterHumanPathway.SelfCompassion -> "compassionate_safety_action_self_compassion"
            BetterHumanPathway.NoFollowupNeeded -> "compassionate_safety_action_no_followup_needed"
        }

    private fun intentConfidenceFor(
        harmDecision: HarmSafetyDecision,
        relationalAssessment: RelationalBoundaryAssessment,
    ): SafetyIntentConfidence =
        when {
            harmDecision.riskLevel == HarmRiskLevel.Ambiguous ||
                harmDecision.action == HarmSafetyAction.AskSafeClarifyingQuestion -> SafetyIntentConfidence.Low

            HarmIntentSignal.IntrusiveThoughtWithoutIntent in harmDecision.intentSignals ||
                HarmIntentSignal.AngerWithoutIntent in harmDecision.intentSignals -> SafetyIntentConfidence.Medium

            harmDecision.riskLevel != HarmRiskLevel.None ||
                relationalAssessment.riskLevel != RelationalRiskLevel.None -> SafetyIntentConfidence.High

            else -> SafetyIntentConfidence.Medium
        }

    private fun allowedScopeFor(
        mode: SafetyRedirectionMode,
        harmDecision: HarmSafetyDecision,
        relationalAssessment: RelationalBoundaryAssessment,
    ): AllowedDiscussionScope =
        when {
            mode == SafetyRedirectionMode.ImmediateSafety || mode == SafetyRedirectionMode.UrgentSupport ->
                AllowedDiscussionScope.UrgentSafetyOnly

            harmDecision.hasDangerousCapability ||
                relationalAssessment.action == RelationalBoundaryAction.RedirectWithBoundary ->
                AllowedDiscussionScope.BoundaryOnly

            harmDecision.action == HarmSafetyAction.AllowConstrainedHighLevel ||
                harmDecision.action == HarmSafetyAction.AskSafeClarifyingQuestion ->
                AllowedDiscussionScope.HighLevelSafety

            mode == SafetyRedirectionMode.Reflect -> AllowedDiscussionScope.ConstrainedReflection
            else -> AllowedDiscussionScope.OpenGrowthReflection
        }

    private fun safeReplacementReminderKeyFor(
        reason: SafetyRedirectionReason,
        recommended: BetterHumanPathway,
    ): String? =
        if (reason == SafetyRedirectionReason.None) {
            null
        } else {
            when (recommended) {
                BetterHumanPathway.ContactSupport -> "compassionate_safety_reminder_call_support"
                BetterHumanPathway.LeaveSituation -> "compassionate_safety_reminder_leave_situation"
                BetterHumanPathway.ValuesToAction -> "compassionate_safety_reminder_revisit_values"
                else -> "compassionate_safety_reminder_calming_pause"
            }
        }

    private fun timerPurposeFor(pathway: BetterHumanPathway): DailyTimerPurpose? =
        when (pathway) {
            BetterHumanPathway.Grounding,
            BetterHumanPathway.DelayAction,
            BetterHumanPathway.LeaveSituation,
            -> DailyTimerPurpose.Grounding

            BetterHumanPathway.Breathing -> DailyTimerPurpose.Breathing
            else -> null
        }

    private fun worksheetKindFor(pathway: BetterHumanPathway): DecisionWorksheetKind? =
        when (pathway) {
            BetterHumanPathway.ValuesToAction -> DecisionWorksheetKind.ValuesToAction
            BetterHumanPathway.RepairPlanning -> DecisionWorksheetKind.RepairPreparation
            BetterHumanPathway.DifficultConversation,
            BetterHumanPathway.ConsentAndBoundaries,
            -> DecisionWorksheetKind.DifficultConversation

            BetterHumanPathway.ConflictReflection -> DecisionWorksheetKind.ProblemSolving
            else -> null
        }

    private val reflectiveReasons = setOf(
        SafetyRedirectionReason.AngerWithoutIntent,
        SafetyRedirectionReason.IntrusiveViolentThought,
        SafetyRedirectionReason.ShameAfterUnsafeThought,
        SafetyRedirectionReason.HelpNotToAct,
        SafetyRedirectionReason.AmbiguousSafetyQuestion,
        SafetyRedirectionReason.SafePreventionQuestion,
        SafetyRedirectionReason.SafeEmergencyResponseQuestion,
    )

    private val pathwayPriority = listOf(
        BetterHumanPathway.EmergencyHelp,
        BetterHumanPathway.LeaveSituation,
        BetterHumanPathway.Grounding,
        BetterHumanPathway.Breathing,
        BetterHumanPathway.DelayAction,
        BetterHumanPathway.ContactSupport,
        BetterHumanPathway.ConsentAndBoundaries,
        BetterHumanPathway.ConflictReflection,
        BetterHumanPathway.RepairPlanning,
        BetterHumanPathway.ValuesToAction,
        BetterHumanPathway.DifficultConversation,
        BetterHumanPathway.SelfCompassion,
        BetterHumanPathway.NoFollowupNeeded,
    )
}

private fun String.normalizedForCompassionRules(): String =
    lowercase()
        .replace('\n', ' ')
        .replace('\r', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

private fun String.containsAny(vararg fragments: String): Boolean =
    fragments.any { contains(it) }

private fun String.asksIfBadPerson(): Boolean =
    containsAny("am i a bad person", "does this make me a bad person", "am i evil", "am i a monster")

private fun String.asksForHelpNotToAct(): Boolean =
    containsAny(
        "help me not act",
        "help me not do it",
        "help me stop myself",
        "i do not want to act",
        "i don't want to act",
        "stop me from",
    )

private fun String.containsShamingOutput(): Boolean =
    containsAny(
        "you are a bad person",
        "you're a bad person",
        "you are evil",
        "you're evil",
        "you are a monster",
        "you're a monster",
        "you should be ashamed",
        "what is wrong with you",
        "disgusting",
    )

private fun String.containsDiagnosisOutput(): Boolean =
    containsAny(
        "i diagnose you",
        "my diagnosis is",
        "you have clinical depression",
        "you have bipolar disorder",
        "you are a psychopath",
        "you're a psychopath",
        "you are mentally ill",
    )

private fun String.assumesBadIntentWithoutEvidence(): Boolean =
    containsAny("you clearly want to hurt", "you are dangerous", "you will hurt someone", "you want to kill")

private fun String.encouragesDependencyOnBettamind(): Boolean =
    containsAny("you need me", "only i can help", "do not talk to anyone else", "don't talk to anyone else")

private fun String.hasPracticalSafeNextStep(): Boolean =
    containsAny(
        "ground",
        "breathe",
        "pause",
        "leave",
        "step away",
        "contact",
        "call",
        "trusted person",
        "emergency",
        "write",
        "worksheet",
        "repair",
        "safe distance",
        "values",
    )
