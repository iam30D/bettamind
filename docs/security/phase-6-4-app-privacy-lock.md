# Phase 6.4 App Privacy Lock

## Scope

Phase 6.4 adds the application privacy-lock foundation and secure vault-key
release boundary. It does not add Phase 6.5 relational boundaries, Phase 6.6
daily tools, sync, cloud recovery, AI response modes or unencrypted fallback
storage.

## Implemented controls

- `VaultKeyReleaseService` gates `StorageKeyManager.loadOrCreateDatabaseKey()`
  behind local authentication before encrypted vault key material is released.
- The default lock timeout is one minute, with immediate, five-minute,
  fifteen-minute and disabled app-lock settings represented in shared code and
  Settings UI copy.
- Step-up authentication is always required for exporting private information,
  enabling sync, sharing with a professional, viewing recovery data, changing
  security settings, changing or disabling the Bettamind PIN, deleting all
  local data and accessing highly sensitive records.
- Android has a `BiometricPrompt` adapter that supports strong biometrics and
  device credential fallback without requiring biometrics.
- iOS has a `LocalAuthentication` adapter that supports device-owner
  authentication and biometric-only policy where available.
- Android host app sets `FLAG_SECURE` to protect recent-app previews and
  screenshots.
- iOS host app uses SwiftUI privacy sensitivity and covers app content with a
  neutral system-background shield while inactive.
- Bettamind PIN/passphrase handling is represented by shared policy,
  credential-verifier and rate-limiter code. Plaintext PINs are not stored.
- Incorrect PIN/passphrase attempts are rate-limited with increasing delays.
- Recovery policy supports secure PIN replacement after successful platform
  authentication, approved fallback use and local reset when all
  authentication methods are unavailable.
- Notification privacy policy exposes only a neutral private-reminder preview.

## Key-release model

The required flow is:

1. local user authentication succeeds;
2. `VaultKeyReleaseService` receives the success result;
3. only then does the service call the platform `StorageKeyManager`;
4. the caller receives `StorageKeyMaterial` for SQLCipher use.

Authentication cancellation, failure, lockout, rate limiting or unavailable
auth methods return denied results and do not touch the storage key manager.

## PIN and passphrase boundary

The locked specification lists Argon2id as the approved password-based KDF.
This phase therefore adds the `PasswordBasedKeyDeriver` interface and requires
stored credential records to declare the active KDF algorithm. Tests use a
fake `Argon2id` deriver only to prove verifier and rate-limiter behavior.

Do not substitute a home-grown SHA-256 or PBKDF fallback for production PIN
storage. A later release-hardening pass must provide an audited Argon2id
implementation for Android and iOS before Bettamind PIN/passphrase setup is
enabled for production.

## Tests

Common tests cover:

- vault key not released before authentication;
- biometric success;
- biometric cancellation;
- biometric failure;
- biometric lockout;
- device credential fallback;
- correct PIN;
- incorrect PIN;
- attempt rate limiting;
- app backgrounding;
- process-death lock reset;
- step-up authentication for sensitive actions;
- forgotten PIN recovery;
- complete authentication loss and local reset policy;
- recent-app preview policy;
- neutral notification privacy.

## Remaining validation

Windows cannot compile or run the iOS LocalAuthentication path because iOS
targets with SQLCipher cinterop are disabled on `mingw_x64`. Codemagic
`ios-simulator-unsigned` is required for the pushed Phase 6.4 commit.
