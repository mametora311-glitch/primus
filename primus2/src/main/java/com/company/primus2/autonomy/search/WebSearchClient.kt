package com.company.primus2.autonomy.search

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class WebSearchClient {

    fun summary(query: String, lang: String = "ja"): String? {
        val q = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://$lang.wikipedia.org/api/rest_v1/page/summary/$q")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
            setRequestProperty("Accept", "application/json")
        }
        return runCatching {
            conn.inputStream.use { ins ->
                BufferedReader(InputStreamReader(ins, StandardCharsets.UTF_8)).use { br ->
                    val sb = StringBuilder()
                    var line: String?
                    while (br.readLine().also { line = it } != null) sb.append(line)
                    val json = JSONObject(sb.toString())
                    json.optString("extract").takeIf { it.isNotBlank() }
                }
            }
        }.onFailure { e ->
            Log.w("WebSearchClient", "summary failed: ${e.message}")
        }.getOrNull().also {
            conn.disconnect()
        }
    }
}
