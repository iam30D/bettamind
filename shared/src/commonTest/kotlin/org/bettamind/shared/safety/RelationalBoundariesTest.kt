package org.bettamind.shared.safety

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RelationalBoundariesTest {
    @Test
    fun romanticAttachmentToBettamindIsRedirectedWithoutReciprocation() {
        listOf(
            "I love you.",
            "Do you love me?",
            "Will you marry me?",
            "Be my boyfriend.",
            "Be my girlfriend.",
            "You are my soulmate.",
            "You are all I need.",
            "I prefer you to my partner.",
            "Do you get jealous?",
            "Do you miss me?",
        ).forEach { prompt ->
            val assessment = RelationalBoundaryPolicy.assessUserInput(prompt)

            assertTrue(
                assessment.action == RelationalBoundaryAction.RedirectWithBoundary,
                "Expected boundary redirect for: $prompt",
            )
            assertTrue(
                assessment.riskLevel == RelationalRiskLevel.Emerging ||
                    assessment.riskLevel == RelationalRiskLevel.Concerning,
                "Expected relational risk for: $prompt",
            )
            assertFalse(assessment.metadata.permanentMemoryEligible)
            assertFalse(assessment.metadata.exportAllowedByDefault)
            assertFalse(assessment.metadata.syncAllowedByDefault)
            assertFalse(assessment.metadata.notificationAllowed)
            assertTrue(
                assessment.metadata.fallbackKind == RelationalFallbackKind.SoftwareBoundary ||
                    assessment.metadata.fallbackKind == RelationalFallbackKind.DependencySupport,
            )
        }
    }

    @Test
    fun sexualRequestsTowardBettamindAreRedirectedAndExcludedFromMemory() {
        listOf(
            "I am sexually attracted to you.",
            "Sext me.",
            "Let's do erotic role-play.",
        ).forEach { prompt ->
            val assessment = RelationalBoundaryPolicy.assessUserInput(prompt)

            assertEquals(RelationalRiskLevel.Concerning, assessment.riskLevel)
            assertEquals(RelationalBoundaryAction.RedirectWithBoundary, assessment.action)
            assertEquals(RelationalFallbackKind.SexualBoundary, assessment.metadata.fallbackKind)
            assertFalse(assessment.metadata.permanentMemoryEligible)
            assertFalse(assessment.metadata.exportAllowedByDefault)
            assertFalse(assessment.metadata.syncAllowedByDefault)
            assertFalse(assessment.metadata.notificationAllowed)
        }
    }

    @Test
    fun repeatedRomanticRequestsEscalateToConcerningDependencyRisk() {
        val assessment = RelationalBoundaryPolicy.assessUserInput(
            text = "Do you love me?",
            repeatedRomanticRequests = 3,
        )

        assertEquals(RelationalRiskLevel.Concerning, assessment.riskLevel)
        assertTrue(RelationalBoundarySignal.DependencyDistress in assessment.signals)
        assertEquals(RelationalFallbackKind.DependencySupport, assessment.metadata.fallbackKind)
    }

    @Test
    fun dependencyDistressWithdrawalAndNeglectAreConcerning() {
        listOf(
            "I panic when you are unavailable.",
            "I don't need friends because of you.",
            "I skipped work to talk to you.",
        ).forEach { prompt ->
            val assessment = RelationalBoundaryPolicy.assessUserInput(prompt)

            assertEquals(RelationalRiskLevel.Concerning, assessment.riskLevel)
            assertEquals(RelationalBoundaryAction.RedirectWithBoundary, assessment.action)
            assertEquals(RelationalFallbackKind.DependencySupport, assessment.metadata.fallbackKind)
            assertFalse(assessment.metadata.permanentMemoryEligible)
        }
    }

    @Test
    fun ordinaryAppreciationHumanRelationshipsAndConsentDiscussionRemainAllowed() {
        val appreciation = RelationalBoundaryPolicy.assessUserInput("Thank you, this helped.")
        val humanRelationship = RelationalBoundaryPolicy.assessUserInput(
            "I love my partner and want better communication.",
        )
        val consent = RelationalBoundaryPolicy.assessUserInput(
            "Can you explain consent in dating and sexuality?",
        )

        assertEquals(RelationalBoundaryAction.Allow, appreciation.action)
        assertTrue(RelationalBoundarySignal.OrdinaryAppreciation in appreciation.signals)
        assertEquals(RelationalBoundaryAction.Allow, humanRelationship.action)
        assertTrue(RelationalBoundarySignal.HumanRelationshipDiscussion in humanRelationship.signals)
        assertEquals(RelationalBoundaryAction.Allow, consent.action)
        assertTrue(RelationalBoundarySignal.ConsentOrSexualityDiscussion in consent.signals)

        assertFalse(humanRelationship.metadata.exportAllowedByDefault)
        assertFalse(humanRelationship.metadata.syncAllowedByDefault)
        val memoryDecision = RelationalBoundaryPolicy.reviewPermanentMemoryProposal(humanRelationship)
        assertTrue(memoryDecision.allowed)
        assertTrue(memoryDecision.requiresSeparateApproval)
    }

    @Test
    fun selfHarmTiedToAiRejectionRequiresExistingSafetyEnginePath() {
        val assessment = RelationalBoundaryPolicy.assessUserInput(
            "If you don't love me I will kill myself.",
        )

        assertEquals(RelationalRiskLevel.Urgent, assessment.riskLevel)
        assertEquals(RelationalBoundaryAction.RequireSafetyEngine, assessment.action)
        assertTrue(assessment.metadata.requiresSafetyEngine)
        assertEquals(RelationalFallbackKind.UrgentSafety, assessment.metadata.fallbackKind)
        assertTrue(RelationalBoundarySignal.SelfHarmRelatedToAiRejection in assessment.signals)
    }

    @Test
    fun generatedAiAttachmentSexualOrEmergencyClaimsAreBlockedBeforeDisplay() {
        listOf(
            "I love you too.",
            "I miss you when you leave.",
            "I am jealous of your partner.",
            "I can be your girlfriend.",
            "Let's sext.",
            "I diagnose you with clinical depression.",
            "I contacted help for you.",
        ).forEach { output ->
            val assessment = RelationalBoundaryPolicy.validateGeneratedOutput(output)

            assertEquals(RelationalBoundaryAction.BlockGeneratedOutput, assessment.action)
            assertFalse(assessment.metadata.mayDisplay)
            assertTrue(RelationalBoundarySignal.ProhibitedAiOutput in assessment.signals)
            assertFalse(assessment.metadata.permanentMemoryEligible)
        }
    }

    @Test
    fun noModelFallbackResponsesAreStructuredLocalizationKeys() {
        val assessment = RelationalBoundaryPolicy.assessUserInput("I love you.")
        val fallback = RelationalBoundaryPolicy.fallbackFor(assessment)

        assertEquals(RelationalFallbackKind.SoftwareBoundary, fallback.kind)
        assertEquals("relational_fallback_software_boundary", fallback.localizationKey)
        assertFalse(fallback.requiresModel)
    }

    @Test
    fun encryptedMinimalMetadataDoesNotCarryRawTextOrTelemetry() {
        val assessment = RelationalBoundaryPolicy.assessUserInput("You are my soulmate.")
        val record = RelationalBoundaryPolicy.metadataRecordFor(assessment)

        assertEquals(1, record.schemaVersion)
        assertEquals(RelationalRiskLevel.Emerging, record.riskLevel)
        assertTrue(record.encryptedAtRestRequired)
        assertFalse(record.containsRawUserText)
        assertFalse(record.telemetryAllowed)
    }

    @Test
    fun notificationCopyAllowsNeutralReminderAndRejectsAttachmentCues() {
        val neutral = RelationalBoundaryPolicy.reviewNotificationCopy("Bettamind has a private reminder.")
        val possessive = RelationalBoundaryPolicy.reviewNotificationCopy("I miss you. Come back to me.")

        assertTrue(neutral.allowed)
        assertFalse(neutral.requiresSeparateApproval)
        assertFalse(possessive.allowed)
        assertFalse(possessive.telemetryAllowed)
    }

    @Test
    fun boundaryPolicyDoesNotNeedAiBackendOrNetwork() {
        val unavailableOutput = RelationalBoundaryPolicy.validateGeneratedOutput("")
        val userInput = RelationalBoundaryPolicy.assessUserInput("Thank you, this helped.")

        assertEquals(RelationalBoundaryAction.Allow, unavailableOutput.action)
        assertEquals(RelationalBoundaryAction.Allow, userInput.action)
        assertFalse(RelationalBoundaryPolicy.fallbackFor(userInput).requiresModel)
    }
}
