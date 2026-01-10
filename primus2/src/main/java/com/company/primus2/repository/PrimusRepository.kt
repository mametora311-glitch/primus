package com.company.primus2.repository

import com.company.primus2.core_ai.model.DispositionState
import com.company.primus2.core_ai.repository.PersonaRepository
import com.company.primus2.memory.db.dao.BeliefDao
import com.company.primus2.memory.db.dao.GoalDao
import com.company.primus2.memory.db.dao.MessageDao
import com.company.primus2.memory.db.dao.PersonalityDao
import com.company.primus2.memory.db.dao.SessionDao
import com.company.primus2.memory.db.entities.BeliefEntity
import com.company.primus2.memory.db.entities.GoalEntity
import com.company.primus2.memory.db.entities.MessageEntity
import com.company.primus2.memory.db.entities.PersonalityEntity
import com.company.primus2.memory.db.entities.SessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PrimusRepository(
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao,
    private val beliefDao: BeliefDao,
    private val goalDao: GoalDao,
    private val personalityDao: PersonalityDao
) : PersonaRepository {

    // --- Session ---
    fun getAllSessions(): Flow<List<SessionEntity>> = sessionDao.observeAll()
    suspend fun getSessionById(id: Long): SessionEntity? = withContext(Dispatchers.IO) { sessionDao.get(id) }
    suspend fun insertSession(session: SessionEntity): Long = withContext(Dispatchers.IO) { sessionDao.insert(session) }
    suspend fun updateSession(session: SessionEntity) = withContext(Dispatchers.IO) { sessionDao.update(session) }

    // ▼▼▼ この関数を追加 ▼▼▼
    suspend fun getLatestSession(): SessionEntity? = withContext(Dispatchers.IO) {
        sessionDao.getLatest()
    }

    // --- Messages ---
    fun getMessagesForSession(sessionId: Long): Flow<List<MessageEntity>> = messageDao.observeBySession(sessionId)
    suspend fun insertMessage(message: MessageEntity): Long = withContext(Dispatchers.IO) { messageDao.insert(message) }
    suspend fun getLatestMessages(limit: Int): List<MessageEntity> = withContext(Dispatchers.IO) { messageDao.latest(limit) }
    suspend fun getMessagesBySessionOnce(sessionId: Long): List<MessageEntity> = withContext(Dispatchers.IO) { messageDao.listBySession(sessionId) }
    suspend fun getAllConversationsForTraining(): Map<Long, List<MessageEntity>> = withContext(Dispatchers.IO) {
        messageDao.getAllMessages().groupBy { it.sessionId }
    }

    // --- Beliefs ---
    suspend fun saveBelief(key: String, value: String) = withContext(Dispatchers.IO) {
        val entity = BeliefEntity(key = key, value = value)
        beliefDao.upsert(entity)
    }
    suspend fun getAllBeliefs(): List<BeliefEntity> = withContext(Dispatchers.IO) {
        beliefDao.getAll()
    }

    // --- Goals ---
    suspend fun getAllGoals(): List<GoalEntity> = withContext(Dispatchers.IO) {
        goalDao.listAll()
    }
    suspend fun insertGoal(goal: GoalEntity) = withContext(Dispatchers.IO) {
        goalDao.upsert(goal)
    }

    // --- Personality ---
    override suspend fun getPersonality(): DispositionState? = withContext(Dispatchers.IO) {
        personalityDao.getPersonality()?.let { entity ->
            DispositionState(energy = entity.energy, warmth = entity.warmth, empathy = entity.empathy)
        }
    }
    override suspend fun savePersonality(persona: DispositionState) = withContext(Dispatchers.IO) {
        val entity = PersonalityEntity(
            energy = persona.energy,
            warmth = persona.warmth,
            empathy = persona.empathy
        )
        personalityDao.savePersonality(entity)
    }
}