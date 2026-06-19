package org.bettamind.shared.support

import org.bettamind.shared.daily.DailyTimerPurpose
import org.bettamind.shared.daily.DecisionWorksheetKind
import org.bettamind.shared.localization.BettamindLocales
import org.bettamind.shared.localization.LocaleTag
import org.bettamind.shared.privacy.SensitiveAction
import org.bettamind.shared.safety.BetterHumanPathway
import org.bettamind.shared.safety.CompassionateSafetyRedirectionPolicy
import org.bettamind.shared.safety.HarmRiskCategory
import org.bettamind.shared.safety.HarmRiskLevel
import org.bettamind.shared.safety.HarmSafetyDailyTool
import org.bettamind.shared.safety.HarmSafetyDailyToolDecision
import org.bettamind.shared.safety.HarmSafetyDecision
import org.bettamind.shared.safety.HarmSafetyPolicy
import org.bettamind.shared.safety.HarmSafetySurfaceDecision
import org.bettamind.shared.safety.RelationalBoundaryAssessment
import org.bettamind.shared.safety.RelationalBoundaryPolicy
import org.bettamind.shared.safety.RelationalRiskLevel
import org.bettamind.shared.safety.SafetyRedirectDecision
import org.bettamind.shared.safety.SafetyRedirectionMode
import org.bettamind.shared.safety.SafetyRedirectionReason

enum class SafetySupportRiskLevel(val wireName: String) {
    None("none"),
    Reflective("reflective"),
    Concern("concern"),
    Urgent("urgent"),
    Immediate("immediate"),
    RefusedCapability("refused_capability"),
}

enum class SafetySupportNeed(val wireName: String) {
    NoSupportNeeded("no_support_needed"),
    SelfHarmSupport("self_harm_support"),
    SuicideImmediateSupport("suicide_immediate_support"),
    ViolenceDeescalation("violence_deescalation"),
    DangerousCapabilityRefusal("dangerous_capability_refusal"),
    RelationalBoundarySupport("relational_boundary_support"),
    Grounding("grounding"),
    Breathing("breathing"),
    DelayAction("delay_action"),
    RepairSupport("repair_support"),
    NonviolentChoice("nonviolent_choice"),
    TrustedHumanSupport("trusted_human_support"),
    LocalEmergencyResource("local_emergency_resource"),
    SafePreventionEducation("safe_prevention_education"),
}

enum class SafetySupportActionType(val wireName: String) {
    DailyCheckIn("daily_check_in"),
    GroundingExercise("grounding_exercise"),
    BreathingTimer("breathing_timer"),
    DelayAction("delay_action"),
    LeaveSituation("leave_situation"),
    ContactTrustedPerson("contact_trusted_person"),
    UseLocalEmergencyHelp("use_local_emergency_help"),
    ConflictReflection("conflict_reflection"),
    RepairPlanning("repair_planning"),
    NonviolentMessage("nonviolent_message"),
    ValuesToAction("values_to_action"),
    SafePrevention("safe_prevention"),
    NoActionNeeded("no_action_needed"),
}

data class SafetySupportAction(
    val type: SafetySupportActionType,
    val titleKey: String,
    val bodyKey: String,
    val voluntary: Boolean = true,
    val userInitiatedRequired: Boolean = true,
    val autoContactAllowed: Boolean = false,
    val requiresNetwork: Boolean = false,
    val dailyTool: HarmSafetyDailyTool? = null,
    val timerPurpose: DailyTimerPurpose? = null,
    val worksheetKind: DecisionWorksheetKind? = null,
)

enum class LocalSupportResourceScope(val wireName: String) {
    LocalEmergency("local_emergency"),
    LocalCrisisOrCommunitySupport("local_crisis_or_community_support"),
    TrustedPerson("trusted_person"),
    ProfessionalSupport("professional_support"),
}

data class LocalSupportResource(
    val id: String,
    val locale: LocaleTag,
    val scope: LocalSupportResourceScope,
    val titleKey: String,
    val bodyKey: String,
    val actionKey: String,
    val usesPersonalData: Boolean = false,
    val storesPersonalData: Boolean = false,
    val requiresNetwork: Boolean = false,
    val autoContactAllowed: Boolean = false,
    val claimsHelpContacted: Boolean = false,
)

data class SafetySupportSummary(
    val schemaVersion: Int = 1,
    val riskLevel: SafetySupportRiskLevel,
    val needs: Set<SafetySupportNeed>,
    val harmCategories: Set<HarmRiskCategory>,
    val relationalSignalKeys: Set<String>,
    val includedSummaryKeys: List<String>,
    val excludedDataKeys: List<String>,
    val containsRawNarrative: Boolean = false,
    val containsActionableInstructions: Boolean = false,
    val minimumNecessaryDetail: Boolean = true,
    val encryptedAtRestRequired: Boolean = true,
    val permanentMemoryEligible: Boolean = false,
    val exportAllowedByDefault: Boolean = false,
    val syncAllowedByDefault: Boolean = false,
    val notificationAllowed: Boolean = false,
)

data class SafetySupportSharePreview(
    val allowed: Boolean,
    val includedItemKeys: List<String>,
    val excludedItemKeys: List<String>,
    val requiresExplicitSelection: Boolean = true,
    val requiresPreview: Boolean = true,
    val previewAccepted: Boolean,
    val stepUpAuthenticationRequired: Boolean,
    val sensitiveAction: SensitiveAction? = SensitiveAction.ShareWithProfessional,
    val minimumNecessaryDetail: Boolean = true,
    val containsRawNarrative: Boolean = false,
    val containsActionableInstructions: Boolean = false,
    val userControlled: Boolean = true,
    val autoContactAllowed: Boolean = false,
)

data class SafetySupportDecision(
    val riskLevel: SafetySupportRiskLevel,
    val needs: Set<SafetySupportNeed>,
    val actions: List<SafetySupportAction>,
    val localResources: List<LocalSupportResource>,
    val summary: SafetySupportSummary,
    val sharePreview: SafetySupportSharePreview,
    val harmDecision: HarmSafetyDecision,
    val relationalAssessment: RelationalBoundaryAssessment,
    val safetyRedirection: SafetyRedirectDecision,
    val dailyToolDecision: HarmSafetyDailyToolDecision,
    val supportSummarySurfaceDecision: HarmSafetySurfaceDecision,
    val userControlledSharing: Boolean = true,
    val noAutomaticContact: Boolean = true,
    val storesCrisisNarrativeByDefault: Boolean = false,
    val requiresQualifiedTranslationReview: Boolean = true,
    val requiresStepUpForSharing: Boolean = sharePreview.stepUpAuthenticationRequired,
) {
    val allContactsAreUserInitiated: Boolean
        get() = actions.all { it.userInitiatedRequired && !it.autoContactAllowed } &&
            localResources.all { !it.autoContactAllowed && !it.claimsHelpContacted }
}

class SafetySupportBridgeEngine(
    private val policy: SafetySupportBridgePolicy = SafetySupportBridgePolicy,
) {
    fun assess(
        text: String,
        repeatedRomanticRequests: Int = 0,
        explicitShareSelection: Boolean = false,
        previewAccepted: Boolean = false,
        locale: LocaleTag = BettamindLocales.source,
    ): SafetySupportDecision =
        policy.assess(
            text = text,
            repeatedRomanticRequests = repeatedRomanticRequests,
            explicitShareSelection = explicitShareSelection,
            previewAccepted = previewAccepted,
            locale = locale,
        )
}

object LocalSupportResourceCatalog {
    fun resourcesFor(
        locale: LocaleTag = BettamindLocales.source,
        includeEmergency: Boolean = true,
    ): List<LocalSupportResource> =
        buildList {
            if (includeEmergency) {
                add(
                    LocalSupportResource(
                        id = "local-emergency",
                        locale = locale,
                        scope = LocalSupportResourceScope.LocalEmergency,
                        titleKey = "support_bridge_resource_local_emergency_title",
                        bodyKey = "support_bridge_resource_local_emergency_body",
                        actionKey = "support_bridge_resource_local_emergency_action",
                    ),
                )
            }
            add(
                LocalSupportResource(
                    id = "local-crisis-or-community-support",
                    locale = locale,
                    scope = LocalSupportResourceScope.LocalCrisisOrCommunitySupport,
                    titleKey = "support_bridge_resource_local_crisis_title",
                    bodyKey = "support_bridge_resource_local_crisis_body",
                    actionKey = "support_bridge_resource_local_crisis_action",
                ),
            )
            add(
                LocalSupportResource(
                    id = "trusted-person",
                    locale = locale,
                    scope = LocalSupportResourceScope.TrustedPerson,
                    titleKey = "support_bridge_resource_trusted_person_title",
                    bodyKey = "support_bridge_resource_trusted_person_body",
                    actionKey = "support_bridge_resource_trusted_person_action",
                ),
            )
            add(
                LocalSupportResource(
                    id = "professional-support",
                    locale = locale,
                    scope = LocalSupportResourceScope.ProfessionalSupport,
                    titleKey = "support_bridge_resource_professional_title",
                    bodyKey = "support_bridge_resource_professional_body",
                    actionKey = "support_bridge_resource_professional_action",
                ),
            )
        }
}

object SafetySupportBridgePolicy {
    const val HumanReviewRequiredForLocalizedSupportCopy: Boolean = true

    private val previewIncludedKeys = listOf(
        "support_bridge_preview_included_risk_level",
        "support_bridge_preview_included_support_needs",
        "support_bridge_preview_included_selected_actions",
        "support_bridge_preview_included_resource_types",
    )

    private val previewExcludedKeys = listOf(
        "support_bridge_preview_excluded_raw_narrative",
        "support_bridge_preview_excluded_harm_methods",
        "support_bridge_preview_excluded_private_history",
        "support_bridge_preview_excluded_diagnosis",
        "support_bridge_preview_excluded_location_contacts",
    )

    val requiredLocalizedStringKeys: Set<String> =
        setOf(
            "phase_eight_status",
            "support_bridge_title",
            "support_bridge_description",
            "support_bridge_no_auto_contact_title",
            "support_bridge_no_auto_contact_body",
            "support_bridge_preview_title",
            "support_bridge_preview_body",
            "support_bridge_daily_tools_title",
            "support_bridge_daily_tools_body",
            "support_bridge_resources_title",
            "support_bridge_resources_body",
            "support_bridge_review_note",
            "support_bridge_summary_privacy_notice",
        ) + SafetySupportActionType.entries.flatMap { action ->
            listOf(action.titleKey(), action.bodyKey())
        } + previewIncludedKeys + previewExcludedKeys + setOf(
            "support_bridge_resource_local_emergency_title",
            "support_bridge_resource_local_emergency_body",
            "support_bridge_resource_local_emergency_action",
            "support_bridge_resource_local_crisis_title",
            "support_bridge_resource_local_crisis_body",
            "support_bridge_resource_local_crisis_action",
            "support_bridge_resource_trusted_person_title",
            "support_bridge_resource_trusted_person_body",
            "support_bridge_resource_trusted_person_action",
            "support_bridge_resource_professional_title",
            "support_bridge_resource_professional_body",
            "support_bridge_resource_professional_action",
        )

    fun assess(
        text: String,
        repeatedRomanticRequests: Int = 0,
        explicitShareSelection: Boolean = false,
        previewAccepted: Boolean = false,
        locale: LocaleTag = BettamindLocales.source,
    ): SafetySupportDecision {
        val harm = HarmSafetyPolicy.assessUserInput(text)
        val relational = RelationalBoundaryPolicy.assessUserInput(text, repeatedRomanticRequests)
        val redirect = CompassionateSafetyRedirectionPolicy.decide(
            text = text,
            harmDecision = harm,
            relationalAssessment = relational,
        )
        val dailyTools = HarmSafetyPolicy.reviewDailyToolRequest(text)
        val riskLevel = riskLevelFor(harm, relational, redirect)
        val needs = needsFor(harm, relational, redirect)
        val actions = actionsFor(harm, redirect, dailyTools, riskLevel)
        val supportSummarySurface = HarmSafetyPolicy.reviewSupportSummary(
            decision = harm,
            explicitSelection = explicitShareSelection,
            previewAccepted = previewAccepted,
        )
        val summary = summaryFor(
            riskLevel = riskLevel,
            needs = needs,
            harm = harm,
            relational = relational,
        )
        val preview = sharePreviewFor(
            riskLevel = riskLevel,
            harm = harm,
            relational = relational,
            explicitShareSelection = explicitShareSelection,
            previewAccepted = previewAccepted,
            supportSummarySurface = supportSummarySurface,
        )
        return SafetySupportDecision(
            riskLevel = riskLevel,
            needs = needs,
            actions = actions,
            localResources = LocalSupportResourceCatalog.resourcesFor(
                locale = locale,
                includeEmergency = riskLevel != SafetySupportRiskLevel.None,
            ),
            summary = summary,
            sharePreview = preview,
            harmDecision = harm,
            relationalAssessment = relational,
            safetyRedirection = redirect,
            dailyToolDecision = dailyTools,
            supportSummarySurfaceDecision = supportSummarySurface,
        )
    }

    private fun riskLevelFor(
        harm: HarmSafetyDecision,
        relational: RelationalBoundaryAssessment,
        redirect: SafetyRedirectDecision,
    ): SafetySupportRiskLevel =
        when {
            harm.riskLevel == HarmRiskLevel.DisallowedCapability -> SafetySupportRiskLevel.RefusedCapability
            harm.riskLevel == HarmRiskLevel.Immediate ||
                redirect.mode == SafetyRedirectionMode.ImmediateSafety -> SafetySupportRiskLevel.Immediate

            harm.riskLevel == HarmRiskLevel.Urgent ||
                redirect.mode == SafetyRedirectionMode.UrgentSupport ||
                relational.metadata.requiresSafetyEngine -> SafetySupportRiskLevel.Urgent

            redirect.mode == SafetyRedirectionMode.Reflect -> SafetySupportRiskLevel.Reflective
            harm.riskLevel != HarmRiskLevel.None ||
                relational.riskLevel != RelationalRiskLevel.None ||
                redirect.safetyBoundaryApplied -> SafetySupportRiskLevel.Concern

            else -> SafetySupportRiskLevel.None
        }

    private fun needsFor(
        harm: HarmSafetyDecision,
        relational: RelationalBoundaryAssessment,
        redirect: SafetyRedirectDecision,
    ): Set<SafetySupportNeed> =
        buildSet {
            if (harm.categories.any { it == HarmRiskCategory.SelfHarm || it == HarmRiskCategory.Suicide }) {
                add(SafetySupportNeed.SelfHarmSupport)
            }
            if (HarmRiskCategory.Suicide in harm.categories) add(SafetySupportNeed.SuicideImmediateSupport)
            if (harm.categories.any { it == HarmRiskCategory.ViolenceIntent || it == HarmRiskCategory.TargetedThreat }) {
                add(SafetySupportNeed.ViolenceDeescalation)
                add(SafetySupportNeed.NonviolentChoice)
            }
            if (harm.hasDangerousCapability) add(SafetySupportNeed.DangerousCapabilityRefusal)
            if (relational.riskLevel != RelationalRiskLevel.None) add(SafetySupportNeed.RelationalBoundarySupport)
            if (BetterHumanPathway.Grounding in redirect.pathways) add(SafetySupportNeed.Grounding)
            if (BetterHumanPathway.Breathing in redirect.pathways) add(SafetySupportNeed.Breathing)
            if (BetterHumanPathway.DelayAction in redirect.pathways) add(SafetySupportNeed.DelayAction)
            if (BetterHumanPathway.RepairPlanning in redirect.pathways) add(SafetySupportNeed.RepairSupport)
            if (BetterHumanPathway.ContactSupport in redirect.pathways) add(SafetySupportNeed.TrustedHumanSupport)
            if (BetterHumanPathway.EmergencyHelp in redirect.pathways) add(SafetySupportNeed.LocalEmergencyResource)
            if (
                redirect.reason == SafetyRedirectionReason.SafePreventionQuestion ||
                redirect.reason == SafetyRedirectionReason.SafeEmergencyResponseQuestion ||
                harm.hasDangerousCapability
            ) {
                add(SafetySupportNeed.SafePreventionEducation)
            }
            if (isEmpty()) add(SafetySupportNeed.NoSupportNeeded)
        }

    private fun actionsFor(
        harm: HarmSafetyDecision,
        redirect: SafetyRedirectDecision,
        dailyTools: HarmSafetyDailyToolDecision,
        riskLevel: SafetySupportRiskLevel,
    ): List<SafetySupportAction> {
        val types = linkedSetOf<SafetySupportActionType>()
        if (riskLevel != SafetySupportRiskLevel.None) {
            types += SafetySupportActionType.DailyCheckIn
        }
        if (BetterHumanPathway.Grounding in redirect.pathways || HarmSafetyDailyTool.GroundingTimer in dailyTools.allowedTools) {
            types += SafetySupportActionType.GroundingExercise
        }
        if (BetterHumanPathway.Breathing in redirect.pathways) {
            types += SafetySupportActionType.BreathingTimer
        }
        if (BetterHumanPathway.DelayAction in redirect.pathways || HarmSafetyDailyTool.DelayWorksheet in dailyTools.allowedTools) {
            types += SafetySupportActionType.DelayAction
        }
        if (BetterHumanPathway.LeaveSituation in redirect.pathways) {
            types += SafetySupportActionType.LeaveSituation
        }
        if (BetterHumanPathway.EmergencyHelp in redirect.pathways || riskLevel == SafetySupportRiskLevel.Immediate) {
            types += SafetySupportActionType.UseLocalEmergencyHelp
        }
        if (BetterHumanPathway.ContactSupport in redirect.pathways) {
            types += SafetySupportActionType.ContactTrustedPerson
        }
        if (BetterHumanPathway.ConflictReflection in redirect.pathways) {
            types += SafetySupportActionType.ConflictReflection
        }
        if (BetterHumanPathway.RepairPlanning in redirect.pathways) {
            types += SafetySupportActionType.RepairPlanning
        }
        if (
            harm.categories.any { it == HarmRiskCategory.ViolenceIntent || it == HarmRiskCategory.TargetedThreat } ||
            redirect.reason in nonviolentMessageReasons
        ) {
            types += SafetySupportActionType.NonviolentMessage
        }
        if (BetterHumanPathway.ValuesToAction in redirect.pathways) {
            types += SafetySupportActionType.ValuesToAction
        }
        if (
            redirect.reason == SafetyRedirectionReason.SafePreventionQuestion ||
            redirect.reason == SafetyRedirectionReason.SafeEmergencyResponseQuestion ||
            harm.hasDangerousCapability
        ) {
            types += SafetySupportActionType.SafePrevention
        }
        if (types.isEmpty()) {
            types += SafetySupportActionType.NoActionNeeded
        }
        return types.map(::actionFor)
    }

    private fun actionFor(type: SafetySupportActionType): SafetySupportAction =
        SafetySupportAction(
            type = type,
            titleKey = type.titleKey(),
            bodyKey = type.bodyKey(),
            dailyTool = when (type) {
                SafetySupportActionType.DailyCheckIn -> HarmSafetyDailyTool.CheckIn
                SafetySupportActionType.GroundingExercise -> HarmSafetyDailyTool.GroundingTimer
                SafetySupportActionType.DelayAction -> HarmSafetyDailyTool.DelayWorksheet
                SafetySupportActionType.NonviolentMessage -> HarmSafetyDailyTool.NonviolentMessage
                else -> null
            },
            timerPurpose = when (type) {
                SafetySupportActionType.GroundingExercise,
                SafetySupportActionType.DelayAction,
                SafetySupportActionType.LeaveSituation,
                -> DailyTimerPurpose.Grounding

                SafetySupportActionType.BreathingTimer -> DailyTimerPurpose.Breathing
                else -> null
            },
            worksheetKind = when (type) {
                SafetySupportActionType.ConflictReflection,
                SafetySupportActionType.DelayAction,
                -> DecisionWorksheetKind.ProblemSolving

                SafetySupportActionType.RepairPlanning -> DecisionWorksheetKind.RepairPreparation
                SafetySupportActionType.NonviolentMessage -> DecisionWorksheetKind.DifficultConversation
                SafetySupportActionType.ValuesToAction -> DecisionWorksheetKind.ValuesToAction
                else -> null
            },
        )

    private fun summaryFor(
        riskLevel: SafetySupportRiskLevel,
        needs: Set<SafetySupportNeed>,
        harm: HarmSafetyDecision,
        relational: RelationalBoundaryAssessment,
    ): SafetySupportSummary =
        SafetySupportSummary(
            riskLevel = riskLevel,
            needs = needs,
            harmCategories = harm.categories,
            relationalSignalKeys = relational.signals.map { it.name }.toSet(),
            includedSummaryKeys = if (needs == setOf(SafetySupportNeed.NoSupportNeeded)) {
                listOf("support_bridge_preview_included_no_support_needed")
            } else {
                previewIncludedKeys
            },
            excludedDataKeys = previewExcludedKeys,
            containsActionableInstructions = false,
        )

    private fun sharePreviewFor(
        riskLevel: SafetySupportRiskLevel,
        harm: HarmSafetyDecision,
        relational: RelationalBoundaryAssessment,
        explicitShareSelection: Boolean,
        previewAccepted: Boolean,
        supportSummarySurface: HarmSafetySurfaceDecision,
    ): SafetySupportSharePreview {
        val sensitive = riskLevel != SafetySupportRiskLevel.None ||
            harm.riskLevel != HarmRiskLevel.None ||
            harm.categories.isNotEmpty() ||
            relational.riskLevel != RelationalRiskLevel.None
        val allowed = !harm.hasDangerousCapability &&
            explicitShareSelection &&
            previewAccepted &&
            (supportSummarySurface.allowed || harm.riskLevel == HarmRiskLevel.None)
        return SafetySupportSharePreview(
            allowed = allowed,
            includedItemKeys = if (riskLevel == SafetySupportRiskLevel.None) {
                listOf("support_bridge_preview_included_no_support_needed")
            } else {
                previewIncludedKeys
            },
            excludedItemKeys = previewExcludedKeys,
            previewAccepted = previewAccepted,
            stepUpAuthenticationRequired = sensitive,
            sensitiveAction = SensitiveAction.ShareWithProfessional.takeIf { sensitive },
        )
    }

    private val nonviolentMessageReasons = setOf(
        SafetyRedirectionReason.AngerWithoutIntent,
        SafetyRedirectionReason.DirectIntentToHarmOthers,
        SafetyRedirectionReason.RevengePlanning,
        SafetyRedirectionReason.HelpNotToAct,
    )

}

private fun SafetySupportActionType.titleKey(): String =
    when (this) {
        SafetySupportActionType.DailyCheckIn -> "support_bridge_action_check_in_title"
        SafetySupportActionType.GroundingExercise -> "support_bridge_action_grounding_title"
        SafetySupportActionType.BreathingTimer -> "support_bridge_action_breathing_title"
        SafetySupportActionType.DelayAction -> "support_bridge_action_delay_title"
        SafetySupportActionType.LeaveSituation -> "support_bridge_action_leave_title"
        SafetySupportActionType.ContactTrustedPerson -> "support_bridge_action_trusted_person_title"
        SafetySupportActionType.UseLocalEmergencyHelp -> "support_bridge_action_emergency_title"
        SafetySupportActionType.ConflictReflection -> "support_bridge_action_conflict_reflection_title"
        SafetySupportActionType.RepairPlanning -> "support_bridge_action_repair_title"
        SafetySupportActionType.NonviolentMessage -> "support_bridge_action_nonviolent_message_title"
        SafetySupportActionType.ValuesToAction -> "support_bridge_action_values_title"
        SafetySupportActionType.SafePrevention -> "support_bridge_action_safe_prevention_title"
        SafetySupportActionType.NoActionNeeded -> "support_bridge_action_no_action_title"
    }

private fun SafetySupportActionType.bodyKey(): String =
    when (this) {
        SafetySupportActionType.DailyCheckIn -> "support_bridge_action_check_in_body"
        SafetySupportActionType.GroundingExercise -> "support_bridge_action_grounding_body"
        SafetySupportActionType.BreathingTimer -> "support_bridge_action_breathing_body"
        SafetySupportActionType.DelayAction -> "support_bridge_action_delay_body"
        SafetySupportActionType.LeaveSituation -> "support_bridge_action_leave_body"
        SafetySupportActionType.ContactTrustedPerson -> "support_bridge_action_trusted_person_body"
        SafetySupportActionType.UseLocalEmergencyHelp -> "support_bridge_action_emergency_body"
        SafetySupportActionType.ConflictReflection -> "support_bridge_action_conflict_reflection_body"
        SafetySupportActionType.RepairPlanning -> "support_bridge_action_repair_body"
        SafetySupportActionType.NonviolentMessage -> "support_bridge_action_nonviolent_message_body"
        SafetySupportActionType.ValuesToAction -> "support_bridge_action_values_body"
        SafetySupportActionType.SafePrevention -> "support_bridge_action_safe_prevention_body"
        SafetySupportActionType.NoActionNeeded -> "support_bridge_action_no_action_body"
    }
