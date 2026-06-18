package org.bettamind.shared.privacy

enum class PrivacyLockTimeout(
    val durationMillis: Long?,
) {
    Disabled(null),
    Immediate(0L),
    OneMinute(60_000L),
    FiveMinutes(5 * 60_000L),
    FifteenMinutes(15 * 60_000L),
}

enum class AuthenticationMethod {
    StrongBiometric,
    DeviceCredential,
    BettamindPin,
    BettamindPassphrase,
}

enum class SensitiveAction {
    OpenVault,
    ExportPrivateInformation,
    EnableSync,
    ShareWithProfessional,
    ViewRecoveryData,
    ChangeSecuritySettings,
    ChangeOrDisablePin,
    DeleteAllLocalData,
    AccessHighlySensitiveRecord,
}

data class AuthenticationCapabilities(
    val strongBiometricAvailable: Boolean,
    val deviceCredentialAvailable: Boolean,
    val bettamindPinAvailable: Boolean,
    val bettamindPassphraseAvailable: Boolean,
) {
    val anyAvailable: Boolean =
        strongBiometricAvailable ||
            deviceCredentialAvailable ||
            bettamindPinAvailable ||
            bettamindPassphraseAvailable
}

data class PrivacyLockSettings(
    val timeout: PrivacyLockTimeout = PrivacyLockTimeout.OneMinute,
    val lockOnReopening: Boolean = true,
    val allowStrongBiometric: Boolean = true,
    val allowDeviceCredential: Boolean = true,
    val allowBettamindPin: Boolean = true,
    val allowBettamindPassphrase: Boolean = true,
) {
    val appLockEnabled: Boolean = timeout != PrivacyLockTimeout.Disabled
}

data class UserAuthenticationRequest(
    val action: SensitiveAction,
    val allowedMethods: Set<AuthenticationMethod>,
    val title: String,
    val subtitle: String,
    val reason: String,
    val cancelLabel: String,
)

sealed class UserAuthenticationResult {
    data class Success(val method: AuthenticationMethod) : UserAuthenticationResult()
    data object Cancelled : UserAuthenticationResult()
    data object Failed : UserAuthenticationResult()
    data class Lockout(val retryAfterEpochMillis: Long? = null) : UserAuthenticationResult()
    data class Unavailable(val reason: String? = null) : UserAuthenticationResult()
    data class RateLimited(val retryAfterEpochMillis: Long) : UserAuthenticationResult()
}

interface UserAuthenticator {
    fun capabilities(): AuthenticationCapabilities
    suspend fun authenticate(request: UserAuthenticationRequest): UserAuthenticationResult
}

data class VaultUnlockRequest(
    val action: SensitiveAction,
    val title: String,
    val subtitle: String,
    val reason: String,
    val cancelLabel: String,
)

sealed class VaultKeyReleaseResult {
    data class Released(
        val key: StorageKeyMaterial,
        val method: AuthenticationMethod?,
        val expiresAtEpochMillis: Long?,
        val reusedSession: Boolean,
    ) : VaultKeyReleaseResult()

    data class Denied(val authenticationResult: UserAuthenticationResult) : VaultKeyReleaseResult()
}

class VaultKeyReleaseService(
    private val keyManager: StorageKeyManager,
    private val authenticator: UserAuthenticator,
    private val settings: PrivacyLockSettings = PrivacyLockSettings(),
    private val nowEpochMillis: () -> Long,
) {
    private var unlockedUntilEpochMillis: Long? = null

    fun isUnlocked(): Boolean {
        val expiresAt = unlockedUntilEpochMillis ?: return false
        return expiresAt == NeverExpires || nowEpochMillis() <= expiresAt
    }

    fun lockNow() {
        unlockedUntilEpochMillis = null
    }

    fun onAppBackgrounded() {
        if (settings.lockOnReopening || settings.timeout == PrivacyLockTimeout.Immediate) {
            lockNow()
        }
    }

    fun requiresFreshAuthentication(action: SensitiveAction): Boolean =
        action in StepUpAuthenticationActions

    suspend fun releaseVaultKey(request: VaultUnlockRequest): VaultKeyReleaseResult {
        val existingGrant = unlockedUntilEpochMillis
        if (
            existingGrant != null &&
            isUnlocked() &&
            !requiresFreshAuthentication(request.action)
        ) {
            return VaultKeyReleaseResult.Released(
                key = keyManager.loadOrCreateDatabaseKey(),
                method = null,
                expiresAtEpochMillis = existingGrant.takeUnless { it == NeverExpires },
                reusedSession = true,
            )
        }

        val shouldAuthenticate = settings.appLockEnabled || requiresFreshAuthentication(request.action)
        if (!shouldAuthenticate) {
            return VaultKeyReleaseResult.Released(
                key = keyManager.loadOrCreateDatabaseKey(),
                method = null,
                expiresAtEpochMillis = null,
                reusedSession = false,
            )
        }

        val allowedMethods = allowedMethodsFor(authenticator.capabilities())
        if (allowedMethods.isEmpty()) {
            return VaultKeyReleaseResult.Denied(UserAuthenticationResult.Unavailable("No local authentication method is available."))
        }

        return when (
            val result = authenticator.authenticate(
                UserAuthenticationRequest(
                    action = request.action,
                    allowedMethods = allowedMethods,
                    title = request.title,
                    subtitle = request.subtitle,
                    reason = request.reason,
                    cancelLabel = request.cancelLabel,
                ),
            )
        ) {
            is UserAuthenticationResult.Success -> {
                val expiresAt = expiresAtForSuccessfulUnlock()
                unlockedUntilEpochMillis = expiresAt
                VaultKeyReleaseResult.Released(
                    key = keyManager.loadOrCreateDatabaseKey(),
                    method = result.method,
                    expiresAtEpochMillis = expiresAt.takeUnless { it == NeverExpires },
                    reusedSession = false,
                )
            }

            else -> VaultKeyReleaseResult.Denied(result)
        }
    }

    private fun allowedMethodsFor(capabilities: AuthenticationCapabilities): Set<AuthenticationMethod> =
        buildSet {
            if (settings.allowStrongBiometric && capabilities.strongBiometricAvailable) {
                add(AuthenticationMethod.StrongBiometric)
            }
            if (settings.allowDeviceCredential && capabilities.deviceCredentialAvailable) {
                add(AuthenticationMethod.DeviceCredential)
            }
            if (settings.allowBettamindPin && capabilities.bettamindPinAvailable) {
                add(AuthenticationMethod.BettamindPin)
            }
            if (settings.allowBettamindPassphrase && capabilities.bettamindPassphraseAvailable) {
                add(AuthenticationMethod.BettamindPassphrase)
            }
        }

    private fun expiresAtForSuccessfulUnlock(): Long =
        when (val duration = settings.timeout.durationMillis) {
            null -> NeverExpires
            0L -> nowEpochMillis()
            else -> nowEpochMillis() + duration
        }

    companion object {
        private const val NeverExpires = Long.MAX_VALUE

        private val StepUpAuthenticationActions = setOf(
            SensitiveAction.ExportPrivateInformation,
            SensitiveAction.EnableSync,
            SensitiveAction.ShareWithProfessional,
            SensitiveAction.ViewRecoveryData,
            SensitiveAction.ChangeSecuritySettings,
            SensitiveAction.ChangeOrDisablePin,
            SensitiveAction.DeleteAllLocalData,
            SensitiveAction.AccessHighlySensitiveRecord,
        )
    }
}

enum class CredentialInputKind {
    SixDigitPin,
    AlphanumericPassphrase,
}

data class CredentialPolicyResult(
    val accepted: Boolean,
    val kind: CredentialInputKind? = null,
)

object BettamindCredentialPolicy {
    fun validate(secret: CharArray): CredentialPolicyResult {
        if (secret.all { it.isDigit() }) {
            return CredentialPolicyResult(
                accepted = secret.size >= MinimumPinDigits,
                kind = CredentialInputKind.SixDigitPin,
            )
        }

        val hasLetter = secret.any { it.isLetter() }
        val hasDigit = secret.any { it.isDigit() }
        return CredentialPolicyResult(
            accepted = secret.size >= MinimumPassphraseCharacters && hasLetter && hasDigit,
            kind = CredentialInputKind.AlphanumericPassphrase,
        )
    }

    private const val MinimumPinDigits = 6
    private const val MinimumPassphraseCharacters = 8
}

data class StoredBettamindCredential(
    val algorithm: String,
    val salt: ByteArray,
    val verifier: ByteArray,
) {
    init {
        require(algorithm.isNotBlank()) { "Credential KDF algorithm cannot be blank." }
        require(salt.isNotEmpty()) { "Credential salt cannot be empty." }
        require(verifier.isNotEmpty()) { "Credential verifier cannot be empty." }
    }
}

interface PasswordBasedKeyDeriver {
    val algorithm: String
    fun derive(secret: CharArray, salt: ByteArray, outputBytes: Int): ByteArray
}

sealed class CredentialVerificationResult {
    data object Accepted : CredentialVerificationResult()
    data object Incorrect : CredentialVerificationResult()
    data class RateLimited(val retryAfterEpochMillis: Long) : CredentialVerificationResult()
}

class PinAttemptRateLimiter(
    private val nowEpochMillis: () -> Long,
) {
    private var consecutiveFailures: Int = 0
    private var lockedUntilEpochMillis: Long? = null

    fun currentLockout(): Long? =
        lockedUntilEpochMillis?.takeIf { nowEpochMillis() < it }

    fun recordAccepted() {
        consecutiveFailures = 0
        lockedUntilEpochMillis = null
    }

    fun recordFailure(): Long? {
        consecutiveFailures += 1
        val delay = when {
            consecutiveFailures >= 7 -> 5 * 60_000L
            consecutiveFailures >= 5 -> 60_000L
            consecutiveFailures >= 3 -> 30_000L
            else -> null
        }
        lockedUntilEpochMillis = delay?.let { nowEpochMillis() + it }
        return lockedUntilEpochMillis
    }
}

class BettamindCredentialVerifier(
    private val storedCredential: StoredBettamindCredential,
    private val deriver: PasswordBasedKeyDeriver,
    private val rateLimiter: PinAttemptRateLimiter,
) {
    init {
        require(storedCredential.algorithm == deriver.algorithm) {
            "Stored credential KDF does not match the configured KDF."
        }
    }

    fun verify(secret: CharArray): CredentialVerificationResult {
        rateLimiter.currentLockout()?.let { retryAt ->
            return CredentialVerificationResult.RateLimited(retryAt)
        }

        val derived = deriver.derive(secret, storedCredential.salt, storedCredential.verifier.size)
        return try {
            if (constantTimeEquals(derived, storedCredential.verifier)) {
                rateLimiter.recordAccepted()
                CredentialVerificationResult.Accepted
            } else {
                val lockout = rateLimiter.recordFailure()
                if (lockout == null) {
                    CredentialVerificationResult.Incorrect
                } else {
                    CredentialVerificationResult.RateLimited(lockout)
                }
            }
        } finally {
            derived.fill(0)
        }
    }

    private fun constantTimeEquals(left: ByteArray, right: ByteArray): Boolean {
        var diff = left.size xor right.size
        val max = maxOf(left.size, right.size)
        for (index in 0 until max) {
            val leftValue = left.getOrNull(index)?.toInt() ?: 0
            val rightValue = right.getOrNull(index)?.toInt() ?: 0
            diff = diff or (leftValue xor rightValue)
        }
        return diff == 0
    }
}

enum class RecoveryPath {
    ReplaceForgottenPinAfterPlatformAuthentication,
    UseAvailableFallback,
    ExplainDataCannotBeRecoveredAndOfferLocalReset,
}

object PrivacyLockRecoveryPolicy {
    fun pathFor(
        pinForgotten: Boolean,
        platformAuthenticationAvailable: Boolean,
        platformAuthenticationSucceeded: Boolean,
        fallbackAvailable: Boolean,
    ): RecoveryPath =
        when {
            pinForgotten && platformAuthenticationAvailable && platformAuthenticationSucceeded ->
                RecoveryPath.ReplaceForgottenPinAfterPlatformAuthentication

            fallbackAvailable -> RecoveryPath.UseAvailableFallback
            else -> RecoveryPath.ExplainDataCannotBeRecoveredAndOfferLocalReset
        }
}

data class BackgroundPrivacyPolicy(
    val protectsRecentAppPreview: Boolean = true,
    val concealsSensitiveScreensWhenBackgrounded: Boolean = true,
)

enum class NotificationPrivacyPreview {
    NeutralPrivateReminder,
}

object NotificationPrivacyPolicy {
    fun neutralPreview(): NotificationPrivacyPreview =
        NotificationPrivacyPreview.NeutralPrivateReminder

    fun allowsPersonalDetailsOnLockScreen(): Boolean = false
}
