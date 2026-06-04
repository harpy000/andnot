package com.whatsappvault.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.whatsappvault.database.AppDatabase
import com.whatsappvault.database.MessageEntity
import com.whatsappvault.utils.ImageSaver
import kotlinx.coroutines.*
import java.io.File

/**
 * Watches WhatsApp's Images folder using FileObserver.
 * When a new image appears (downloaded/received), we copy it immediately
 * so it's preserved even if the sender deletes it.
 */
class ImageObserverService : Service() {

    companion object {
        private const val TAG = "WA_ImageObserver"
        private const val CHANNEL_ID = "vault_service_channel"
        private const val NOTIF_ID = 1001

        // WhatsApp media paths (varies by Android version)
        private val WA_IMAGE_PATHS = listOf(
            "/storage/emulated/0/WhatsApp/Media/WhatsApp Images/",
            "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/",
            "/sdcard/WhatsApp/Media/WhatsApp Images/"
        )
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var db: AppDatabase
    private lateinit var imageSaver: ImageSaver
    private val observers = mutableListOf<FileObserver>()

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(applicationContext)
        imageSaver = ImageSaver(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildForegroundNotification())
        startWatchingImageFolders()
        return START_STICKY
    }

    private fun startWatchingImageFolders() {
        observers.forEach { it.stopWatching() }
        observers.clear()

        for (path in WA_IMAGE_PATHS) {
            val dir = File(path)
            if (!dir.exists()) continue

            Log.d(TAG, "Watching: $path")

            val observer = object : FileObserver(path, CREATE or CLOSE_WRITE or MOVED_TO) {
                override fun onEvent(event: Int, relativePath: String?) {
                    if (relativePath == null) return
                    if (!relativePath.endsWith(".jpg", true) &&
                        !relativePath.endsWith(".jpeg", true) &&
                        !relativePath.endsWith(".png", true) &&
                        !relativePath.endsWith(".webp", true)) return

                    val fullPath = path + relativePath
                    Log.d(TAG, "New WhatsApp image detected: $fullPath")

                    scope.launch {
                        delay(500) // brief wait for file to finish writing
                        saveImageToVault(fullPath)
                    }
                }
            }
            observer.startWatching()
            observers.add(observer)
        }

        if (observers.isEmpty()) {
            Log.w(TAG, "No WhatsApp image folders found. Storage permission may be needed.")
        }
    }

    private suspend fun saveImageToVault(originalPath: String) {
        try {
            val file = File(originalPath)
            if (!file.exists() || file.length() == 0L) return

            val savedPath = imageSaver.saveFromFile(file)
            if (savedPath != null) {
                // Save a record in DB
                val entity = MessageEntity(
                    sender = "Unknown (image)",
                    message = "[Image from WhatsApp]",
                    timestamp = System.currentTimeMillis(),
                    whatsappTimestamp = file.lastModified(),
                    imageCopyPath = savedPath,
                    imageUri = originalPath
                )
                db.messageDao().insertMessage(entity)
                Log.d(TAG, "Saved image to vault: $savedPath")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WhatsApp Vault Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running in background to capture messages"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WhatsApp Vault")
            .setContentText("Monitoring WhatsApp images...")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        observers.forEach { it.stopWatching() }
        job.cancel()
    }
}
