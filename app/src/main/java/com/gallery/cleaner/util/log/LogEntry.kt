package com.gallery.cleaner.util.log

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日志条目数据类，包含完整的日志信息
 */
data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val processId: Int = android.os.Process.myPid(),
    val threadId: Long = Thread.currentThread().id,
    val threadName: String = Thread.currentThread().name,
    val className: String = "",
    val methodName: String = ""
) {
    companion object {
        private val TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    }

    /**
     * 格式化日志为字符串
     */
    fun format(): String {
        val timeStr = TIME_FORMAT.format(Date(timestamp))
        val throwableStr = throwable?.let { "\n${it.stackTraceToString()}" } ?: ""
        return "$timeStr [${level.name}] PID:$processId TID:$threadId($threadName) [$className.$methodName] $message$throwableStr"
    }

    override fun toString(): String = format()
}
