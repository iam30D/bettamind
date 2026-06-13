package org.bettamind.shared.privacy

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidKeystoreStorageKeyManager(
    private val context: Context,
    private val alias: String = DefaultAlias,
) : StorageKeyManager {
    private val keyStore: KeyStore = KeyStore.getInstance(AndroidKeyStore).apply { load(null) }
    private val wrappedKeyFile: File
        get() = File(context.noBackupFilesDir, "$alias.wrapped-sqlcipher-key")

    override fun loadOrCreateDatabaseKey(): StorageKeyMaterial {
        ensureWrappingKey()
        if (wrappedKeyFile.exists()) {
            return StorageKeyMaterial.fromBytes(decryptStoredKey(wrappedKeyFile.readBytes()))
        }

        val rawKey = ByteArray(SqlCipherKeyBytes)
        SecureRandom().nextBytes(rawKey)
        return try {
            wrappedKeyFile.parentFile?.mkdirs()
            wrappedKeyFile.writeBytes(encryptKey(rawKey))
            StorageKeyMaterial.fromBytes(rawKey)
        } finally {
            rawKey.fill(0)
        }
    }

    override fun replaceDatabaseKey(newKey: StorageKeyMaterial) {
        ensureWrappingKey()
        val rawKey = newKey.copyBytes()
        wrappedKeyFile.parentFile?.mkdirs()
        try {
            wrappedKeyFile.writeBytes(encryptKey(rawKey))
        } finally {
            rawKey.fill(0)
        }
    }

    override fun deleteDatabaseKey() {
        if (wrappedKeyFile.exists()) {
            wrappedKeyFile.delete()
        }
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }

    private fun ensureWrappingKey() {
        if (keyStore.containsAlias(alias)) {
            return
        }
        generateWrappingKey(strongBox = supportsStrongBox())
    }

    private fun generateWrappingKey(strongBox: Boolean) {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore)
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }

        try {
            generator.init(builder.build())
            generator.generateKey()
        } catch (exception: StrongBoxUnavailableException) {
            if (strongBox) {
                generateWrappingKey(strongBox = false)
            } else {
                throw EncryptedStorageException.StoreUnavailable(exception)
            }
        }
    }

    private fun supportsStrongBox(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)

    private fun wrappingKey(): SecretKey =
        keyStore.getKey(alias, null) as? SecretKey
            ?: throw EncryptedStorageException.StoreUnavailable()

    private fun encryptKey(rawKey: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AesGcmTransformation)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
        val ciphertext = cipher.doFinal(rawKey)
        return byteArrayOf(WrappedKeyVersion, cipher.iv.size.toByte()) + cipher.iv + ciphertext
    }

    private fun decryptStoredKey(stored: ByteArray): ByteArray {
        if (stored.size <= WrappedKeyHeaderBytes || stored[0] != WrappedKeyVersion) {
            throw EncryptedStorageException.IntegrityCheckFailed()
        }
        val ivLength = stored[1].toInt()
        val iv = stored.copyOfRange(WrappedKeyHeaderBytes, WrappedKeyHeaderBytes + ivLength)
        val ciphertext = stored.copyOfRange(WrappedKeyHeaderBytes + ivLength, stored.size)
        val cipher = Cipher.getInstance(AesGcmTransformation)
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey(), GCMParameterSpec(GcmTagBits, iv))
        return cipher.doFinal(ciphertext)
    }

    companion object {
        const val DefaultAlias = "bettamind-phase3-sqlcipher"
        private const val AndroidKeyStore = "AndroidKeyStore"
        private const val AesGcmTransformation = "AES/GCM/NoPadding"
        private const val GcmTagBits = 128
        private const val SqlCipherKeyBytes = 32
        private const val WrappedKeyVersion: Byte = 1
        private const val WrappedKeyHeaderBytes = 2
    }
}
