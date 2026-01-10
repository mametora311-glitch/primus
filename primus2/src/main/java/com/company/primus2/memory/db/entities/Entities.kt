package com.company.primus2.memory.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.company.primus2.memory.db.GoalStatus
import com.company.primus2.memory.db.Role

/* ── Session ── */
@Entity(
    tableName = "sessions",
    indices = [Index("createdAt")]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

/* ── Message ── */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("createdAt")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val role: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

/* ── Belief ── */
@Entity(
    tableName = "beliefs",
    indices = [
        Index(value = ["key"], unique = true),
        Index("updatedAt")
    ]
)
data class BeliefEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

/* ── Goal ── */
@Entity(
    tableName = "goals",
    indices = [Index("createdAt"), Index("priority")]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val priority: Int,
    val status: GoalStatus,
    val dueAt: Long?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

// ▼▼▼ ここから追加 ▼▼▼
/* ── Personality ── */
@Entity(
    tableName = "personality"
)
data class PersonalityEntity(
    // 常に単一のレコードであるため、IDは1に固定
    @PrimaryKey val id: Int = 1,
    val energy: Float,
    val warmth: Float,
    val empathy: Float,
    val updatedAt: Long = System.currentTimeMillis()
)
// ▲▲▲ 追加ここまで ▲▲▲

/* ── Converter ── */
object RoleConverter {
    @TypeConverter
    fun toString(role: Role): String = role.name

    @TypeConverter
    fun toRole(s: String): Role = Role.valueOf(s)
}