package com.whatsappvault.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): LiveData<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE isDeleted = 1 ORDER BY timestamp DESC")
    fun getDeletedMessages(): LiveData<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE sender = :sender ORDER BY timestamp DESC")
    fun getMessagesBySender(sender: String): LiveData<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages 
        WHERE sender = :sender AND whatsappTimestamp = :timestamp
        LIMIT 1
    """)
    suspend fun findMessage(sender: String, timestamp: Long): MessageEntity?

    @Query("UPDATE messages SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun markAsDeleted(id: Long, deletedAt: Long)

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: Long): MessageEntity?

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM messages WHERE isDeleted = 1")
    suspend fun getDeletedCount(): Int

    // Search
    @Query("""
        SELECT * FROM messages 
        WHERE message LIKE '%' || :query || '%' 
           OR sender LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchMessages(query: String): LiveData<List<MessageEntity>>
}
