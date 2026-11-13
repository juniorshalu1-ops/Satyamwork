package com.example.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptedBackupManager {

    private const val SECRET_SEED = "ShikshakSathiBiharSecureBackup2026@Key"
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private val IV = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10)

    private fun getSecretKey(): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(SECRET_SEED.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptData(plainText: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = getSecretKey()
        val ivSpec = IvParameterSpec(IV)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }

    fun decryptData(cipherText: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = getSecretKey()
        val ivSpec = IvParameterSpec(IV)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val decodedBytes = Base64.decode(cipherText, Base64.DEFAULT)
        val decryptedBytes = cipher.doFinal(decodedBytes)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    fun createEncryptedBackupFile(context: Context, jsonContent: String): File {
        val encryptedData = encryptData(jsonContent)
        val backupDir = File(context.cacheDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        val backupFile = File(backupDir, "Shikshak_Sathi_Secure_Backup_${System.currentTimeMillis()}.ssb")
        backupFile.writeText(encryptedData, StandardCharsets.UTF_8)
        return backupFile
    }

    fun shareBackupToDriveOrDevice(context: Context, backupFile: File) {
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "शिक्षक साथी - सुरक्षित डेटा बैकअप (.ssb)")
            putExtra(Intent.EXTRA_TEXT, "शिक्षक साथी का सुरक्षित एन्क्रिप्टेड बैकअप। इसे Google Drive या सुरक्षित स्थानीय स्टोरेज में सहेजें।")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Google Drive या डिवाइस में बैकअप सहेजें")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
