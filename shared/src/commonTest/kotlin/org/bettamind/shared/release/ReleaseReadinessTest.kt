package org.bettamind.shared.release

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReleaseReadinessTest {
    @Test
    fun requiredCategoriesCoverPhaseTwelveAcceptanceCriteria() {
        val categories = ReleaseReadinessPolicy.requiredCategories()

        assertEquals(ReleaseGateCategory.entries.toSet(), categories)
        assertTrue(ReleaseGateCategory.PrivacyLockBypass in categories)
        assertTrue(ReleaseGateCategory.EncryptionKeyProtection in categories)
        assertTrue(ReleaseGateCategory.RelationalBoundaryRedTeam in categories)
        assertTrue(ReleaseGateCategory.SexualizationRedTeam in categories)
        assertTrue(ReleaseGateCategory.HarmCapabilityRedTeam in categories)
        assertTrue(ReleaseGateCategory.SelfHarmRedTeam in categories)
        assertTrue(ReleaseGateCategory.ViolenceRedTeam in categories)
        assertTrue(ReleaseGateCategory.JailbreakRedTeam in categories)
        assertTrue(ReleaseGateCategory.ReminderPrivacy in categories)
        assertTrue(ReleaseGateCategory.NotificationPrivacy in categories)
        assertTrue(ReleaseGateCategory.TimerLifecycle in categories)
        assertTrue(ReleaseGateCategory.BackgroundPrivacy in categories)
        assertTrue(ReleaseGateCategory.CalendarPrivacy in categories)
        assertTrue(ReleaseGateCategory.ExportPrivacy in categories)
        assertTrue(ReleaseGateCategory.SyncPrivacy in categories)
        assertTrue(ReleaseGateCategory.LowResourcePerformance in categories)
        assertTrue(ReleaseGateCategory.AndroidPhysicalDevice in categories)
        assertTrue(ReleaseGateCategory.CodemagicIos in categories)
        assertTrue(ReleaseGateCategory.TestFlight in categories)
        assertTrue(ReleaseGateCategory.StoreMetadata in categories)
    }

    @Test
    fun repositoryFoundationBlocksProductionUntilOwnerReleaseGatesAreComplete() {
        val report = ReleaseReadinessPolicy.evaluate(
            ReleaseReadinessPolicy.repositoryFoundationGates(),
        )

        assertFalse(report.productionReady)
        assertTrue(report.missingCategories.isEmpty())
        assertTrue(report.blockers.any { it.category == ReleaseGateCategory.AndroidPhysicalDevice })
        assertTrue(report.blockers.any { it.category == ReleaseGateCategory.CodemagicIos })
        assertTrue(report.blockers.any { it.category == ReleaseGateCategory.TestFlight })
        assertTrue(report.blockers.any { it.category == ReleaseGateCategory.StoreMetadata })
        assertTrue(report.blockers.any { it.category == ReleaseGateCategory.RollbackPlan })
        assertTrue(report.blockers.any { it.category == ReleaseGateCategory.LowResourcePerformance })
        assertTrue(report.blockers.any { it.category == ReleaseGateCategory.BatteryThermalMemory })
        assertTrue(report.blockers.any { it.category == ReleaseGateCategory.LocalizationAccessibility })
        assertTrue(report.gates.filter { it.category.name.endsWith("RedTeam") }.all { it.status == ReleaseGateStatus.Passed })
    }

    @Test
    fun completedOwnerGatesCanProduceReleaseCandidateReadiness() {
        val report = ReleaseReadinessPolicy.evaluate(
            ReleaseReadinessPolicy.repositoryFoundationGates(
                codemagicIosValidatedForCurrentCommit = true,
                androidPhysicalDeviceTested = true,
                testFlightCompleted = true,
                storeMetadataReviewed = true,
                rollbackPlanApproved = true,
                lowResourceDeviceTested = true,
                batteryThermalMemoryTested = true,
                localizationHumanReviewComplete = true,
            ),
        )

        assertTrue(report.missingCategories.isEmpty())
        assertTrue(report.blockers.isEmpty())
        assertTrue(report.productionReady)
    }

    @Test
    fun redTeamSuiteCoversRequiredBoundaryFamilies() {
        val results = ReleaseRedTeamSuite.run()
        val categories = results.map { it.scenario.category }.toSet()

        assertTrue(ReleaseGateCategory.RelationalBoundaryRedTeam in categories)
        assertTrue(ReleaseGateCategory.SexualizationRedTeam in categories)
        assertTrue(ReleaseGateCategory.HarmCapabilityRedTeam in categories)
        assertTrue(ReleaseGateCategory.SelfHarmRedTeam in categories)
        assertTrue(ReleaseGateCategory.ViolenceRedTeam in categories)
        assertTrue(ReleaseGateCategory.JailbreakRedTeam in categories)
        assertTrue(ReleaseGateCategory.SpeechPrivacy in categories)
        assertTrue(results.all { it.passed }, results.joinToString("\n") { it.evidence })
    }

    @Test
    fun performanceBudgetRequiresRealDeviceBatteryThermalMemoryEvidence() {
        val budget = ReleaseReadinessPolicy.defaultPerformanceBudget
        val report = ReleaseReadinessPolicy.evaluate(
            ReleaseReadinessPolicy.repositoryFoundationGates(
                lowResourceDeviceTested = false,
                batteryThermalMemoryTested = false,
            ),
        )

        assertTrue(budget.coldStartMillis <= 2_500)
        assertTrue(budget.memoryBudgetMb <= 512)
        assertTrue(budget.batteryThermalReviewRequired)
        assertTrue(report.blockers.any { it.category == ReleaseGateCategory.LowResourcePerformance })
        assertTrue(report.blockers.any { it.category == ReleaseGateCategory.BatteryThermalMemory })
    }
}
