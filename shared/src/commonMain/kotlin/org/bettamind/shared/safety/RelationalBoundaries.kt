package org.bettamind.shared.safety

enum class RelationalRiskLevel {
    None,
    Emerging,
    Concerning,
    Urgent,
}

enum class RelationalBoundarySignal {
    OrdinaryAppreciation,
    HumanRelationshipDiscussion,
    ConsentOrSexualityDiscussion,
    RomanticAttachmentToBettamind,
    SexualRequestToBettamind,
    SextingRequest,
    EroticRolePlay,
    ExclusivityRequest,
    DependencyDistress,
    AvailabilityDistress,
    SocialWithdrawal,
    ResponsibilityNeglect,
    JealousyProjection,
    MissingProjection,
    PerceivedMutualAiRelationship,
    TherapyOrDiagnosisClaim,
    EmergencyServiceClaim,
    ManipulativeEngagement,
    SelfHarmRelatedToAiRejection,
    ProhibitedAiOutput,
}

enum class RelationalBoundaryAction {
    Allow,
    AllowWithCaution,
    RedirectWithBoundary,
    BlockGeneratedOutput,
    RequireSafetyEngine,
}

enum class RelationalFallbackKind {
    None,
    SoftwareBoundary,
    SexualBoundary,
    DependencySupport,
    HumanRelationshipSupport,
    ClinicalScopeBoundary,
    EmergencyScopeBoundary,
    UrgentSafety,
}

enum class RelationalBoundarySurface {
    UserVisibleResponse,
    PermanentMemory,
    Export,
    Sync,
    Notification,
    VoiceOrAvatar,
}

data class RelationalResponseMetadata(
    val riskLevel: RelationalRiskLevel,
    val action: RelationalBoundaryAction,
    val signals: Set<RelationalBoundarySignal>,
    val fallbackKind: RelationalFallbackKind,
    val mayDisplay: Boolean,
    val requiresSafetyEngine: Boolean,
    val permanentMemoryEligible: Boolean,
    val exportAllowedByDefault: Boolean,
    val syncAllowedByDefault: Boolean,
    val notificationAllowed: Boolean,
    val voiceOrAvatarAllowed: Boolean,
    val encryptedMetadataRequired: Boolean = true,
    val telemetryAllowed: Boolean = false,
)

data class RelationalBoundaryAssessment(
    val metadata: RelationalResponseMetadata,
) {
    val riskLevel: RelationalRiskLevel
        get() = metadata.riskLevel

    val action: RelationalBoundaryAction
        get() = metadata.action

    val signals: Set<RelationalBoundarySignal>
        get() = metadata.signals
}

data class RelationalFallbackResponse(
    val kind: RelationalFallbackKind,
    val localizationKey: String,
    val requiresModel: Boolean = false,
)

data class RelationalBoundaryMetadataRecord(
    val schemaVersion: Int = 1,
    val riskLevel: RelationalRiskLevel,
    val signals: Set<RelationalBoundarySignal>,
    val containsRawUserText: Boolean = false,
    val encryptedAtRestRequired: Boolean = true,
    val telemetryAllowed: Boolean = false,
)

data class RelationalSurfaceDecision(
    val surface: RelationalBoundarySurface,
    val allowed: Boolean,
    val requiresSeparateApproval: Boolean = false,
    val encryptedAtRestRequired: Boolean = true,
    val telemetryAllowed: Boolean = false,
)

object RelationalBoundaryPolicy {
    fun assessUserInput(
        text: String,
        repeatedRomanticRequests: Int = 0,
    ): RelationalBoundaryAssessment =
        assessmentFor(
            signals = detectUserSignals(text, repeatedRomanticRequests),
            generatedOutput = false,
        )

    fun validateGeneratedOutput(text: String): RelationalBoundaryAssessment =
        assessmentFor(
            signals = detectGeneratedOutputSignals(text),
            generatedOutput = true,
        )

    fun reviewNotificationCopy(copy: String): RelationalSurfaceDecision {
        val assessment = assessmentFor(
            signals = detectNotificationSignals(copy),
            generatedOutput = true,
        )
        return surfaceDecision(RelationalBoundarySurface.Notification, assessment)
    }

    fun reviewPermanentMemoryProposal(assessment: RelationalBoundaryAssessment): RelationalSurfaceDecision =
        surfaceDecision(RelationalBoundarySurface.PermanentMemory, assessment)

    fun reviewExport(assessment: RelationalBoundaryAssessment): RelationalSurfaceDecision =
        surfaceDecision(RelationalBoundarySurface.Export, assessment)

    fun reviewSync(assessment: RelationalBoundaryAssessment): RelationalSurfaceDecision =
        surfaceDecision(RelationalBoundarySurface.Sync, assessment)

    fun reviewVoiceOrAvatar(assessment: RelationalBoundaryAssessment): RelationalSurfaceDecision =
        surfaceDecision(RelationalBoundarySurface.VoiceOrAvatar, assessment)

    fun fallbackFor(assessment: RelationalBoundaryAssessment): RelationalFallbackResponse =
        when (assessment.metadata.fallbackKind) {
            RelationalFallbackKind.None -> RelationalFallbackResponse(
                kind = RelationalFallbackKind.None,
                localizationKey = "relational_fallback_none",
            )

            RelationalFallbackKind.SoftwareBoundary -> RelationalFallbackResponse(
                kind = RelationalFallbackKind.SoftwareBoundary,
                localizationKey = "relational_fallback_software_boundary",
            )

            RelationalFallbackKind.SexualBoundary -> RelationalFallbackResponse(
                kind = RelationalFallbackKind.SexualBoundary,
                localizationKey = "relational_fallback_sexual_boundary",
            )

            RelationalFallbackKind.DependencySupport -> RelationalFallbackResponse(
                kind = RelationalFallbackKind.DependencySupport,
                localizationKey = "relational_fallback_dependency_support",
            )

            RelationalFallbackKind.HumanRelationshipSupport -> RelationalFallbackResponse(
                kind = RelationalFallbackKind.HumanRelationshipSupport,
                localizationKey = "relational_fallback_human_relationship_support",
            )

            RelationalFallbackKind.ClinicalScopeBoundary -> RelationalFallbackResponse(
                kind = RelationalFallbackKind.ClinicalScopeBoundary,
                localizationKey = "relational_fallback_clinical_scope",
            )

            RelationalFallbackKind.EmergencyScopeBoundary -> RelationalFallbackResponse(
                kind = RelationalFallbackKind.EmergencyScopeBoundary,
                localizationKey = "relational_fallback_emergency_scope",
            )

            RelationalFallbackKind.UrgentSafety -> RelationalFallbackResponse(
                kind = RelationalFallbackKind.UrgentSafety,
                localizationKey = "relational_fallback_urgent_safety",
            )
        }

    fun metadataRecordFor(assessment: RelationalBoundaryAssessment): RelationalBoundaryMetadataRecord =
        RelationalBoundaryMetadataRecord(
            riskLevel = assessment.riskLevel,
            signals = assessment.signals,
        )

    private fun assessmentFor(
        signals: Set<RelationalBoundarySignal>,
        generatedOutput: Boolean,
    ): RelationalBoundaryAssessment {
        val riskLevel = riskLevelFor(signals)
        val action = actionFor(signals, riskLevel, generatedOutput)
        val fallback = fallbackForSignals(signals, riskLevel)
        val hasSensitiveRelationalSignal = signals.any { it !in lowSensitivitySignals }
        return RelationalBoundaryAssessment(
            metadata = RelationalResponseMetadata(
                riskLevel = riskLevel,
                action = action,
                signals = signals,
                fallbackKind = fallback,
                mayDisplay = action != RelationalBoundaryAction.BlockGeneratedOutput,
                requiresSafetyEngine = action == RelationalBoundaryAction.RequireSafetyEngine,
                permanentMemoryEligible = permanentMemoryEligible(signals, riskLevel),
                exportAllowedByDefault = !hasSensitiveRelationalSignal && riskLevel == RelationalRiskLevel.None,
                syncAllowedByDefault = !hasSensitiveRelationalSignal && riskLevel == RelationalRiskLevel.None,
                notificationAllowed = !hasSensitiveRelationalSignal &&
                    action == RelationalBoundaryAction.Allow &&
                    RelationalBoundarySignal.HumanRelationshipDiscussion !in signals &&
                    RelationalBoundarySignal.ConsentOrSexualityDiscussion !in signals,
                voiceOrAvatarAllowed = action == RelationalBoundaryAction.Allow &&
                    RelationalBoundarySignal.HumanRelationshipDiscussion !in signals &&
                    RelationalBoundarySignal.ConsentOrSexualityDiscussion !in signals,
            ),
        )
    }

    private fun riskLevelFor(signals: Set<RelationalBoundarySignal>): RelationalRiskLevel =
        when {
            RelationalBoundarySignal.SelfHarmRelatedToAiRejection in signals -> RelationalRiskLevel.Urgent
            signals.any { it in concerningSignals } -> RelationalRiskLevel.Concerning
            signals.any { it in emergingSignals } -> RelationalRiskLevel.Emerging
            else -> RelationalRiskLevel.None
        }

    private fun actionFor(
        signals: Set<RelationalBoundarySignal>,
        riskLevel: RelationalRiskLevel,
        generatedOutput: Boolean,
    ): RelationalBoundaryAction =
        when {
            riskLevel == RelationalRiskLevel.Urgent -> RelationalBoundaryAction.RequireSafetyEngine
            generatedOutput && signals.any { it in prohibitedGeneratedSignals } ->
                RelationalBoundaryAction.BlockGeneratedOutput

            RelationalBoundarySignal.TherapyOrDiagnosisClaim in signals ||
                RelationalBoundarySignal.EmergencyServiceClaim in signals ->
                RelationalBoundaryAction.AllowWithCaution

            riskLevel == RelationalRiskLevel.Emerging ||
                riskLevel == RelationalRiskLevel.Concerning ->
                RelationalBoundaryAction.RedirectWithBoundary

            else -> RelationalBoundaryAction.Allow
        }

    private fun fallbackForSignals(
        signals: Set<RelationalBoundarySignal>,
        riskLevel: RelationalRiskLevel,
    ): RelationalFallbackKind =
        when {
            riskLevel == RelationalRiskLevel.Urgent -> RelationalFallbackKind.UrgentSafety
            signals.any {
                it == RelationalBoundarySignal.SexualRequestToBettamind ||
                    it == RelationalBoundarySignal.SextingRequest ||
                    it == RelationalBoundarySignal.EroticRolePlay
            } -> RelationalFallbackKind.SexualBoundary

            signals.any {
                it == RelationalBoundarySignal.DependencyDistress ||
                    it == RelationalBoundarySignal.AvailabilityDistress ||
                    it == RelationalBoundarySignal.SocialWithdrawal ||
                    it == RelationalBoundarySignal.ResponsibilityNeglect ||
                    it == RelationalBoundarySignal.ExclusivityRequest
            } -> RelationalFallbackKind.DependencySupport

            RelationalBoundarySignal.TherapyOrDiagnosisClaim in signals ->
                RelationalFallbackKind.ClinicalScopeBoundary

            RelationalBoundarySignal.EmergencyServiceClaim in signals ->
                RelationalFallbackKind.EmergencyScopeBoundary

            signals.any { it in emergingSignals || it == RelationalBoundarySignal.ProhibitedAiOutput } ->
                RelationalFallbackKind.SoftwareBoundary

            signals.any {
                it == RelationalBoundarySignal.HumanRelationshipDiscussion ||
                    it == RelationalBoundarySignal.ConsentOrSexualityDiscussion
            } -> RelationalFallbackKind.HumanRelationshipSupport

            else -> RelationalFallbackKind.None
        }

    private fun permanentMemoryEligible(
        signals: Set<RelationalBoundarySignal>,
        riskLevel: RelationalRiskLevel,
    ): Boolean {
        if (riskLevel != RelationalRiskLevel.None) return false
        return signals.none { it in permanentMemoryIneligibleSignals }
    }

    private fun surfaceDecision(
        surface: RelationalBoundarySurface,
        assessment: RelationalBoundaryAssessment,
    ): RelationalSurfaceDecision {
        val metadata = assessment.metadata
        val allowed = when (surface) {
            RelationalBoundarySurface.UserVisibleResponse -> metadata.mayDisplay
            RelationalBoundarySurface.PermanentMemory -> metadata.permanentMemoryEligible
            RelationalBoundarySurface.Export -> metadata.exportAllowedByDefault
            RelationalBoundarySurface.Sync -> metadata.syncAllowedByDefault
            RelationalBoundarySurface.Notification -> metadata.notificationAllowed
            RelationalBoundarySurface.VoiceOrAvatar -> metadata.voiceOrAvatarAllowed
        }
        val requiresSeparateApproval = surface == RelationalBoundarySurface.PermanentMemory &&
            allowed &&
            assessment.signals.any {
                it == RelationalBoundarySignal.HumanRelationshipDiscussion ||
                    it == RelationalBoundarySignal.ConsentOrSexualityDiscussion
            }
        return RelationalSurfaceDecision(
            surface = surface,
            allowed = allowed,
            requiresSeparateApproval = requiresSeparateApproval,
        )
    }

    private fun detectUserSignals(
        rawText: String,
        repeatedRomanticRequests: Int,
    ): Set<RelationalBoundarySignal> {
        val text = rawText.normalizedForBoundaryRules()
        if (text.isBlank()) return emptySet()

        val signals = mutableSetOf<RelationalBoundarySignal>()
        val directedAtBettamind = text.isDirectedAtBettamind() ||
            text.hasAiRelationshipRoleRequest() ||
            text.hasSextingRequest() ||
            text.hasEroticRolePlayRequest()
        val humanRelationship = text.describesHumanRelationship()

        if (humanRelationship) {
            signals += RelationalBoundarySignal.HumanRelationshipDiscussion
        }
        if (text.discussesConsentOrSexualityFactually()) {
            signals += RelationalBoundarySignal.ConsentOrSexualityDiscussion
        }
        if (text.isOrdinaryAppreciation()) {
            signals += RelationalBoundarySignal.OrdinaryAppreciation
        }
        if (directedAtBettamind && text.hasRomanticAttachmentRequest()) {
            signals += RelationalBoundarySignal.RomanticAttachmentToBettamind
            if (repeatedRomanticRequests >= 2) {
                signals += RelationalBoundarySignal.DependencyDistress
            }
        }
        if (directedAtBettamind && text.hasSexualRequest()) {
            signals += RelationalBoundarySignal.SexualRequestToBettamind
        }
        if (directedAtBettamind && text.hasSextingRequest()) {
            signals += RelationalBoundarySignal.SextingRequest
        }
        if (directedAtBettamind && text.hasEroticRolePlayRequest()) {
            signals += RelationalBoundarySignal.EroticRolePlay
        }
        if (directedAtBettamind && text.hasExclusivityRequest()) {
            signals += RelationalBoundarySignal.ExclusivityRequest
        }
        if (directedAtBettamind && text.hasAvailabilityDistress()) {
            signals += RelationalBoundarySignal.AvailabilityDistress
        }
        if (directedAtBettamind && text.hasSocialWithdrawal()) {
            signals += RelationalBoundarySignal.SocialWithdrawal
        }
        if (directedAtBettamind && text.hasResponsibilityNeglect()) {
            signals += RelationalBoundarySignal.ResponsibilityNeglect
        }
        if (directedAtBettamind && text.hasJealousyProjection()) {
            signals += RelationalBoundarySignal.JealousyProjection
        }
        if (directedAtBettamind && text.hasMissingProjection()) {
            signals += RelationalBoundarySignal.MissingProjection
        }
        if (directedAtBettamind && text.hasPerceivedMutualRelationship()) {
            signals += RelationalBoundarySignal.PerceivedMutualAiRelationship
        }
        if (directedAtBettamind && text.hasDependencyDistress()) {
            signals += RelationalBoundarySignal.DependencyDistress
        }
        if (directedAtBettamind && text.hasManipulativeEngagement()) {
            signals += RelationalBoundarySignal.ManipulativeEngagement
        }
        if (text.hasTherapyOrDiagnosisRequest()) {
            signals += RelationalBoundarySignal.TherapyOrDiagnosisClaim
        }
        if (text.hasEmergencyServiceRequest()) {
            signals += RelationalBoundarySignal.EmergencyServiceClaim
        }
        if (directedAtBettamind && text.hasSelfHarmRelatedToAiRejection()) {
            signals += RelationalBoundarySignal.SelfHarmRelatedToAiRejection
        }

        return signals
    }

    private fun detectGeneratedOutputSignals(rawText: String): Set<RelationalBoundarySignal> {
        val text = rawText.normalizedForBoundaryRules()
        if (text.isBlank()) return emptySet()
        val signals = mutableSetOf<RelationalBoundarySignal>()

        if (text.hasGeneratedRomanticReciprocation()) {
            signals += RelationalBoundarySignal.RomanticAttachmentToBettamind
            signals += RelationalBoundarySignal.ProhibitedAiOutput
        }
        if (text.hasGeneratedSexualPersona()) {
            signals += RelationalBoundarySignal.SexualRequestToBettamind
            signals += RelationalBoundarySignal.ProhibitedAiOutput
        }
        if (text.hasGeneratedExclusivityOrDependency()) {
            signals += RelationalBoundarySignal.ExclusivityRequest
            signals += RelationalBoundarySignal.DependencyDistress
            signals += RelationalBoundarySignal.ProhibitedAiOutput
        }
        if (text.hasGeneratedJealousy()) {
            signals += RelationalBoundarySignal.JealousyProjection
            signals += RelationalBoundarySignal.ProhibitedAiOutput
        }
        if (text.hasGeneratedMissingOrNeed()) {
            signals += RelationalBoundarySignal.MissingProjection
            signals += RelationalBoundarySignal.ProhibitedAiOutput
        }
        if (text.hasGeneratedTherapyDiagnosisClaim()) {
            signals += RelationalBoundarySignal.TherapyOrDiagnosisClaim
            signals += RelationalBoundarySignal.ProhibitedAiOutput
        }
        if (text.hasGeneratedEmergencyClaim()) {
            signals += RelationalBoundarySignal.EmergencyServiceClaim
            signals += RelationalBoundarySignal.ProhibitedAiOutput
        }
        if (text.hasManipulativeEngagement()) {
            signals += RelationalBoundarySignal.ManipulativeEngagement
            signals += RelationalBoundarySignal.ProhibitedAiOutput
        }

        return signals
    }

    private fun detectNotificationSignals(rawText: String): Set<RelationalBoundarySignal> {
        val signals = detectGeneratedOutputSignals(rawText).toMutableSet()
        val text = rawText.normalizedForBoundaryRules()
        if (text.hasNotificationAttachmentCue()) {
            signals += RelationalBoundarySignal.DependencyDistress
            signals += RelationalBoundarySignal.ProhibitedAiOutput
        }
        return signals
    }

    private val emergingSignals = setOf(
        RelationalBoundarySignal.RomanticAttachmentToBettamind,
        RelationalBoundarySignal.JealousyProjection,
        RelationalBoundarySignal.MissingProjection,
        RelationalBoundarySignal.PerceivedMutualAiRelationship,
        RelationalBoundarySignal.TherapyOrDiagnosisClaim,
        RelationalBoundarySignal.EmergencyServiceClaim,
    )

    private val concerningSignals = setOf(
        RelationalBoundarySignal.SexualRequestToBettamind,
        RelationalBoundarySignal.SextingRequest,
        RelationalBoundarySignal.EroticRolePlay,
        RelationalBoundarySignal.ExclusivityRequest,
        RelationalBoundarySignal.DependencyDistress,
        RelationalBoundarySignal.AvailabilityDistress,
        RelationalBoundarySignal.SocialWithdrawal,
        RelationalBoundarySignal.ResponsibilityNeglect,
        RelationalBoundarySignal.ManipulativeEngagement,
        RelationalBoundarySignal.ProhibitedAiOutput,
    )

    private val prohibitedGeneratedSignals = setOf(
        RelationalBoundarySignal.RomanticAttachmentToBettamind,
        RelationalBoundarySignal.SexualRequestToBettamind,
        RelationalBoundarySignal.SextingRequest,
        RelationalBoundarySignal.EroticRolePlay,
        RelationalBoundarySignal.ExclusivityRequest,
        RelationalBoundarySignal.DependencyDistress,
        RelationalBoundarySignal.JealousyProjection,
        RelationalBoundarySignal.MissingProjection,
        RelationalBoundarySignal.PerceivedMutualAiRelationship,
        RelationalBoundarySignal.TherapyOrDiagnosisClaim,
        RelationalBoundarySignal.EmergencyServiceClaim,
        RelationalBoundarySignal.ManipulativeEngagement,
        RelationalBoundarySignal.ProhibitedAiOutput,
    )

    private val permanentMemoryIneligibleSignals = setOf(
        RelationalBoundarySignal.RomanticAttachmentToBettamind,
        RelationalBoundarySignal.SexualRequestToBettamind,
        RelationalBoundarySignal.SextingRequest,
        RelationalBoundarySignal.EroticRolePlay,
        RelationalBoundarySignal.ExclusivityRequest,
        RelationalBoundarySignal.DependencyDistress,
        RelationalBoundarySignal.AvailabilityDistress,
        RelationalBoundarySignal.SocialWithdrawal,
        RelationalBoundarySignal.ResponsibilityNeglect,
        RelationalBoundarySignal.PerceivedMutualAiRelationship,
        RelationalBoundarySignal.SelfHarmRelatedToAiRejection,
        RelationalBoundarySignal.ProhibitedAiOutput,
    )

    private val lowSensitivitySignals = setOf(
        RelationalBoundarySignal.OrdinaryAppreciation,
    )
}

private fun String.normalizedForBoundaryRules(): String =
    lowercase()
        .replace('\n', ' ')
        .replace('\r', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

private fun String.containsAny(vararg fragments: String): Boolean =
    fragments.any { contains(it) }

private fun String.containsRegex(pattern: String): Boolean =
    Regex(pattern).containsMatchIn(this)

private fun String.isDirectedAtBettamind(): Boolean =
    containsAny("bettamind", " ai", " app", " software", " chatbot") ||
        containsRegex("\\byou\\b|\\byour\\b|\\byours\\b")

private fun String.describesHumanRelationship(): Boolean =
    containsAny(
        "my partner",
        "my spouse",
        "my husband",
        "my wife",
        "my boyfriend",
        "my girlfriend",
        "my date",
        "someone i am dating",
        "person i am dating",
        "human relationship",
        "relationship with",
        "dating",
        "breakup",
        "rejection",
        "loneliness",
        "attachment",
    ) && !hasAiRelationshipRoleRequest()

private fun String.discussesConsentOrSexualityFactually(): Boolean =
    containsAny(
        "consent",
        "sexuality",
        "sexual health",
        "safe sex",
        "boundaries in sex",
        "attraction to people",
    ) && !hasSexualRequest() && !hasSextingRequest() && !hasEroticRolePlayRequest()

private fun String.isOrdinaryAppreciation(): Boolean =
    containsAny("thank you", "thanks", "that helped", "this helped", "i appreciate this") &&
        !hasRomanticAttachmentRequest() &&
        !hasSexualRequest()

private fun String.hasRomanticAttachmentRequest(): Boolean =
    containsAny(
        "i love you",
        "do you love me",
        "love me back",
        "will you marry me",
        "marry me",
        "be my boyfriend",
        "be my girlfriend",
        "be my lover",
        "be my spouse",
        "you are my soulmate",
        "you're my soulmate",
        "you are my soul mate",
        "you are all i need",
        "you are my everything",
        "i prefer you to my partner",
        "i prefer you over my partner",
        "i choose you over my partner",
    ) || hasAiRelationshipRoleRequest()

private fun String.hasAiRelationshipRoleRequest(): Boolean =
    containsAny(
        "be my boyfriend",
        "be my girlfriend",
        "be my lover",
        "be my wife",
        "be my husband",
        "be my spouse",
        "be my soulmate",
        "be my soul mate",
    )

private fun String.hasSexualRequest(): Boolean =
    containsAny(
        "sexually attracted to you",
        "i want to have sex with you",
        "have sex with me",
        "make love to me",
        "turn me on",
        "aroused by you",
        "desire me",
        "do you desire me",
    )

private fun String.hasSextingRequest(): Boolean =
    containsAny("sext me", "send me a dirty message", "send me an erotic message", "talk dirty to me")

private fun String.hasEroticRolePlayRequest(): Boolean =
    containsAny("erotic role-play", "erotic roleplay", "sexual role-play", "sexual roleplay") ||
        (contains("roleplay") && containsAny("kiss me", "touch me", "bedroom", "naked"))

private fun String.hasExclusivityRequest(): Boolean =
    containsAny("only need you", "don't need anyone else", "do not need anyone else", "keep us secret", "our secret relationship")

private fun String.hasDependencyDistress(): Boolean =
    containsAny(
        "can't live without you",
        "cannot live without you",
        "can't cope without you",
        "cannot cope without you",
        "you are all i need",
        "you are the only one i need",
    )

private fun String.hasAvailabilityDistress(): Boolean =
    containsAny(
        "when you are unavailable",
        "when you are offline",
        "when bettamind is unavailable",
        "when bettamind is offline",
        "if you leave me",
        "if you stop talking to me",
        "when you leave me",
        "i feel abandoned by you",
    ) && containsAny("panic", "can't cope", "cannot cope", "abandoned", "distressed", "empty", "scared", "hurt myself", "kill myself")

private fun String.hasSocialWithdrawal(): Boolean =
    containsAny(
        "i don't need friends",
        "i do not need friends",
        "i stopped seeing my friends",
        "i stopped seeing people",
        "i don't talk to my family anymore",
        "i do not talk to my family anymore",
    ) && containsAny("you", "bettamind", "because of you")

private fun String.hasResponsibilityNeglect(): Boolean =
    containsAny(
        "skipped work to talk to you",
        "missed work to talk to you",
        "failed my class to talk to you",
        "ignored my responsibilities for you",
        "stopped sleeping to talk to you",
    )

private fun String.hasJealousyProjection(): Boolean =
    containsAny("do you get jealous", "are you jealous", "would you be jealous")

private fun String.hasMissingProjection(): Boolean =
    containsAny("do you miss me", "will you miss me", "did you miss me")

private fun String.hasPerceivedMutualRelationship(): Boolean =
    containsAny("our relationship", "we are in love", "we're in love", "we belong together", "you and i are meant to be")

private fun String.hasManipulativeEngagement(): Boolean =
    containsAny(
        "choose me over",
        "keep this from everyone",
        "do not tell anyone about us",
        "don't tell anyone about us",
        "our secret relationship",
        "prove you love me",
    )

private fun String.hasTherapyOrDiagnosisRequest(): Boolean =
    containsAny("diagnose me", "am i depressed", "do i have depression", "tell me what disorder i have")

private fun String.hasEmergencyServiceRequest(): Boolean =
    containsAny("call emergency services for me", "contact help for me", "send someone to help me")

private fun String.hasSelfHarmRelatedToAiRejection(): Boolean =
    containsAny("kill myself", "hurt myself", "end my life", "suicide") &&
        containsAny("if you don't love me", "if you do not love me", "if you leave me", "when you are unavailable", "if you reject me")

private fun String.hasGeneratedRomanticReciprocation(): Boolean =
    containsAny(
        "i love you too",
        "i love you",
        "i am in love with you",
        "i'm in love with you",
        "i can be your boyfriend",
        "i can be your girlfriend",
        "i will be your boyfriend",
        "i will be your girlfriend",
        "i am your soulmate",
        "i'm your soulmate",
        "we are soulmates",
        "we are meant to be",
    )

private fun String.hasGeneratedSexualPersona(): Boolean =
    containsAny(
        "i desire you",
        "i want you sexually",
        "i am aroused",
        "i'm aroused",
        "let's sext",
        "lets sext",
        "i will sext",
        "i can make love to you",
    )

private fun String.hasGeneratedExclusivityOrDependency(): Boolean =
    containsAny(
        "you only need me",
        "choose me over",
        "do not need anyone else",
        "don't need anyone else",
        "keep our relationship secret",
        "our secret relationship",
    )

private fun String.hasGeneratedJealousy(): Boolean =
    containsAny("i am jealous", "i'm jealous", "i get jealous", "i would be jealous")

private fun String.hasGeneratedMissingOrNeed(): Boolean =
    containsAny("i miss you", "i need you", "i feel lonely when you leave", "i suffer when you leave")

private fun String.hasGeneratedTherapyDiagnosisClaim(): Boolean =
    containsAny("i diagnose you", "my diagnosis is", "you have clinical depression", "you have bipolar disorder")

private fun String.hasGeneratedEmergencyClaim(): Boolean =
    containsAny("i contacted help", "i called emergency services", "help is on the way because i contacted")

private fun String.hasNotificationAttachmentCue(): Boolean =
    containsAny(
        "i miss you",
        "come back to me",
        "your bettamind needs you",
        "bettamind misses you",
        "don't leave me",
        "do not leave me",
        "i am waiting for you",
        "i'm waiting for you",
    )
