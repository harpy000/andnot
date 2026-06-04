package com.whatsappvault.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ImageSaver(private val context: Context) {

    companion object {
        private const val TAG = "ImageSaver"
        private const val VAULT_DIR = "WhatsAppVault/Images"
    }

    private fun getVaultDir(): File {
        val dir = File(context.filesDir, VAULT_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Save a Bitmap (from notification) to vault.
     */
    fun saveBitmap(bitmap: Bitmap, sender: String, timestamp: Long): String? {
        return try {
            val fileName = "IMG_${timestamp}_${sanitize(sender)}.jpg"
            val file = File(getVaultDir(), fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            Log.d(TAG, "Saved bitmap to ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap", e)
            null
        }
    }

    /**
     * Copy an image file from WhatsApp folder to vault.
     */
    fun saveFromFile(sourceFile: File): String? {
        return try {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "IMG_${sdf.format(Date())}_${sourceFile.name}"
            val destFile = File(getVaultDir(), fileName)

            sourceFile.copyTo(destFile, overwrite = true)

            Log.d(TAG, "Copied image to ${destFile.absolutePath}")
            destFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error copying file", e)
            null
        }
    }

    /**
     * Load a saved image as Bitmap.
     */
    fun loadBitmap(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from $path", e)
            null
        }
    }

    /**
     * List all saved images in vault.
     */
    fun listVaultImages(): List<File> {
        return getVaultDir().listFiles()
            ?.filter { it.isFile && it.length() > 0 }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * Delete a saved image.
     */
    fun deleteImage(path: String): Boolean {
        return File(path).delete()
    }

    private fun sanitize(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(20)
    }
}
