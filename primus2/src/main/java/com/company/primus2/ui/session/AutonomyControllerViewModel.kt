package com.company.primus2.ui.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.company.primus2.PrimusApp
import com.company.primus2.autonomy.Action
import com.company.primus2.autonomy.AdvancedPlanner
import com.company.primus2.autonomy.AutonomyLoop
import com.company.primus2.autonomy.SimpleCritic
import com.company.primus2.autonomy.StoreBackedConsentGate
import com.company.primus2.autonomy.search.AutonomyBudget
import com.company.primus2.consent.ConsentStore
import kotlinx.coroutines.launch

class AutonomyControllerViewModel(application: Application) : AndroidViewModel(application) {

    private val loop: AutonomyLoop

    init {
        val context = application.applicationContext
        val primusApp = application as PrimusApp

        val consentGate = StoreBackedConsentGate(ConsentStore(context))
        val budget = AutonomyBudget(context)
        val planner = AdvancedPlanner(primusApp.repository)
        val critic = SimpleCritic()

        loop = AutonomyLoop(
            consent = consentGate,
            budget = budget,
            planner = planner,
            critic = critic,
            scope = viewModelScope,
            onAction = { action -> // ▼▼▼ AutonomyLoopからの通知をここで受け取る ▼▼▼
                if (action != Action.NOOP) {
                    viewModelScope.launch {
                        // ▼▼▼ PrimusAppの中継地点にアクションを送信 ▼▼▼
                        primusApp.autonomousActionFlow.emit(action)
                    }
                }
            }
        )
    }

    fun startIfNeeded() {
        loop.start()
    }

    fun stop() {
        loop.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}