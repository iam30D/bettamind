package org.bettamind.shared.privacy

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidPrivacyLockAuthenticator(
    private val activity: FragmentActivity,
) : UserAuthenticator {
    override fun capabilities(): AuthenticationCapabilities {
        val manager = BiometricManager.from(activity)
        val strongBiometricAvailable =
            manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
        val deviceCredentialAvailable =
            manager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) ==
                BiometricManager.BIOMETRIC_SUCCESS
        return AuthenticationCapabilities(
            strongBiometricAvailable = strongBiometricAvailable,
            deviceCredentialAvailable = deviceCredentialAvailable,
            bettamindPinAvailable = false,
            bettamindPassphraseAvailable = false,
        )
    }

    override suspend fun authenticate(request: UserAuthenticationRequest): UserAuthenticationResult =
        suspendCancellableCoroutine { continuation ->
            val authenticators = request.allowedMethods.toAndroidAuthenticators()
            if (authenticators == 0) {
                continuation.resume(UserAuthenticationResult.Unavailable("No Android authentication method is available."))
                return@suspendCancellableCoroutine
            }

            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) {
                            continuation.resume(
                                UserAuthenticationResult.Success(
                                    method = request.allowedMethods.preferredPlatformMethod(),
                                ),
                            )
                        }
                    }

                    override fun onAuthenticationFailed() {
                        if (continuation.isActive) {
                            continuation.resume(UserAuthenticationResult.Failed)
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (continuation.isActive) {
                            continuation.resume(errorCode.toAuthenticationResult(errString.toString()))
                        }
                    }
                },
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(request.title)
                .setSubtitle(request.subtitle)
                .setDescription(request.reason)
                .setAllowedAuthenticators(authenticators)
                .apply {
                    if (BiometricManager.Authenticators.DEVICE_CREDENTIAL !in authenticators.authenticatorFlags()) {
                        setNegativeButtonText(request.cancelLabel)
                    }
                }
                .build()

            prompt.authenticate(promptInfo)
            continuation.invokeOnCancellation {
                prompt.cancelAuthentication()
            }
        }

    private fun Set<AuthenticationMethod>.toAndroidAuthenticators(): Int {
        var authenticators = 0
        if (AuthenticationMethod.StrongBiometric in this) {
            authenticators = authenticators or BiometricManager.Authenticators.BIOMETRIC_STRONG
        }
        if (AuthenticationMethod.DeviceCredential in this) {
            authenticators = authenticators or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }
        return authenticators
    }

    private fun Set<AuthenticationMethod>.preferredPlatformMethod(): AuthenticationMethod =
        when {
            AuthenticationMethod.StrongBiometric in this -> AuthenticationMethod.StrongBiometric
            AuthenticationMethod.DeviceCredential in this -> AuthenticationMethod.DeviceCredential
            else -> AuthenticationMethod.DeviceCredential
        }

    private fun Int.authenticatorFlags(): Set<Int> =
        buildSet {
            if (this@authenticatorFlags and BiometricManager.Authenticators.BIOMETRIC_STRONG != 0) {
                add(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            }
            if (this@authenticatorFlags and BiometricManager.Authenticators.DEVICE_CREDENTIAL != 0) {
                add(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            }
        }

    private fun Int.toAuthenticationResult(message: String): UserAuthenticationResult =
        when (this) {
            BiometricPrompt.ERROR_CANCELED,
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_USER_CANCELED,
            -> UserAuthenticationResult.Cancelled

            BiometricPrompt.ERROR_LOCKOUT,
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
            -> UserAuthenticationResult.Lockout()

            BiometricPrompt.ERROR_HW_NOT_PRESENT,
            BiometricPrompt.ERROR_HW_UNAVAILABLE,
            BiometricPrompt.ERROR_NO_BIOMETRICS,
            BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
            BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED,
            -> UserAuthenticationResult.Unavailable(message)

            else -> UserAuthenticationResult.Failed
        }
}
