package com.whatsappvault.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val sender: String,
    val message: String,
    val timestamp: Long,          // when we captured it
    val whatsappTimestamp: Long,  // timestamp from notification

    // Image support
    val imageUri: String? = null,         // URI of original image if captured
    val imageCopyPath: String? = null,    // Our saved copy path

    val isGroup: Boolean = false,
    val groupName: String? = null,

    val isDeleted: Boolean = false,       // flagged when we detect deletion
    val deletedAt: Long? = null
)
