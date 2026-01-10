package com.company.primus2

// /license HTTP 取得用
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.company.primus2.billing.LicenseFetchUseCase
import com.company.primus2.billing.PlanEnforcer
import com.company.primus2.billing.PlanProvider
import com.company.primus2.billing.PlanStore
import com.company.primus2.billing.isPaid
import com.company.primus2.consent.UmpConsentManager
import com.company.primus2.firebase.FirebaseAuthManager
import com.company.primus2.firebase.FirebaseConfig
import com.company.primus2.firebase.LicenseSync
import com.company.primus2.firebase.TokenRefresher
import com.company.primus2.net.ProxyClient
import com.company.primus2.net.ProxyEnv
import com.company.primus2.ui.consent.ConsentScreen
import com.company.primus2.ui.consent.TutorialScreen
import com.company.primus2.ui.journal.MemoryJournalScreen
import com.company.primus2.ui.session.AutonomyControllerViewModel
import com.company.primus2.ui.session.SessionScreen
import com.company.primus2.ui.session.SessionViewModel
import com.company.primus2.ui.settings.EraseDataScreen
import com.company.primus2.ui.settings.SettingsScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var planEnforceJob: Job? = null
    private var tokenRefreshJob: Job? = null
    private var licenseSyncJob: Job? = null

    private val autoVm: AutonomyControllerViewModel by viewModels()

    private val sessionVm: SessionViewModel by viewModels {
        val app = application as PrimusApp
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SessionViewModel(
                    application = app,
                    repository = app.repository,
                    selfAgent = app.selfAgent
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // UMP: 必要地域のみ同意ダイアログ（広告未導入でもno-op）
        lifecycleScope.launch {
            runCatching { UmpConsentManager.requestIfRequired(this@MainActivity, this@MainActivity) }
        }

        // Firebase 設定チェック（必須/警告）
        val configured = FirebaseAuthManager.isConfigured(applicationContext)
        if (!configured) {
            if (FirebaseConfig.ENFORCE) {
                Toast.makeText(this, "Firebase設定が見つかりません（google-services.json）", Toast.LENGTH_LONG).show()
                finish()
                return
            } else {
                // Debug 時のみ警告（任意運用）
                Toast.makeText(this, "Firebase未設定: google-services.json を配置してください", Toast.LENGTH_LONG).show()
            }
        }

        lifecycleScope.launch {
            // 匿名サインイン（未設定環境では内部 no-op）
            FirebaseAuthManager.ensureAnonymousSignIn(applicationContext)
            // IDトークン定期更新（内部 no-op 可）
            tokenRefreshJob = TokenRefresher.start(applicationContext, lifecycleScope)
            // Firestore のライセンス同期（内部 no-op 可）
            licenseSyncJob = LicenseSync.start(applicationContext, lifecycleScope)
            // HTTPで /license を一度取得→反映（失敗は握りつぶし）
            runCatching {
                ProxyClient.default().use { pc ->
                    LicenseFetchUseCase(applicationContext, pc)()
                    Log.d("Primus/ProxyClient", "BASE=${ProxyEnv.BASE_URL}")
                }
            }

            // 起動時のプランに合わせて自律の整合
            val plan = PlanStore.observe(applicationContext).first()
            if (plan.isPaid) autoVm.startIfNeeded() else autoVm.stop()
        }

        // 実行中のプラン変更にも追随
        planEnforceJob = PlanEnforcer.start(
            context = applicationContext,
            scope = lifecycleScope,
            onStart = { autoVm.startIfNeeded() },
            onStop = { autoVm.stop() }
        )

        setContent {
            PlanProvider(context = applicationContext) {
                MaterialTheme {
                    Surface {
                        val navController = rememberNavController()
                        NavHost(navController = navController, startDestination = "consent") {

                            // 同意 → チュートリアル
                            composable("consent") {
                                ConsentScreen(
                                    onAgree = { navController.navigate("tutorial") },
                                    onDisagree = {
                                        navController.navigate("session") {
                                            popUpTo("consent") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("tutorial") {
                                TutorialScreen(
                                    onCompleted = {
                                        navController.navigate("session") {
                                            popUpTo("consent") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // セッション
                            composable("session") {
                                val state by sessionVm.uiState.collectAsState()

                                SessionScreen(
                                    state = state,
                                    onSend = { text -> sessionVm.sendUserInput(text) },
                                    onErrorShown = { sessionVm.errorShown() },
                                    onNavigateToSettings = { navController.navigate("settings") },
                                    onNavigateToJournal = { navController.navigate("journal") },

                                    // ▼ ここから追加：音声出力・表示・ボイス切替の配線 ▼
                                    isMuted = state.status.isMuted,
                                    showText = state.status.showText,
                                    voiceId = state.status.voiceId,
                                    onToggleMute = { sessionVm.toggleMute() },
                                    onSetShowText = { enabled -> sessionVm.setShowText(enabled) },
                                    onSetVoice = { id -> sessionVm.setVoice(id) }
                                )
                            }


                            // 設定
                            composable("settings") {
                                SettingsScreen(
                                    viewModel = autoVm,
                                    onNavigateBack = { navController.popBackStack() },
                                    onOpenErase = { navController.navigate("erase") }
                                )
                            }

                            // ジャーナル
                            composable("journal") {
                                val state by sessionVm.uiState.collectAsState()
                                MemoryJournalScreen(
                                    beliefs = state.beliefs,
                                    goals = state,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            // 表示データのリセット
                            composable("erase") {
                                EraseDataScreen(
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    override fun onDestroy() {
        planEnforceJob?.cancel(); planEnforceJob = null
        tokenRefreshJob?.cancel(); tokenRefreshJob = null
        licenseSyncJob?.cancel(); licenseSyncJob = null
        super.onDestroy()
    }
}
