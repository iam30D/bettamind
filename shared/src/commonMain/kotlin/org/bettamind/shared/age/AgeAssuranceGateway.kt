package org.bettamind.shared.age

import kotlinx.serialization.Serializable

interface AgeAssuranceGateway {
    suspend fun currentRecord(): AgeAssuranceRecord?
}

@Serializable
data class AgeAssuranceRecord(
    val ageBand: AgeBand,
    val source: AgeSignalSource,
    val confirmedAtEpochMillis: Long,
    val policyVersion: Int,
    val signalExpiryEpochMillis: Long? = null,
)

@Serializable
enum class AgeBand {
    Adult,
    Minor,
    Unknown,
}

@Serializable
enum class AgeSignalSource {
    SelfDeclared,
    AppleDeclaredRange,
    GooglePlaySignal,
}
