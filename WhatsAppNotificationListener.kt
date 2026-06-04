package com.whatsappvault.service

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.whatsappvault.database.AppDatabase
import com.whatsappvault.database.MessageEntity
import com.whatsappvault.utils.ImageSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class WhatsAppNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "WA_Vault"
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var db: AppDatabase
    private lateinit var imageSaver: ImageSaver

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(applicationContext)
        imageSaver = ImageSaver(applicationContext)
        Log.d(TAG, "Notification Listener Service started")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        // Only process WhatsApp notifications
        if (packageName != WHATSAPP_PACKAGE && packageName != WHATSAPP_BUSINESS_PACKAGE) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Extract message content
        val title = extras.getString(Notification.EXTRA_TITLE) ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val timestamp = sbn.postTime

        // Skip system/group summary notifications
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (text.isBlank() && subText == null) return

        // Detect group messages
        val isGroup = subText != null && title != subText
        val groupName = if (isGroup) title else null
        val sender = if (isGroup) {
            // In groups: title = group name, text starts with "Sender: message"
            text.substringBefore(":").trim().takeIf { it.isNotEmpty() } ?: title
        } else {
            title
        }
        val messageText = if (isGroup) {
            text.substringAfter(":").trim()
        } else {
            text
        }

        // Try to extract image from notification
        val largeIcon = extras.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON)
        val bigPicture = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_PICTURE, Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(Notification.EXTRA_PICTURE)
        }

        scope.launch {
            try {
                // Save image if present
                var imageCopyPath: String? = null
                val bitmap = bigPicture ?: largeIcon
                if (bigPicture != null) {
                    // bigPicture = actual image content shared in message
                    imageCopyPath = imageSaver.saveBitmap(
                        bitmap = bigPicture,
                        sender = sender,
                        timestamp = timestamp
                    )
                }

                val entity = MessageEntity(
                    sender = sender,
                    message = messageText,
                    timestamp = System.currentTimeMillis(),
                    whatsappTimestamp = timestamp,
                    imageCopyPath = imageCopyPath,
                    isGroup = isGroup,
                    groupName = groupName
                )

                val insertedId = db.messageDao().insertMessage(entity)
                Log.d(TAG, "Saved message #$insertedId from $sender: $messageText")

            } catch (e: Exception) {
                Log.e(TAG, "Error saving message", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // When a WhatsApp notification is dismissed, it MIGHT mean a message was deleted
        // We keep all captured messages regardless — the user can see them all
        val packageName = sbn.packageName
        if (packageName != WHATSAPP_PACKAGE && packageName != WHATSAPP_BUSINESS_PACKAGE) return
        Log.d(TAG, "WhatsApp notification removed (message may have been deleted)")
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
