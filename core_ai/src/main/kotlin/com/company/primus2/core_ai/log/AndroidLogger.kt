// log/AndroidLogger.kt
package com.company.primus2.core_ai.log
import android.util.Log

object AndroidLogger : PrimusLogger {
    override fun d(tag: String, msg: String) { Log.d(tag, msg) }
    override fun i(tag: String, msg: String) { Log.i(tag, msg) }
    override fun w(tag: String, msg: String) { Log.w(tag, msg) }
    override fun e(tag: String, msg: String, tr: Throwable?) {
        if (tr != null) Log.e(tag, msg, tr) else Log.e(tag, msg)
    }
}
