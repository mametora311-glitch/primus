package com.company.primus2.memory.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.company.primus2.memory.db.dao.BeliefDao
import com.company.primus2.memory.db.dao.GoalDao
import com.company.primus2.memory.db.dao.MessageDao
import com.company.primus2.memory.db.dao.PersonalityDao // ▼ 追加
import com.company.primus2.memory.db.dao.SessionDao
import com.company.primus2.memory.db.entities.BeliefEntity
import com.company.primus2.memory.db.entities.GoalEntity
import com.company.primus2.memory.db.entities.MessageEntity
import com.company.primus2.memory.db.entities.PersonalityEntity // ▼ 追加
import com.company.primus2.memory.db.entities.RoleConverter
import com.company.primus2.memory.db.entities.SessionEntity

@Database(
    entities = [
        SessionEntity::class,
        MessageEntity::class,
        BeliefEntity::class,
        GoalEntity::class,
        PersonalityEntity::class // ▼ 追加
    ],
    version = 3, // ▼ バージョンを2から3へ更新
    exportSchema = true
)
@TypeConverters(RoleConverter::class)
abstract class PrimusDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun beliefDao(): BeliefDao
    abstract fun goalDao(): GoalDao
    abstract fun personalityDao(): PersonalityDao // ▼ 追加

    companion object {
        @Volatile private var INSTANCE: PrimusDatabase? = null
        fun get(context: Context): PrimusDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PrimusDatabase::class.java,
                    "primus.db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}