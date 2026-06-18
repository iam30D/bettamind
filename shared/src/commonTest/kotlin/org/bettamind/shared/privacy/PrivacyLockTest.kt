package org.bettamind.shared.privacy

import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PrivacyLockTest {
    @Test
    fun vaultKeyRemainsUnavailableBeforeAuthentication() = runTest {
        val keyManager = FakeStorageKeyManager()
        val service = VaultKeyReleaseService(
            keyManager = keyManager,
            authenticator = FakeAuthenticator(UserAuthenticationResult.Cancelled),
            nowEpochMillis = { 1_000L },
        )

        val result = service.releaseVaultKey(unlockRequest())

        assertIs<VaultKeyReleaseResult.Denied>(result)
        assertEquals(0, keyManager.loadCalls)
        assertFalse(service.isUnlocked())
    }

    @Test
    fun biometricSuccessReleasesVaultKey() = runTest {
        val keyManager = FakeStorageKeyManager()
        val service = VaultKeyReleaseService(
            keyManager = keyManager,
            authenticator = FakeAuthenticator(
                UserAuthenticationResult.Success(AuthenticationMethod.StrongBiometric),
            ),
            nowEpochMillis = { 2_000L },
        )

        val result = service.releaseVaultKey(unlockRequest())

        assertIs<VaultKeyReleaseResult.Released>(result)
        assertEquals(AuthenticationMethod.StrongBiometric, result.method)
        assertEquals(62_000L, result.expiresAtEpochMillis)
        assertEquals(1, keyManager.loadCalls)
        assertTrue(service.isUnlocked())
        result.key.destroy()
    }

    @Test
    fun biometricCancellationFailureAndLockoutDenyVaultKey() = runTest {
        listOf(
            UserAuthenticationResult.Cancelled,
            UserAuthenticationResult.Failed,
            UserAuthenticationResult.Lockout(retryAfterEpochMillis = 10_000L),
        ).forEach { authResult ->
            val keyManager = FakeStorageKeyManager()
            val service = VaultKeyReleaseService(
                keyManager = keyManager,
                authenticator = FakeAuthenticator(authResult),
                nowEpochMillis = { 2_000L },
            )

            val result = service.releaseVaultKey(unlockRequest())

            assertIs<VaultKeyReleaseResult.Denied>(result)
            assertEquals(authResult, result.authenticationResult)
            assertEquals(0, keyManager.loadCalls)
        }
    }

    @Test
    fun deviceCredentialFallbackCanReleaseVaultKeyWithoutBiometricRequirement() = runTest {
        val keyManager = FakeStorageKeyManager()
        val authenticator = FakeAuthenticator(
            result = UserAuthenticationResult.Success(AuthenticationMethod.DeviceCredential),
            capabilities = AuthenticationCapabilities(
                strongBiometricAvailable = false,
                deviceCredentialAvailable = true,
                bettamindPinAvailable = false,
                bettamindPassphraseAvailable = false,
            ),
        )
        val service = VaultKeyReleaseService(
            keyManager = keyManager,
            authenticator = authenticator,
            nowEpochMillis = { 5_000L },
        )

        val result = service.releaseVaultKey(unlockRequest())

        assertIs<VaultKeyReleaseResult.Released>(result)
        assertEquals(AuthenticationMethod.DeviceCredential, result.method)
        assertEquals(setOf(AuthenticationMethod.DeviceCredential), authenticator.lastRequest?.allowedMethods)
        result.key.destroy()
    }

    @Test
    fun appBackgroundingAndProcessDeathLockTheVaultAgain() = runTest {
        var now = 1_000L
        val keyManager = FakeStorageKeyManager()
        val service = VaultKeyReleaseService(
            keyManager = keyManager,
            authenticator = FakeAuthenticator(
                UserAuthenticationResult.Success(AuthenticationMethod.StrongBiometric),
            ),
            nowEpochMillis = { now },
        )

        val firstUnlock = service.releaseVaultKey(unlockRequest())
        assertIs<VaultKeyReleaseResult.Released>(firstUnlock)
        assertTrue(service.isUnlocked())

        service.onAppBackgrounded()
        assertFalse(service.isUnlocked())

        val restartedService = VaultKeyReleaseService(
            keyManager = keyManager,
            authenticator = FakeAuthenticator(
                UserAuthenticationResult.Success(AuthenticationMethod.StrongBiometric),
            ),
            nowEpochMillis = { now },
        )
        assertFalse(restartedService.isUnlocked())
        now += 61_000L
        assertFalse(restartedService.isUnlocked())
        firstUnlock.key.destroy()
    }

    @Test
    fun sensitiveActionsRequireStepUpAuthenticationEvenWhenAppLockIsDisabled() = runTest {
        val keyManager = FakeStorageKeyManager()
        val authenticator = FakeAuthenticator(
            UserAuthenticationResult.Success(AuthenticationMethod.DeviceCredential),
        )
        val service = VaultKeyReleaseService(
            keyManager = keyManager,
            authenticator = authenticator,
            settings = PrivacyLockSettings(timeout = PrivacyLockTimeout.Disabled),
            nowEpochMillis = { 1_000L },
        )

        val result = service.releaseVaultKey(
            unlockRequest(action = SensitiveAction.ExportPrivateInformation),
        )

        assertIs<VaultKeyReleaseResult.Released>(result)
        assertEquals(AuthenticationMethod.DeviceCredential, result.method)
        assertEquals(SensitiveAction.ExportPrivateInformation, authenticator.lastRequest?.action)
        result.key.destroy()
    }

    @Test
    fun correctPinIsAcceptedWithoutStoringPlaintextPin() {
        val secret = "123456".toCharArray()
        val deriver = FakeArgon2idDeriver()
        val credential = StoredBettamindCredential(
            algorithm = deriver.algorithm,
            salt = byteArrayOf(1, 2, 3),
            verifier = deriver.derive(secret, byteArrayOf(1, 2, 3), outputBytes = 32),
        )
        val verifier = BettamindCredentialVerifier(
            storedCredential = credential,
            deriver = deriver,
            rateLimiter = PinAttemptRateLimiter { 1_000L },
        )

        assertTrue(BettamindCredentialPolicy.validate(secret).accepted)
        assertEquals(CredentialVerificationResult.Accepted, verifier.verify(secret))
        assertFalse(credential.verifier.decodeToString().contains("123456"))
    }

    @Test
    fun incorrectPinAttemptsAreRateLimitedWithIncreasingDelay() {
        var now = 1_000L
        val correctSecret = "123456".toCharArray()
        val deriver = FakeArgon2idDeriver()
        val credential = StoredBettamindCredential(
            algorithm = deriver.algorithm,
            salt = byteArrayOf(3, 2, 1),
            verifier = deriver.derive(correctSecret, byteArrayOf(3, 2, 1), outputBytes = 32),
        )
        val verifier = BettamindCredentialVerifier(
            storedCredential = credential,
            deriver = deriver,
            rateLimiter = PinAttemptRateLimiter { now },
        )

        assertEquals(CredentialVerificationResult.Incorrect, verifier.verify("000000".toCharArray()))
        assertEquals(CredentialVerificationResult.Incorrect, verifier.verify("111111".toCharArray()))

        val firstLimit = verifier.verify("222222".toCharArray())
        assertIs<CredentialVerificationResult.RateLimited>(firstLimit)
        assertEquals(31_000L, firstLimit.retryAfterEpochMillis)

        now = 31_001L
        assertIs<CredentialVerificationResult.RateLimited>(verifier.verify("333333".toCharArray()))
    }

    @Test
    fun pinAndPassphrasePolicyRequiresStrongEnoughSecrets() {
        assertFalse(BettamindCredentialPolicy.validate("12345".toCharArray()).accepted)
        assertTrue(BettamindCredentialPolicy.validate("123456".toCharArray()).accepted)
        assertFalse(BettamindCredentialPolicy.validate("password".toCharArray()).accepted)
        assertTrue(BettamindCredentialPolicy.validate("pass1234".toCharArray()).accepted)
    }

    @Test
    fun forgottenPinCanBeReplacedOnlyAfterPlatformAuthentication() {
        assertEquals(
            RecoveryPath.ReplaceForgottenPinAfterPlatformAuthentication,
            PrivacyLockRecoveryPolicy.pathFor(
                pinForgotten = true,
                platformAuthenticationAvailable = true,
                platformAuthenticationSucceeded = true,
                fallbackAvailable = false,
            ),
        )
    }

    @Test
    fun completeAuthenticationLossOffersLocalResetWithoutAutomaticErase() = runTest {
        val keyManager = FakeStorageKeyManager()
        val service = VaultKeyReleaseService(
            keyManager = keyManager,
            authenticator = FakeAuthenticator(
                result = UserAuthenticationResult.Unavailable(),
                capabilities = AuthenticationCapabilities(
                    strongBiometricAvailable = false,
                    deviceCredentialAvailable = false,
                    bettamindPinAvailable = false,
                    bettamindPassphraseAvailable = false,
                ),
            ),
            nowEpochMillis = { 1_000L },
        )

        assertIs<VaultKeyReleaseResult.Denied>(service.releaseVaultKey(unlockRequest()))
        assertEquals(
            RecoveryPath.ExplainDataCannotBeRecoveredAndOfferLocalReset,
            PrivacyLockRecoveryPolicy.pathFor(
                pinForgotten = true,
                platformAuthenticationAvailable = false,
                platformAuthenticationSucceeded = false,
                fallbackAvailable = false,
            ),
        )
        assertEquals(0, keyManager.deleteCalls)
    }

    @Test
    fun backgroundAndNotificationPoliciesRevealNoPrivateContent() {
        val backgroundPolicy = BackgroundPrivacyPolicy()

        assertTrue(backgroundPolicy.protectsRecentAppPreview)
        assertTrue(backgroundPolicy.concealsSensitiveScreensWhenBackgrounded)
        assertEquals(
            NotificationPrivacyPreview.NeutralPrivateReminder,
            NotificationPrivacyPolicy.neutralPreview(),
        )
        assertFalse(NotificationPrivacyPolicy.allowsPersonalDetailsOnLockScreen())
    }

    private fun unlockRequest(action: SensitiveAction = SensitiveAction.OpenVault): VaultUnlockRequest =
        VaultUnlockRequest(
            action = action,
            title = "Unlock",
            subtitle = "Private storage",
            reason = "Authenticate to release the vault key.",
            cancelLabel = "Cancel",
        )
}

private class FakeAuthenticator(
    private val result: UserAuthenticationResult,
    private val capabilities: AuthenticationCapabilities = AuthenticationCapabilities(
        strongBiometricAvailable = true,
        deviceCredentialAvailable = true,
        bettamindPinAvailable = false,
        bettamindPassphraseAvailable = false,
    ),
) : UserAuthenticator {
    var lastRequest: UserAuthenticationRequest? = null
        private set

    override fun capabilities(): AuthenticationCapabilities = capabilities

    override suspend fun authenticate(request: UserAuthenticationRequest): UserAuthenticationResult {
        lastRequest = request
        return result
    }
}

private class FakeStorageKeyManager : StorageKeyManager {
    var loadCalls: Int = 0
        private set
    var deleteCalls: Int = 0
        private set

    override fun loadOrCreateDatabaseKey(): StorageKeyMaterial {
        loadCalls += 1
        val random = Random(64)
        return StorageKeyMaterial.fromBytes(ByteArray(32) { random.nextInt(0, 256).toByte() })
    }

    override fun replaceDatabaseKey(newKey: StorageKeyMaterial) = Unit

    override fun deleteDatabaseKey() {
        deleteCalls += 1
    }
}

private class FakeArgon2idDeriver : PasswordBasedKeyDeriver {
    override val algorithm: String = "Argon2id"

    override fun derive(secret: CharArray, salt: ByteArray, outputBytes: Int): ByteArray {
        val output = ByteArray(outputBytes)
        val secretBytes = secret.concatToString().encodeToByteArray()
        for (index in output.indices) {
            val secretByte = secretBytes[index % secretBytes.size]
            val saltByte = salt[index % salt.size]
            output[index] = (secretByte.toInt() xor saltByte.toInt() xor index).toByte()
        }
        secretBytes.fill(0)
        return output
    }
}
