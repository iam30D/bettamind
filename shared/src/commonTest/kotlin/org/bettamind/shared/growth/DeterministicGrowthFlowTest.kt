package org.bettamind.shared.growth

import org.bettamind.shared.age.AgeAssuranceRecord
import org.bettamind.shared.age.AgeBand
import org.bettamind.shared.age.AgeSignalSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeterministicGrowthFlowTest {
    @Test
    fun stepsStayInLockedHumanGrowthOrder() {
        assertEquals(
            listOf(
                GrowthStep.Awareness,
                GrowthStep.Choice,
                GrowthStep.Action,
                GrowthStep.Consequence,
                GrowthStep.Reflection,
                GrowthStep.Repair,
                GrowthStep.Growth,
            ),
            GrowthStep.entries,
        )
    }

    @Test
    fun unknownAndMinorUsersDoNotEnterPersonalFlow() {
        val unknown = DeterministicGrowthEngine.initial()
        val minor = DeterministicGrowthEngine.initial(ageAssuranceRecord = ageRecord(AgeBand.Minor))

        assertFalse(unknown.personalFeaturesAvailable)
        assertFalse(minor.personalFeaturesAvailable)
        assertEquals(NarrativeStorageStatus.LockedUntilAdult, unknown.narrativeStorageStatus)
        assertEquals(NarrativeStorageStatus.LockedUntilAdult, minor.narrativeStorageStatus)
        assertEquals(unknown, DeterministicGrowthEngine.advance(unknown))
        assertEquals(minor, DeterministicGrowthEngine.advance(minor))
    }

    @Test
    fun adultFlowWorksWithoutCreatingNarrativeStorageFallback() {
        val adult = DeterministicGrowthEngine.initial(
            ageAssuranceRecord = ageRecord(AgeBand.Adult),
            encryptedStorageAvailable = false,
        )

        assertTrue(adult.personalFeaturesAvailable)
        assertFalse(adult.narrativeStorageAvailable)
        assertEquals(NarrativeStorageStatus.EncryptedStorageUnavailable, adult.narrativeStorageStatus)

        val advanced = DeterministicGrowthEngine.advance(adult)

        assertEquals(GrowthStep.Choice, advanced.currentStep)
        assertEquals(setOf(GrowthStep.Awareness), advanced.completedSteps)
        assertFalse(advanced.narrativeStorageAvailable)
    }

    @Test
    fun encryptedNarrativeStorageRequiresAdultGateAndSecureStorage() {
        val adultWithStorage = DeterministicGrowthEngine.initial(
            ageAssuranceRecord = ageRecord(AgeBand.Adult),
            encryptedStorageAvailable = true,
        )
        val unknownWithStorage = DeterministicGrowthEngine.initial(
            ageAssuranceRecord = ageRecord(AgeBand.Unknown),
            encryptedStorageAvailable = true,
        )

        assertTrue(adultWithStorage.narrativeStorageAvailable)
        assertEquals(NarrativeStorageStatus.EncryptedStorageReady, adultWithStorage.narrativeStorageStatus)
        assertFalse(unknownWithStorage.narrativeStorageAvailable)
        assertEquals(NarrativeStorageStatus.LockedUntilAdult, unknownWithStorage.narrativeStorageStatus)
    }

    @Test
    fun completingAllStepsMarksSessionComplete() {
        var state = DeterministicGrowthEngine.adultConfirmed(encryptedStorageAvailable = false)

        repeat(GrowthStep.entries.size) {
            state = DeterministicGrowthEngine.advance(state)
        }

        assertTrue(state.isComplete)
        assertEquals(GrowthStep.Growth, state.currentStep)
    }

    private fun ageRecord(ageBand: AgeBand): AgeAssuranceRecord =
        AgeAssuranceRecord(
            ageBand = ageBand,
            source = AgeSignalSource.SelfDeclared,
            confirmedAtEpochMillis = 1L,
            policyVersion = 1,
        )
}
