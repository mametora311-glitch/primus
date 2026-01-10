package com.company.primus2.ui.consent

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val PREF = "primus_profile"
private const val KEY_NAME = "display_name"
private const val KEY_PNAME = "primus_name"

@Composable
fun TutorialScreen(
    onCompleted: () -> Unit
) {
    val ctx = LocalContext.current
    var displayName by remember { mutableStateOf("") }
    var primusName by remember { mutableStateOf("Primus") }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("チュートリアル", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("あなたの呼称") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = primusName, onValueChange = { primusName = it }, label = { Text("Primus個体名") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                saveProfile(ctx, displayName, primusName)
                onCompleted()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("保存してはじめる") }
    }
}

private fun saveProfile(ctx: Context, name: String, pname: String) {
    val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    sp.edit().putString(KEY_NAME, name).putString(KEY_PNAME, pname).apply()
}
