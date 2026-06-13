package org.bettamind.shared.growth

import org.bettamind.shared.age.AgeAssuranceRecord
import org.bettamind.shared.age.AgeBand

enum class GrowthStep {
    Awareness,
    Choice,
    Action,
    Consequence,
    Reflection,
    Repair,
    Growth,
}

enum class AdultGateState {
    NeedsAdultConfirmation,
    AdultConfirmed,
    BlockedMinorOrUnknown,
}

enum class NarrativeStorageStatus {
    LockedUntilAdult,
    EncryptedStorageUnavailable,
    EncryptedStorageReady,
}

data class GrowthSessionState(
    val adultGateState: AdultGateState,
    val narrativeStorageStatus: NarrativeStorageStatus,
    val currentStep: GrowthStep = GrowthStep.Awareness,
    val completedSteps: Set<GrowthStep> = emptySet(),
) {
    val personalFeaturesAvailable: Boolean
        get() = adultGateState == AdultGateState.AdultConfirmed

    val narrativeStorageAvailable: Boolean
        get() = narrativeStorageStatus == NarrativeStorageStatus.EncryptedStorageReady

    val isComplete: Boolean
        get() = completedSteps.containsAll(GrowthStep.entries)
}

object DeterministicGrowthEngine {
    fun initial(
        ageAssuranceRecord: AgeAssuranceRecord? = null,
        encryptedStorageAvailable: Boolean = false,
    ): GrowthSessionState =
        when (ageAssuranceRecord?.ageBand) {
            AgeBand.Adult -> adultConfirmed(encryptedStorageAvailable)
            AgeBand.Minor, AgeBand.Unknown, null -> locked()
        }

    fun adultConfirmed(encryptedStorageAvailable: Boolean): GrowthSessionState =
        GrowthSessionState(
            adultGateState = AdultGateState.AdultConfirmed,
            narrativeStorageStatus = if (encryptedStorageAvailable) {
                NarrativeStorageStatus.EncryptedStorageReady
            } else {
                NarrativeStorageStatus.EncryptedStorageUnavailable
            },
        )

    fun blockedMinorOrUnknown(): GrowthSessionState =
        GrowthSessionState(
            adultGateState = AdultGateState.BlockedMinorOrUnknown,
            narrativeStorageStatus = NarrativeStorageStatus.LockedUntilAdult,
        )

    fun locked(): GrowthSessionState =
        GrowthSessionState(
            adultGateState = AdultGateState.NeedsAdultConfirmation,
            narrativeStorageStatus = NarrativeStorageStatus.LockedUntilAdult,
        )

    fun advance(state: GrowthSessionState): GrowthSessionState {
        if (!state.personalFeaturesAvailable || state.isComplete) {
            return state
        }

        val completed = state.completedSteps + state.currentStep
        val nextStep = GrowthStep.entries
            .dropWhile { it != state.currentStep }
            .drop(1)
            .firstOrNull()
            ?: state.currentStep

        return state.copy(
            currentStep = nextStep,
            completedSteps = completed,
        )
    }

    fun resetFrom(state: GrowthSessionState): GrowthSessionState =
        state.copy(
            currentStep = GrowthStep.Awareness,
            completedSteps = emptySet(),
        )
}
