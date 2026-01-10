package com.company.primus2.memory.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.company.primus2.memory.db.entities.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun get(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatest(): SessionEntity?

    // ★ 追加：全削除（messages→sessions の順で呼ぶ想定）
    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}
