package com.gallery.cleaner.util.log

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AppLogger {

    @Volatile
    private var minLevel: LogLevel = LogLevel.VERBOSE
    @Volatile
    private var isInitialized = false
    private var fileLogger: FileLogger? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val filters = mutableListOf<LogFilter>()

    @JvmStatic
    fun init(context: Context, level: LogLevel = LogLevel.DEBUG, config: FileLogger.LogConfig = FileLogger.LogConfig()) {
        if (isInitialized) {
            Log.w("AppLogger", "日志系统已初始化，跳过重复调用")
            return
        }
        minLevel = level
        try {
            fileLogger = FileLogger.getInstance(context, config)
        } catch (e: Exception) {
            Log.e("AppLogger", "FileLogger 初始化失败", e)
        }
        isInitialized = true
        Log.i("AppLogger", "日志系统初始化完成，级别: ${level.name}")
        i("AppLogger", "日志系统初始化完成，级别: ${level.name}")
    }

    @JvmStatic
    fun setLevel(level: LogLevel) {
        minLevel = level
    }

    @JvmStatic
    fun addFilter(filter: LogFilter) {
        synchronized(filters) {
            filters.add(filter)
        }
    }

    @JvmStatic
    fun clearFilters() {
        synchronized(filters) {
            filters.clear()
        }
    }

    @JvmStatic
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        if (!level.isAtLeast(minLevel)) return

        val stackTrace = Throwable().stackTrace
        val caller = stackTrace.getOrNull(2)
        val className = caller?.className?.substringAfterLast('.') ?: "Unknown"
        val methodName = caller?.methodName ?: "unknown"

        val formattedMessage = "[$className.$methodName] $message"

        val currentFilters = synchronized(filters) { filters.toList() }
        if (currentFilters.isNotEmpty()) {
            val entry = LogEntry(
                level = level,
                tag = tag,
                message = message,
                throwable = throwable,
                className = className,
                methodName = methodName
            )
            if (!currentFilters.all { it.filter(entry) }) {
                return
            }
        }

        try {
            when (level) {
                LogLevel.VERBOSE -> Log.v(tag, formattedMessage, throwable)
                LogLevel.DEBUG -> Log.d(tag, formattedMessage, throwable)
                LogLevel.INFO -> Log.i(tag, formattedMessage, throwable)
                LogLevel.WARN -> Log.w(tag, formattedMessage, throwable)
                LogLevel.ERROR -> Log.e(tag, formattedMessage, throwable)
            }
        } catch (e: Exception) {
            Log.e("AppLogger", "日志输出异常", e)
        }

        if (isInitialized) {
            val entry = LogEntry(
                level = level,
                tag = tag,
                message = message,
                throwable = throwable,
                className = className,
                methodName = methodName
            )
            scope.launch {
                try {
                    fileLogger?.log(entry)
                } catch (e: Exception) {
                    Log.e("AppLogger", "文件日志写入异常", e)
                }
            }
        }
    }

    @JvmStatic
    fun v(tag: String, message: String) = log(LogLevel.VERBOSE, tag, message)

    @JvmStatic
    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)

    @JvmStatic
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)

    @JvmStatic
    fun w(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.WARN, tag, message, throwable)

    @JvmStatic
    fun e(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, tag, message, throwable)

    @JvmStatic
    fun enter(tag: String, methodName: String, vararg params: Pair<String, Any?>) {
        val paramStr = if (params.isEmpty()) "" else params.joinToString(", ") { "${it.first}=${it.second}" }
        d(tag, "→ ENTER $methodName${if (paramStr.isNotEmpty()) " | $paramStr" else ""}")
    }

    @JvmStatic
    fun exit(tag: String, methodName: String, result: Any? = null) {
        val resultStr = result?.let { " | result=$it" } ?: ""
        d(tag, "← EXIT $methodName$resultStr")
    }

    @JvmStatic
    fun perf(tag: String, operation: String, startTime: Long) {
        val elapsed = System.currentTimeMillis() - startTime
        d(tag, "⏱ PERF $operation 耗时: ${elapsed}ms")
    }

    @JvmStatic
    fun userAction(tag: String, action: String, details: String = "") {
        i(tag, "👤 USER_ACTION $action${if (details.isNotEmpty()) " | $details" else ""}")
    }

    @JvmStatic
    fun dataChange(tag: String, operation: String, details: String = "") {
        d(tag, "📝 DATA_CHANGE $operation${if (details.isNotEmpty()) " | $details" else ""}")
    }

    @JvmStatic
    fun network(tag: String, url: String, method: String = "GET", params: String = "") {
        d(tag, "🌐 NETWORK $method $url${if (params.isNotEmpty()) " | params=$params" else ""}")
    }

    @JvmStatic
    fun exception(tag: String, message: String, throwable: Throwable) {
        e(tag, "💥 EXCEPTION $message", throwable)
    }

    @JvmStatic
    fun getLogFiles(): List<java.io.File> {
        return fileLogger?.getLogFiles() ?: emptyList()
    }

    @JvmStatic
    fun flush() {
        fileLogger?.flush()
    }
}
