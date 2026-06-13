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

iOS SQLCipher database linking still requires a native SQLCipher build or
package integration on macOS. The SQLCipher source tree checked during this
phase did not expose a simple `Package.swift` or podspec at the repository root,
so the iOS SQLCipher storage adapter must be completed and validated through
Codemagic once the native dependency route is selected.

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
Kotlin/Native `iosSimulatorArm64` compilation verifies the iOS Keychain adapter
source on Windows. Xcode validation and the future iOS SQLCipher native link
must run on Codemagic.
