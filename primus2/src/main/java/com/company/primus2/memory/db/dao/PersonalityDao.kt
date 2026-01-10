package com.company.primus2.memory.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.company.primus2.memory.db.entities.PersonalityEntity

@Dao
interface PersonalityDao {

    /** 現在の人格を取得する（レコードは常に1つのはず） */
    @Query("SELECT * FROM personality WHERE id = 1")
    suspend fun getPersonality(): PersonalityEntity?

    /** 人格を保存する（存在すれば更新、なければ挿入） */
    @Upsert
    suspend fun savePersonality(personality: PersonalityEntity)

    // ★ 追加：全削除（初期状態へ）
    @Query("DELETE FROM personality")
    suspend fun deleteAll()
}
