// log/PrimusLogger.kt
package com.company.primus2.core_ai.log
interface PrimusLogger {
    fun d(tag: String, msg: String)
    fun i(tag: String, msg: String)
    fun w(tag: String, msg: String)
    fun e(tag: String, msg: String, tr: Throwable? = null)
}
object NoopLogger : PrimusLogger {
    override fun d(tag:String,msg:String) {}
    override fun i(tag:String,msg:String) {}
    override fun w(tag:String,msg:String) {}
    override fun e(tag:String,msg:String,tr:Throwable?) {}
}
