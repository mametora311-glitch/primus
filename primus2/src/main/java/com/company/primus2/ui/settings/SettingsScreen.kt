package com.company.primus2.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.company.primus2.BuildConfig
import com.company.primus2.autonomy.AutonomyPanel
import com.company.primus2.billing.LocalPlan
import com.company.primus2.billing.Plan
import com.company.primus2.billing.PlanStore
import com.company.primus2.consent.ConsentStore
import com.company.primus2.data.SettingsStore
import com.company.primus2.device.DeviceIdStore
import com.company.primus2.plan.PlanProvider
import com.company.primus2.ui.session.AutonomyControllerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AutonomyControllerViewModel,
    onNavigateBack: () -> Unit,
    onOpenErase: () -> Unit
) {
    val plan = remember { PlanProvider.current() }

    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val learningEnabled by settingsStore.isLearningEnabled.collectAsState(initial = false)
    val languageTag by settingsStore.languageTag.collectAsState(initial = "ja")
    var languageMenuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 設定中の言語に合わせてアプリ全体のロケールを適用
    LaunchedEffect(languageTag) {
        val locales: LocaleListCompat = LocaleListCompat.forLanguageTags(languageTag)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    val languageLabel = when (languageTag) {
        "en"  -> "English"
        "zh"  -> "中文"
        "ko"  -> "한국어"
        "fr"  -> "Français"
        "es"  -> "Español"
        "pt"  -> "Português"
        "ru"  -> "Русский"
        "th"  -> "ไทย"
        "fil" -> "Filipino"
        "hi"  -> "हिन्दी"
        "ar"  -> "العربية"
        else  -> "日本語"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
        ) {
            // ▼ 学習トグル（SettingsStore 連携）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("学習を有効にする")
                Switch(
                    checked = learningEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsStore.setLearningEnabled(enabled)
                        }
                    },
                    enabled = true
                )
            }

            // ▼ データ削除UIへの導線（端末表層のみリセット）
            OutlinedButton(
                onClick = onOpenErase,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) { Text("データ削除（端末表層・ダミー）") }

            // ▼ 表示言語
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("表示言語")

                Box {
                    OutlinedButton(onClick = { languageMenuExpanded = true }) {
                        Text(languageLabel)
                    }

                    DropdownMenu(
                        expanded = languageMenuExpanded,
                        onDismissRequest = { languageMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("日本語") },
                            onClick = {
                                languageMenuExpanded = false
                                scope.launch { settingsStore.setLanguageTag("ja") }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("English") },
                            onClick = {
                                languageMenuExpanded = false
                                scope.launch { settingsStore.setLanguageTag("en") }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("中文") },
                            onClick = {
                                languageMenuExpanded = false
                                scope.launch { settingsStore.setLanguageTag("zh") }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("한국어") },
                            onClick = {
                                languageMenuExpanded = false
                                scope.launch { settingsStore.setLanguageTag("ko") }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Français") },
                            onClick = {
                                languageMenuExpanded = false
                                scope.launch { settingsStore.setLanguageTag("fr") }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Español") },
                            onClick = {
                                languageMenuExpanded = false
                                scope.launch { settingsStore.setLanguageTag("es") }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Português") },
                            onClick = {
                                languageMenuExpanded = false
                                scope.launch { settingsStore.setLanguageTag("pt") }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Русский") },
                            onClick = {
                                languageMenuExpanded = false
                                scope.launch { settingsStore.setLanguageTag("ru") }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("ไทย") },
                            onClick = {
                                languageMenuExpanded = false
                                scope.launch { settingsStore.setLanguageTag("th") }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Filipino") },
                            onClick = {
                                languageMenuExpanded = false
                                scope.launch { settingsStore.setLanguageTag("fil") }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("हिन्दी") },
                            onClick = {
                                languageMenuExpanded = false
                                scope.launch { settingsStore.setLanguageTag("hi") }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("العربية") },
                            onClick = {
                                languageMenuExpanded = false
                                scope.launch { settingsStore.setLanguageTag("ar") }
                            }
                        )
                    }
                }
            }

            // 既存：自律パネル（開発用）
            AutonomyPanel(
                vm = viewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            if (BuildConfig.DEBUG) {
                DebugInfoSection()
            }
        }
    }
}

@Composable
private fun DebugInfoSection() {
    val ctx = LocalContext.current
    val plan = LocalPlan.current
    val consent = ConsentStore(ctx).allowedFlow.collectAsState(initial = false).value
    val deviceId = DeviceIdStore.flow(ctx).collectAsState(initial = null).value ?: "—"
    val scope = rememberCoroutineScope()

    Spacer(Modifier.height(24.dp))
    Surface(tonalElevation = 2.dp) {
        Column(Modifier.padding(12.dp)) {
            Text("Debug Info", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Plan: ${if (plan == Plan.PAID) "PAID" else "FREE"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Consent: ${if (consent) "ON" else "OFF"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text("device_id: $deviceId", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(12.dp))
            Row {
                OutlinedButton(
                    onClick = { scope.launch { PlanStore.set(ctx, Plan.FREE) } }
                ) { Text("Set FREE") }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = { scope.launch { PlanStore.set(ctx, Plan.PAID) } }
                ) { Text("Set PAID") }
            }
        }
    }
}
