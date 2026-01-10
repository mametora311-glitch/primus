package com.company.primus2.ui.util

import android.util.Log
import com.company.primus2.PrimusApp

object AppEvents {
    fun userLogout(app: PrimusApp) {
        try {
            // "core"を抜いて、直接sleepRefineを呼び出す
            app.services.sleepRefine()
        } catch (t: Throwable) {
            Log.w("Primus", "logout.sleepRefine: ${t.message}")
        }
        Log.i("Primus", "logout done")
    }
}