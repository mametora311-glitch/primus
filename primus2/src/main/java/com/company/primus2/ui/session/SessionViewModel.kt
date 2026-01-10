package com.company.primus2.ui.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.company.primus2.PrimusApp
import com.company.primus2.autonomy.Action
import com.company.primus2.autonomy.SelfAgent
import com.company.primus2.core_ai.model.ChatMessage
import com.company.primus2.core_ai.model.UserInput
import com.company.primus2.memory.db.entities.BeliefEntity
import com.company.primus2.memory.db.entities.GoalEntity
import com.company.primus2.memory.db.entities.MessageEntity
import com.company.primus2.memory.db.entities.SessionEntity
import com.company.primus2.net.ProxyClient
import com.company.primus2.net.ProxyStatus
import com.company.primus2.repository.PrimusRepository
import com.company.primus2.tts.ITts
import com.company.primus2.tts.NativeTts
import com.company.primus2.tts.isSynthesizing
import com.company.primus2.tts.shutdown
import com.company.primus2.ui.state.AiState
import com.company.primus2.ui.state.SessionUiState
import com.company.primus2.ui.state.VmStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class SessionViewModel(
    application: Application,
    private val repository: PrimusRepository,
    private val selfAgent: SelfAgent
) : AndroidViewModel(application) {

    private val app = application as PrimusApp
    private val ttsManager: ITts = NativeTts(app)

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    private val _status = MutableStateFlow(VmStatus())
    private val _aiState = MutableStateFlow<AiState?>(null)
    private val _beliefs = MutableStateFlow<List<BeliefEntity>>(emptyList())
    private val _goals = MutableStateFlow<List<GoalEntity>>(emptyList())

    // 🔌 Proxy接続ステータス
    private val _proxyStatus = MutableStateFlow(ProxyStatus.UNKNOWN)
    val proxyStatus: StateFlow<ProxyStatus> = _proxyStatus

    val uiState: StateFlow<SessionUiState> =
        _currentSessionId.flatMapLatest { currentId ->
            val messagesFlow =
                currentId?.let { repository.getMessagesForSession(it) } ?: flowOf(emptyList())
            val journalFlow = combine(_beliefs, _goals) { beliefs, goals -> beliefs to goals }

            combine(
                repository.getAllSessions(),
                messagesFlow,
                _status,
                _aiState,
                journalFlow
            ) { allSessions, messages, status, aiState, (beliefs, goals) ->
                SessionUiState(
                    sessions = allSessions,
                    currentSessionId = currentId,
                    messages = messages,
                    status = status,
                    aiState = aiState,
                    beliefs = beliefs,
                    goals = goals
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionUiState()
        )

    init {
        // Personaロード・初期セッション決定
        viewModelScope.launch(Dispatchers.IO) {
            selfAgent.loadPersona()
            loadJournalData()
            val latest = repository.getLatestSession()
            if (latest != null) {
                _currentSessionId.value = latest.id
            } else {
                newSession()
            }
        }

        // 自律アクションの処理
        viewModelScope.launch {
            app.autonomousActionFlow.collect { action ->
                handleAutonomousAction(action)
            }
        }

        // TTS状態（音声生成中）をUIへ反映
        viewModelScope.launch {
            ttsManager.isSynthesizing.collect { syn ->
                _status.update { it.copy(isSynthesizing = syn) }
            }
        }

        // 🔌 起動時に1回だけ接続チェック
        refreshProxyStatus()
    }

    private fun loadJournalData() {
        viewModelScope.launch(Dispatchers.IO) {
            _beliefs.value = repository.getAllBeliefs()
            _goals.value = repository.getAllGoals()
        }
    }

    /**
     * Proxy 接続状態をチェックして _proxyStatus に反映
     * - authOk && fetchConfig() != null で OK
     */
    fun refreshProxyStatus() {
        viewModelScope.launch {
            _proxyStatus.value = ProxyStatus.CHECKING

            val status = withContext(Dispatchers.IO) {
                val client = ProxyClient.default()
                try {
                    val ok = client.authOk() && client.fetchConfig() != null
                    if (ok) ProxyStatus.OK else ProxyStatus.ERROR
                } catch (e: Exception) {
                    ProxyStatus.ERROR
                } finally {
                    client.close()
                }
            }

            _proxyStatus.value = status
        }
    }

    private fun handleAutonomousAction(action: Action) {
        val sessionId = _currentSessionId.value ?: return

        viewModelScope.launch {
            _status.update { it.copy(isLoading = true) }
            try {
                val autonomousMessageText = when (action) {
                    Action.ASK_CLARIFY -> "何かお考えですか？"
                    Action.REMIND -> "そういえば、以前お話しした件ですが…"
                    else -> null
                }

                if (autonomousMessageText != null) {
                    val aiMsg = MessageEntity(
                        sessionId = sessionId,
                        role = "AI",
                        content = autonomousMessageText
                    )
                    repository.insertMessage(aiMsg)

                    if (!_status.value.isMuted) {
                        ttsManager.speak(autonomousMessageText)
                    }
                }
            } finally {
                _status.update { it.copy(isLoading = false) }
            }
        }
    }


    fun newSession() {
        viewModelScope.launch(Dispatchers.IO) {
            val newSession = SessionEntity(
                title = "新しいセッション",
                createdAt = Date().time,
                updatedAt = Date().time
            )
            val id = repository.insertSession(newSession)
            _currentSessionId.value = id
        }
    }

    fun toggleMute() {
        _status.update { it.copy(isMuted = !it.isMuted) }
    }

    fun setShowText(enabled: Boolean) {
        _status.update { it.copy(showText = enabled) }
    }

    fun setVoice(id: Int) {
        _status.update { it.copy(voiceId = id) }
    }

    fun selectSession(sessionId: Long) {
        _currentSessionId.value = sessionId
    }

    fun sendUserInput(text: String) {
        val sessionId = _currentSessionId.value
        // セッション未選択 / 空文字 / 思考中 or 音声再生中なら何もしない
        if (sessionId == null || text.isBlank() || _status.value.isThinking || _status.value.isSynthesizing) return

        viewModelScope.launch {
            _status.update { it.copy(isThinking = true) }
            try {
                // ユーザー発話を保存
                repository.insertMessage(
                    MessageEntity(
                        sessionId = sessionId,
                        role = "USER",
                        content = text
                    )
                )

                // 画面上のメッセージ一覧から会話履歴を作る
                val currentHistory = uiState.value.messages
                val chatHistory = currentHistory.map { m ->
                    ChatMessage(role = m.role, content = m.content)
                }
                val latestHistory = chatHistory + ChatMessage(role = "USER", content = text)

                // 自律思考エンジンへの入力
                val userInput = UserInput(
                    text = text,
                    history = latestHistory
                )

                // Agent応答
                val result = selfAgent.respond(userInput)

                // AI状態の反映
                _aiState.value = AiState(
                    disposition = result.disposition,
                    emotion = result.emotion
                )

                // 応答を保存 &（ミュートでなければ）発話
                val reply = result.text
                repository.insertMessage(
                    MessageEntity(
                        sessionId = sessionId,
                        role = "AI",
                        content = reply
                    )
                )
                if (!_status.value.isMuted) {
                    ttsManager.speak(reply)
                }

                // ジャーナルを随時更新
                loadJournalData()
            } catch (e: Exception) {
                _status.update {
                    it.copy(
                        error = "メッセージ送信またはAI応答生成に失敗しました: ${e.message}",
                        isThinking = false
                    )
                }
            } finally {
                _status.update { it.copy(isThinking = false) }
            }
        }
    }

    fun errorShown() {
        _status.update { it.copy(error = null) }
    }

    override fun onCleared() {
        ttsManager.shutdown()
        super.onCleared()
    }
}
