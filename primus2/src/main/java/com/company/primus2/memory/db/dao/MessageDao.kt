package com.company.primus2.memory.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.company.primus2.memory.db.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun observeBySession(sessionId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun listBySession(sessionId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages ORDER BY createdAt DESC LIMIT :limit")
    suspend fun latest(limit: Int): List<MessageEntity>

    @Query("SELECT * FROM messages ORDER BY sessionId, createdAt ASC")
    suspend fun getAllMessages(): List<MessageEntity>

    // ★ 追加：全削除（FKの都合でセッションより先に消すことを想定）
    @Query("DELETE FROM messages")
    suspend fun deleteAll()
}
