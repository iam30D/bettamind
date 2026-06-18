package org.bettamind.shared.privacy

import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorAppCancel
import platform.LocalAuthentication.LAErrorAuthenticationFailed
import platform.LocalAuthentication.LAErrorBiometryLockout
import platform.LocalAuthentication.LAErrorBiometryNotAvailable
import platform.LocalAuthentication.LAErrorBiometryNotEnrolled
import platform.LocalAuthentication.LAErrorPasscodeNotSet
import platform.LocalAuthentication.LAErrorSystemCancel
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAErrorUserFallback
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlinx.coroutines.suspendCancellableCoroutine

@OptIn(ExperimentalForeignApi::class)
class IosPrivacyLockAuthenticator : UserAuthenticator {
    override fun capabilities(): AuthenticationCapabilities {
        val biometricAvailable = canEvaluate(LAPolicyDeviceOwnerAuthenticationWithBiometrics)
        val deviceOwnerAuthAvailable = canEvaluate(LAPolicyDeviceOwnerAuthentication)
        return AuthenticationCapabilities(
            strongBiometricAvailable = biometricAvailable,
            deviceCredentialAvailable = deviceOwnerAuthAvailable,
            bettamindPinAvailable = false,
            bettamindPassphraseAvailable = false,
        )
    }

    override suspend fun authenticate(request: UserAuthenticationRequest): UserAuthenticationResult =
        suspendCancellableCoroutine { continuation ->
            val policy = request.allowedMethods.toIosPolicy()
            if (policy == null) {
                continuation.resume(UserAuthenticationResult.Unavailable("No iOS authentication method is available."))
                return@suspendCancellableCoroutine
            }

            val context = LAContext()
            context.localizedCancelTitle = request.cancelLabel
            context.evaluatePolicy(policy, localizedReason = request.reason) { success, error ->
                if (!continuation.isActive) {
                    return@evaluatePolicy
                }
                if (success) {
                    continuation.resume(
                        UserAuthenticationResult.Success(
                            method = request.allowedMethods.preferredPlatformMethod(),
                        ),
                    )
                } else {
                    continuation.resume(error.toAuthenticationResult())
                }
            }
        }

    private fun canEvaluate(policy: Long): Boolean =
        LAContext().canEvaluatePolicy(policy, error = null)

    private fun Set<AuthenticationMethod>.toIosPolicy(): Long? =
        when {
            AuthenticationMethod.DeviceCredential in this -> LAPolicyDeviceOwnerAuthentication
            AuthenticationMethod.StrongBiometric in this -> LAPolicyDeviceOwnerAuthenticationWithBiometrics
            else -> null
        }

    private fun Set<AuthenticationMethod>.preferredPlatformMethod(): AuthenticationMethod =
        when {
            AuthenticationMethod.StrongBiometric in this -> AuthenticationMethod.StrongBiometric
            AuthenticationMethod.DeviceCredential in this -> AuthenticationMethod.DeviceCredential
            else -> AuthenticationMethod.DeviceCredential
        }

    private fun NSError?.toAuthenticationResult(): UserAuthenticationResult {
        val code = this?.code ?: return UserAuthenticationResult.Failed
        return when (code) {
            LAErrorUserCancel,
            LAErrorUserFallback,
            LAErrorSystemCancel,
            LAErrorAppCancel,
            -> UserAuthenticationResult.Cancelled

            LAErrorBiometryLockout -> UserAuthenticationResult.Lockout()
            LAErrorBiometryNotAvailable,
            LAErrorBiometryNotEnrolled,
            LAErrorPasscodeNotSet,
            -> UserAuthenticationResult.Unavailable(localizedDescription)

            LAErrorAuthenticationFailed -> UserAuthenticationResult.Failed
            else -> UserAuthenticationResult.Failed
        }
    }
}
