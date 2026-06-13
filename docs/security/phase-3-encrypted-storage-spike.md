# Phase 3 Encrypted Storage Spike

## Scope

Phase 3 introduces the encrypted-storage technical spike only. It does not wire
personal product data, finished app flows, sync, exports to other apps, AI or
backend storage.

## Shared contract

`shared/src/commonMain/kotlin/org/bettamind/shared/privacy/EncryptedStorage.kt`
defines:

- `StorageKeyMaterial`
- `StorageRecordKey`
- `EncryptedBackupPackage`
- `EncryptedRecordStore`
- `StorageKeyManager`
- explicit `EncryptedStorageException` failures

The contract requires a key before use, supports record put/get/delete,
wrong-key rejection, key rotation, encrypted backup, restore and deletion.

## Android proof

Android uses:

- `net.zetetic:sqlcipher-android:4.16.0`
- `androidx.sqlite:sqlite:2.6.2`
- `AndroidSqlCipherEncryptedRecordStore`
- `AndroidKeystoreStorageKeyManager`

Database files are stored under `Context.noBackupFilesDir`. The SQLCipher key
is a random 32-byte database key. That key is wrapped with an Android Keystore
AES-GCM key and stored only as ciphertext in `noBackupFilesDir`.

`AndroidSqlCipherEncryptedRecordStore` opens only through SQLCipher and has no
standard SQLite fallback path. Backup packages are raw SQLCipher database bytes,
so exported backups remain ciphertext. Restores verify the backup with
SQLCipher before replacing the current database.

## iOS proof boundary

`IosKeychainStorageKeyManager` adds the iOS Keychain adapter source for storing
the SQLCipher database key with `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`.
It fails closed with OSStatus details when Security.framework operations fail
and does not write key material to fallback storage.

The selected iOS SQLCipher route is the official `SQLCipher.swift` Swift Package
pinned to `4.16.0`, matching the Android SQLCipher dependency. The Xcode project
links the `SQLCipher` package product for the app build. Gradle downloads the
same pinned `SQLCipher.xcframework.zip`, verifies its SHA-256 checksum, and uses
the framework headers for Kotlin/Native cinterop.

`IosSqlCipherEncryptedRecordStore` opens only through SQLCipher C APIs exposed
by the package. It verifies `PRAGMA cipher_version`, uses `sqlite3_key` before
schema access, rotates keys with `sqlite3_rekey`, stores the database under
Application Support with backup exclusion, and exports/restores raw SQLCipher
database bytes as ciphertext.

Because Kotlin/Native cinterop for iOS requires Apple's native toolchain, Windows
cannot compile or execute the iOS SQLCipher adapter. Codemagic macOS validation
is the required proof for this part of Phase 3.

The real Keychain proof must run from an app-hosted iOS simulator process. The
standalone Kotlin/Native simulator test binary remains useful for SQLCipher
database behaviour, but it is not treated as the Keychain authority because it
does not have the same app-hosted Keychain context.

The Codemagic workflow still performs the required unsigned simulator
`xcodebuild` build. For the Keychain validation only, it performs a separate
simulator build with ad-hoc local signing and
`iosApp/StorageValidation.entitlements`, then installs that validation build
into the simulator. The app-hosted validation reads the signed
`keychain-access-groups` value from the validation app and passes it explicitly
to the iOS Keychain adapter. This does not introduce Apple release signing
credentials and does not change the unsigned build artifact.

Do not substitute system SQLite as an unencrypted fallback.

## Tests

Common tests cover the encrypted-storage contract with a test-only in-memory
fake:

- wrong-key rejection;
- key rotation;
- encrypted backup and restore;
- deletion.

The fake exists only in `commonTest` and is not production storage.

Android compilation verifies the SQLCipher and Keystore adapter APIs on Windows.
After the iOS SQLCipher cinterop was added, Windows skips the iOS Native targets
because cinterop cross-compilation is unsupported on `mingw_x64`. Codemagic
`ios-simulator-unsigned` must run `:shared:iosSimulatorArm64Test`,
compile all iOS targets, build the unsigned simulator app with `xcodebuild`,
install the simulator app and launch it with
`BETTAMIND_IOS_STORAGE_VALIDATION=1`. The app-hosted validation writes
`bettamind-ios-storage-validation.txt` and the workflow requires a `PASS:`
result.
