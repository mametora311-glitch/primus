package com.company.primus2

import android.app.Application
import com.company.primus2.autonomy.Action
import com.company.primus2.autonomy.SelfAgent
import com.company.primus2.autonomy.goal.GoalEngine
import com.company.primus2.core_ai.persona.PersonalityEngine
import com.company.primus2.memory.db.PrimusDatabase
import com.company.primus2.repository.PrimusRepository
import com.company.primus2.ui.util.Services
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow

class PrimusApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db: PrimusDatabase by lazy { PrimusDatabase.get(this) }

    val repository: PrimusRepository by lazy {
        PrimusRepository(
            db.sessionDao(),
            db.messageDao(),
            db.beliefDao(),
            db.goalDao(),
            db.personalityDao()
        )
    }

    val selfAgent: SelfAgent by lazy {
        val personalityEngine = PersonalityEngine(repository)
        SelfAgent(
            ctxProvider = { this },
            personalityEngine = personalityEngine,
            repository = repository
        )
    }

    val services: Services by lazy { Services(this) }
    val goalEngine: GoalEngine by lazy { GoalEngine(repository) }
    val autonomousActionFlow = MutableStateFlow<Action>(Action.NOOP)
}