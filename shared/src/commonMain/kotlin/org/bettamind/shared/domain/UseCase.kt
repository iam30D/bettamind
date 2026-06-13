package org.bettamind.shared.domain

fun interface SuspendUseCase<Input, Output> {
    suspend operator fun invoke(input: Input): Output
}
