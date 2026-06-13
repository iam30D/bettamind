package org.bettamind.shared.privacy

import cnames.structs.sqlite3
import cnames.structs.sqlite3_stmt
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import org.bettamind.shared.privacy.sqlcipher.SQLITE_DONE
import org.bettamind.shared.privacy.sqlcipher.SQLITE_OK
import org.bettamind.shared.privacy.sqlcipher.SQLITE_OPEN_CREATE
import org.bettamind.shared.privacy.sqlcipher.SQLITE_OPEN_FULLMUTEX
import org.bettamind.shared.privacy.sqlcipher.SQLITE_OPEN_READWRITE
import org.bettamind.shared.privacy.sqlcipher.SQLITE_ROW
import org.bettamind.shared.privacy.sqlcipher.bettamind_sqlcipher_transient
import org.bettamind.shared.privacy.sqlcipher.sqlite3_bind_blob
import org.bettamind.shared.privacy.sqlcipher.sqlite3_bind_int64
import org.bettamind.shared.privacy.sqlcipher.sqlite3_bind_text
import org.bettamind.shared.privacy.sqlcipher.sqlite3_close_v2
import org.bettamind.shared.privacy.sqlcipher.sqlite3_column_blob
import org.bettamind.shared.privacy.sqlcipher.sqlite3_column_bytes
import org.bettamind.shared.privacy.sqlcipher.sqlite3_column_text
import org.bettamind.shared.privacy.sqlcipher.sqlite3_exec
import org.bettamind.shared.privacy.sqlcipher.sqlite3_finalize
import org.bettamind.shared.privacy.sqlcipher.sqlite3_free
import org.bettamind.shared.privacy.sqlcipher.sqlite3_key
import org.bettamind.shared.privacy.sqlcipher.sqlite3_open_v2
import org.bettamind.shared.privacy.sqlcipher.sqlite3_prepare_v2
import org.bettamind.shared.privacy.sqlcipher.sqlite3_rekey
import org.bettamind.shared.privacy.sqlcipher.sqlite3_step
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.posix.EEXIST
import platform.posix.F_OK
import platform.posix.S_IRGRP
import platform.posix.S_IROTH
import platform.posix.S_IRUSR
import platform.posix.S_IWUSR
import platform.posix.access
import platform.posix.errno
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.fwrite
import platform.posix.getenv
import platform.posix.mkdir
import platform.posix.remove
import platform.posix.rename
import platform.posix.rewind
import platform.posix.time

@OptIn(ExperimentalForeignApi::class)
class IosSqlCipherEncryptedRecordStore(
    private val databasePath: String = defaultDatabasePath(),
) : EncryptedRecordStore {
    private var database: CPointer<sqlite3>? = null

    override fun open(key: StorageKeyMaterial) {
        close()
        assertSqlCipherAvailable()
        ensureParentDirectory(databasePath)
        excludeFromBackup(databasePath.substringBeforeLast('/'))
        database = try {
            openDatabase(databasePath, key)
        } catch (exception: EncryptedStorageException) {
            close()
            throw exception
        } catch (exception: RuntimeException) {
            close()
            throw EncryptedStorageException.WrongKey(exception)
        }
    }

    override fun put(recordKey: StorageRecordKey, value: ByteArray) {
        requireOpen().withStatement(
            """
            INSERT OR REPLACE INTO records (record_key, record_value, updated_at)
            VALUES (?, ?, ?)
            """.trimIndent(),
        ) { statement ->
            bindText(statement, 1, recordKey.value)
            bindBlob(statement, 2, value)
            requireOk(sqlite3_bind_int64(statement, 3, currentTimeMillis()), requireOpen())
            requireDone(sqlite3_step(statement), requireOpen())
        }
    }

    override fun get(recordKey: StorageRecordKey): ByteArray? {
        var result: ByteArray? = null
        requireOpen().withStatement("SELECT record_value FROM records WHERE record_key = ?") { statement ->
            bindText(statement, 1, recordKey.value)
            when (sqlite3_step(statement)) {
                SQLITE_ROW -> {
                    val byteCount = sqlite3_column_bytes(statement, 0)
                    result = if (byteCount > 0) {
                        sqlite3_column_blob(statement, 0)?.readBytes(byteCount) ?: ByteArray(0)
                    } else {
                        ByteArray(0)
                    }
                }

                SQLITE_DONE -> result = null
                else -> throw EncryptedStorageException.StoreUnavailable()
            }
        }
        return result
    }

    override fun delete(recordKey: StorageRecordKey) {
        requireOpen().withStatement("DELETE FROM records WHERE record_key = ?") { statement ->
            bindText(statement, 1, recordKey.value)
            requireDone(sqlite3_step(statement), requireOpen())
        }
    }

    override fun rotateKey(currentKey: StorageKeyMaterial, newKey: StorageKeyMaterial) {
        open(currentKey)
        val newKeyBytes = newKey.copyBytes()
        try {
            val result = newKeyBytes.usePinned { pinned ->
                sqlite3_rekey(requireOpen(), pinned.addressOf(0), newKeyBytes.size)
            }
            requireOk(result, requireOpen())
        } finally {
            newKeyBytes.fill(0)
        }
    }

    override fun exportEncryptedBackup(key: StorageKeyMaterial): EncryptedBackupPackage {
        open(key)
        close()
        if (!fileExists(databasePath)) {
            throw EncryptedStorageException.StoreUnavailable()
        }
        return EncryptedBackupPackage(formatVersion = BackupFormatVersion, ciphertext = readFile(databasePath))
    }

    override fun restoreEncryptedBackup(key: StorageKeyMaterial, backup: EncryptedBackupPackage) {
        require(backup.formatVersion == BackupFormatVersion) {
            "Unsupported encrypted backup format ${backup.formatVersion}."
        }
        close()
        assertSqlCipherAvailable()
        ensureParentDirectory(databasePath)
        val temporaryPath = "$databasePath.restore"
        writeFile(temporaryPath, backup.ciphertext)
        try {
            openDatabase(temporaryPath, key).also { sqlite3_close_v2(it) }
            deleteDatabaseFiles(databasePath)
            if (rename(temporaryPath, databasePath) != 0) {
                throw EncryptedStorageException.StoreUnavailable()
            }
            excludeFromBackup(databasePath)
            open(key)
        } catch (exception: EncryptedStorageException) {
            throw exception
        } catch (exception: RuntimeException) {
            throw EncryptedStorageException.WrongKey(exception)
        } finally {
            remove(temporaryPath)
        }
    }

    override fun deleteAll() {
        close()
        deleteDatabaseFiles(databasePath)
    }

    override fun close() {
        database?.let { sqlite3_close_v2(it) }
        database = null
    }

    private fun requireOpen(): CPointer<sqlite3> =
        database ?: throw EncryptedStorageException.StoreUnavailable()

    private fun openDatabase(path: String, key: StorageKeyMaterial): CPointer<sqlite3> = memScoped {
        val databasePointer = alloc<CPointerVar<sqlite3>>()
        val openResult = sqlite3_open_v2(
            path,
            databasePointer.ptr,
            SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE or SQLITE_OPEN_FULLMUTEX,
            null,
        )
        val opened = databasePointer.value
        if (openResult != SQLITE_OK || opened == null) {
            opened?.let { sqlite3_close_v2(it) }
            throw EncryptedStorageException.StoreUnavailable()
        }

        val keyBytes = key.copyBytes()
        try {
            val keyResult = keyBytes.usePinned { pinned ->
                sqlite3_key(opened, pinned.addressOf(0), keyBytes.size)
            }
            requireOk(keyResult, opened)
        } finally {
            keyBytes.fill(0)
        }

        try {
            execute(opened, "PRAGMA foreign_keys = ON")
            execute(opened, "PRAGMA journal_mode = DELETE")
            execute(
                opened,
                """
                CREATE TABLE IF NOT EXISTS records (
                    record_key TEXT PRIMARY KEY NOT NULL,
                    record_value BLOB NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            opened.withStatement("SELECT count(*) FROM records") { statement ->
                require(sqlite3_step(statement) == SQLITE_ROW)
            }
            opened
        } catch (exception: RuntimeException) {
            sqlite3_close_v2(opened)
            throw EncryptedStorageException.WrongKey(exception)
        }
    }

    private fun bindText(statement: CPointer<sqlite3_stmt>, index: Int, value: String) {
        requireOk(
            sqlite3_bind_text(
                statement,
                index,
                value,
                -1,
                bettamind_sqlcipher_transient(),
            ),
            requireOpen(),
        )
    }

    private fun bindBlob(statement: CPointer<sqlite3_stmt>, index: Int, value: ByteArray) {
        val result = if (value.isEmpty()) {
            sqlite3_bind_blob(
                statement,
                index,
                null,
                0,
                bettamind_sqlcipher_transient(),
            )
        } else {
            value.usePinned { pinned ->
                sqlite3_bind_blob(
                    statement,
                    index,
                    pinned.addressOf(0),
                    value.size,
                    bettamind_sqlcipher_transient(),
                )
            }
        }
        requireOk(result, requireOpen())
    }

    companion object {
        const val DefaultDatabaseName = "bettamind-phase3-vault.db"
        const val BackupFormatVersion = 1

        fun assertSqlCipherAvailable() {
            memScoped {
                val databasePointer = alloc<CPointerVar<sqlite3>>()
                val openResult = sqlite3_open_v2(
                    ":memory:",
                    databasePointer.ptr,
                    SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE or SQLITE_OPEN_FULLMUTEX,
                    null,
                )
                val opened = databasePointer.value
                    ?: throw EncryptedStorageException.StoreUnavailable()
                try {
                    requireOk(openResult, opened)
                    val probeKey = ByteArray(32)
                    try {
                        val keyResult = probeKey.usePinned { pinned ->
                            sqlite3_key(opened, pinned.addressOf(0), probeKey.size)
                        }
                        requireOk(keyResult, opened)
                    } finally {
                        probeKey.fill(0)
                    }

                    val version = opened.queryText("PRAGMA cipher_version")
                    if (version.isNullOrBlank()) {
                        throw EncryptedStorageException.StoreUnavailable()
                    }
                } finally {
                    sqlite3_close_v2(opened)
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<sqlite3>.withStatement(
    sql: String,
    block: (CPointer<sqlite3_stmt>) -> Unit,
) = memScoped {
    val statementPointer = alloc<CPointerVar<sqlite3_stmt>>()
    requireOk(sqlite3_prepare_v2(this@withStatement, sql, -1, statementPointer.ptr, null), this@withStatement)
    val statement = statementPointer.value ?: throw EncryptedStorageException.StoreUnavailable()
    try {
        block(statement)
    } finally {
        sqlite3_finalize(statement)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<sqlite3>.queryText(sql: String): String? {
    var result: String? = null
    withStatement(sql) { statement ->
        when (sqlite3_step(statement)) {
            SQLITE_ROW -> result = sqlite3_column_text(statement, 0)?.reinterpret<ByteVar>()?.toKString()
            SQLITE_DONE -> result = null
            else -> throw EncryptedStorageException.StoreUnavailable()
        }
    }
    return result
}

@OptIn(ExperimentalForeignApi::class)
private fun execute(database: CPointer<sqlite3>, sql: String) = memScoped {
    val errorMessage = alloc<CPointerVar<ByteVar>>()
    val result = sqlite3_exec(database, sql, null, null, errorMessage.ptr)
    if (result != SQLITE_OK) {
        errorMessage.value?.let { sqlite3_free(it) }
        throw EncryptedStorageException.StoreUnavailable()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun requireOk(result: Int, database: CPointer<sqlite3>) {
    if (result != SQLITE_OK) {
        throw EncryptedStorageException.StoreUnavailable()
    }
}

private fun requireDone(result: Int, database: CPointer<sqlite3>) {
    if (result != SQLITE_DONE) {
        throw EncryptedStorageException.StoreUnavailable()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun currentTimeMillis(): Long = time(null) * 1000L

@OptIn(ExperimentalForeignApi::class)
private fun defaultDatabasePath(): String {
    val home = getenv("HOME")?.toKString()
        ?: throw EncryptedStorageException.StoreUnavailable()
    return "$home/Library/Application Support/Bettamind/$IosDefaultDatabaseName"
}

private fun ensureParentDirectory(path: String) {
    val directory = path.substringBeforeLast('/', missingDelimiterValue = "")
    if (directory.isNotBlank()) {
        ensureDirectory(directory)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ensureDirectory(directory: String) {
    var current = if (directory.startsWith("/")) "/" else ""
    directory.split('/').filter { it.isNotBlank() }.forEach { segment ->
        current = when (current) {
            "" -> segment
            "/" -> "/$segment"
            else -> "$current/$segment"
        }
        if (mkdir(current, DirectoryMode.convert()) != 0 && errno != EEXIST) {
            throw EncryptedStorageException.StoreUnavailable()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun excludeFromBackup(path: String) {
    NSURL.fileURLWithPath(path)
        .setResourceValue(NSNumber.numberWithBool(true), forKey = NSURLIsExcludedFromBackupKey, error = null)
}

@OptIn(ExperimentalForeignApi::class)
private fun fileExists(path: String): Boolean = access(path, F_OK) == 0

@OptIn(ExperimentalForeignApi::class)
private fun readFile(path: String): ByteArray {
    val file = fopen(path, "rb") ?: throw EncryptedStorageException.StoreUnavailable()
    try {
        if (fseek(file, 0, 2) != 0) throw EncryptedStorageException.StoreUnavailable()
        val size = ftell(file)
        if (size < 0) throw EncryptedStorageException.StoreUnavailable()
        rewind(file)
        val output = ByteArray(size.toInt())
        if (output.isNotEmpty()) {
            output.usePinned { pinned ->
                val read = fread(pinned.addressOf(0), 1.convert(), output.size.convert(), file)
                if (read != output.size.convert()) {
                    throw EncryptedStorageException.StoreUnavailable()
                }
            }
        }
        return output
    } finally {
        fclose(file)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun writeFile(path: String, bytes: ByteArray) {
    val file = fopen(path, "wb") ?: throw EncryptedStorageException.StoreUnavailable()
    try {
        if (bytes.isNotEmpty()) {
            bytes.usePinned { pinned ->
                val written = fwrite(pinned.addressOf(0), 1.convert(), bytes.size.convert(), file)
                if (written != bytes.size.convert()) {
                    throw EncryptedStorageException.StoreUnavailable()
                }
            }
        }
    } finally {
        fclose(file)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun deleteDatabaseFiles(path: String) {
    remove(path)
    listOf("-journal", "-shm", "-wal").forEach { suffix -> remove(path + suffix) }
}

private const val IosDefaultDatabaseName = "bettamind-phase3-vault.db"
private val DirectoryMode = S_IRUSR or S_IWUSR or S_IRGRP or S_IROTH
