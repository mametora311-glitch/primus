package com.company.primus2.memory.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.company.primus2.memory.db.GoalStatus
import com.company.primus2.memory.db.entities.GoalEntity

@Dao
interface GoalDao {
    @Upsert
    suspend fun upsert(e: GoalEntity): Long

    @Update
    suspend fun update(e: GoalEntity)

    @Query("SELECT * FROM goals ORDER BY priority DESC, createdAt DESC")
    suspend fun listAll(): List<GoalEntity>

    @Query("""
        UPDATE goals
        SET status = :status, updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun setStatus(id: Long, status: GoalStatus, updatedAt: Long = System.currentTimeMillis())

    // ★ 追加：全削除
    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}
