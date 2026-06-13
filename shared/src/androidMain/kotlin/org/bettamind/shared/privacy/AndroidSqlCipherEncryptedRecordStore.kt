package org.bettamind.shared.privacy

import android.content.Context
import android.database.Cursor
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File

class AndroidSqlCipherEncryptedRecordStore(
    context: Context,
    private val databaseName: String = DefaultDatabaseName,
) : EncryptedRecordStore {
    private val databaseFile = File(context.noBackupFilesDir, databaseName)
    private var database: SQLiteDatabase? = null

    override fun open(key: StorageKeyMaterial) {
        close()
        assertSqlCipherAvailable()
        try {
            databaseFile.parentFile?.mkdirs()
            database = openDatabase(databaseFile, key)
        } catch (exception: EncryptedStorageException) {
            close()
            throw exception
        } catch (exception: RuntimeException) {
            close()
            throw EncryptedStorageException.WrongKey(exception)
        }
    }

    override fun put(recordKey: StorageRecordKey, value: ByteArray) {
        requireOpen().execSQL(
            """
            INSERT OR REPLACE INTO records (record_key, record_value, updated_at)
            VALUES (?, ?, ?)
            """.trimIndent(),
            arrayOf(recordKey.value, value.copyOf(), System.currentTimeMillis()),
        )
    }

    override fun get(recordKey: StorageRecordKey): ByteArray? =
        requireOpen()
            .rawQuery("SELECT record_value FROM records WHERE record_key = ?", arrayOf(recordKey.value))
            .useCursor { cursor ->
                if (cursor.moveToFirst()) cursor.getBlob(0).copyOf() else null
            }

    override fun delete(recordKey: StorageRecordKey) {
        requireOpen().execSQL("DELETE FROM records WHERE record_key = ?", arrayOf(recordKey.value))
    }

    override fun rotateKey(currentKey: StorageKeyMaterial, newKey: StorageKeyMaterial) {
        open(currentKey)
        requireOpen().changePassword(newKey.copyBytes())
    }

    override fun exportEncryptedBackup(key: StorageKeyMaterial): EncryptedBackupPackage {
        open(key)
        close()
        if (!databaseFile.exists()) {
            throw EncryptedStorageException.StoreUnavailable()
        }
        return EncryptedBackupPackage(formatVersion = BackupFormatVersion, ciphertext = databaseFile.readBytes())
    }

    override fun restoreEncryptedBackup(key: StorageKeyMaterial, backup: EncryptedBackupPackage) {
        require(backup.formatVersion == BackupFormatVersion) {
            "Unsupported encrypted backup format ${backup.formatVersion}."
        }
        close()
        assertSqlCipherAvailable()
        databaseFile.parentFile?.mkdirs()
        val temporaryFile = File(databaseFile.parentFile, "$databaseName.restore")
        temporaryFile.writeBytes(backup.ciphertext)
        try {
            openDatabase(temporaryFile, key).close()
            deleteDatabaseFiles(databaseFile)
            temporaryFile.copyTo(databaseFile, overwrite = true)
            open(key)
        } catch (exception: EncryptedStorageException) {
            throw exception
        } catch (exception: RuntimeException) {
            throw EncryptedStorageException.WrongKey(exception)
        } finally {
            temporaryFile.delete()
        }
    }

    override fun deleteAll() {
        close()
        deleteDatabaseFiles(databaseFile)
    }

    override fun close() {
        database?.close()
        database = null
    }

    private fun requireOpen(): SQLiteDatabase =
        database ?: throw EncryptedStorageException.StoreUnavailable()

    private fun openDatabase(file: File, key: StorageKeyMaterial): SQLiteDatabase {
        val keyBytes = key.copyBytes()
        return try {
            SQLiteDatabase.openOrCreateDatabase(
                file,
                keyBytes,
                null,
                null,
            ).also { db ->
                db.execSQL("PRAGMA foreign_keys = ON")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS records (
                        record_key TEXT PRIMARY KEY NOT NULL,
                        record_value BLOB NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.rawQuery("SELECT count(*) FROM records", emptyArray()).useCursor { it.moveToFirst() }
            }
        } finally {
            keyBytes.fill(0)
        }
    }

    private fun deleteDatabaseFiles(file: File) {
        SQLiteDatabase.deleteDatabase(file)
        listOf("-journal", "-shm", "-wal").forEach { suffix ->
            File(file.absolutePath + suffix).delete()
        }
    }

    companion object {
        const val DefaultDatabaseName = "bettamind-phase3-vault.db"
        const val BackupFormatVersion = 1

        fun assertSqlCipherAvailable() {
            if (!SQLiteDatabase.hasCodec()) {
                throw EncryptedStorageException.StoreUnavailable()
            }
        }
    }
}

private inline fun <T> Cursor.useCursor(block: (Cursor) -> T): T =
    use { cursor -> block(cursor) }
