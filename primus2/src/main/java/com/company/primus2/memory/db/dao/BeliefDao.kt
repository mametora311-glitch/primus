package com.company.primus2.memory.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.company.primus2.memory.db.entities.BeliefEntity

@Dao
interface BeliefDao {

    @Upsert
    suspend fun upsert(e: BeliefEntity): Long

    @Update
    suspend fun update(e: BeliefEntity)

    /**
     * key で 1件取得する。
     * Room のカラム解析と確実に一致させるため、部分指定ではなく SELECT * を使う。
     */
    @Query(
        """
        SELECT *
        FROM beliefs
        WHERE `key` = :key
        LIMIT 1
        """
    )
    suspend fun find(key: String): BeliefEntity?

    /**
     * 更新日時順で全件取得。
     */
    @Query("SELECT * FROM beliefs ORDER BY updatedAt DESC")
    suspend fun getAll(): List<BeliefEntity>

    /**
     * 全削除（表面データ初期化用）。
     */
    @Query("DELETE FROM beliefs")
    suspend fun deleteAll()
}
