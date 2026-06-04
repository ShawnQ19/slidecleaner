package com.gallery.cleaner.util.log

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * 文件日志管理器
 * 支持日志文件大小限制和自动轮转策略
 */
class FileLogger private constructor(
    private val context: Context,
    private val config: LogConfig
) {
    data class LogConfig(
        val maxFileSize: Long = 5 * 1024 * 1024,  // 单个日志文件最大 5MB
        val maxFileCount: Int = 5,                  // 最多保留 5 个日志文件
        val logDirName: String = "logs",            // 日志目录名
        val bufferSize: Int = 100,                  // 缓冲区大小
        val flushIntervalMs: Long = 5000            // 自动刷新间隔（毫秒）
    )

    companion object {
        private const val TAG = "FileLogger"
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private const val LOG_FILE_PREFIX = "app_"
        private const val LOG_FILE_SUFFIX = ".log"

        @Volatile
        private var instance: FileLogger? = null

        fun getInstance(context: Context, config: LogConfig = LogConfig()): FileLogger {
            return instance ?: synchronized(this) {
                instance ?: FileLogger(context.applicationContext, config).also {
                    instance = it
                }
            }
        }
    }

    private val logDir: File by lazy {
        File(context.getExternalFilesDir(null), config.logDirName).apply {
            if (!exists()) mkdirs()
        }
    }

    private val logChannel = Channel<LogEntry>(Channel.BUFFERED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isInitialized = AtomicBoolean(false)
    private val currentFileSize = AtomicLong(0)
    private var currentLogFile: File? = null
    private var fileWriter: FileWriter? = null

    init {
        initialize()
    }

    private fun initialize() {
        if (isInitialized.getAndSet(true)) return

        scope.launch {
            try {
                openLogFile()
                startAutoFlush()
                processLogEntries()
            } catch (e: Exception) {
                Log.e(TAG, "文件日志初始化失败", e)
            }
        }
    }

    /**
     * 打开当前日志文件
     */
    private fun openLogFile() {
        try {
            val dateStr = DATE_FORMAT.format(Date())
            val fileName = "${LOG_FILE_PREFIX}${dateStr}${LOG_FILE_SUFFIX}"
            val file = File(logDir, fileName)

            if (!file.exists()) {
                file.createNewFile()
                currentFileSize.set(0)
            } else {
                currentFileSize.set(file.length())
            }

            fileWriter?.close()
            fileWriter = FileWriter(file, true)
            currentLogFile = file

            Log.i(TAG, "日志文件已打开: ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e(TAG, "打开日志文件失败", e)
        }
    }

    /**
     * 检查并轮转日志文件
     */
    private fun checkAndRotate() {
        if (currentFileSize.get() >= config.maxFileSize) {
            rotateLogFiles()
        }
    }

    /**
     * 执行日志文件轮转
     */
    private fun rotateLogFiles() {
        try {
            fileWriter?.close()
            fileWriter = null

            val dateStr = DATE_FORMAT.format(Date())
            val timestamp = System.currentTimeMillis()
            val newName = "${LOG_FILE_PREFIX}${dateStr}_${timestamp}${LOG_FILE_SUFFIX}"
            currentLogFile?.renameTo(File(logDir, newName))

            cleanupOldFiles()
            openLogFile()
        } catch (e: IOException) {
            Log.e(TAG, "日志轮转失败", e)
        }
    }

    /**
     * 清理旧的日志文件
     */
    private fun cleanupOldFiles() {
        try {
            val files = logDir.listFiles { file ->
                file.name.startsWith(LOG_FILE_PREFIX) && file.name.endsWith(LOG_FILE_SUFFIX)
            }?.sortedBy { it.lastModified() } ?: return

            if (files.size > config.maxFileCount) {
                files.take(files.size - config.maxFileCount).forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理旧日志文件失败", e)
        }
    }

    /**
     * 启动自动刷新任务
     */
    private fun startAutoFlush() {
        scope.launch {
            while (isActive) {
                delay(config.flushIntervalMs)
                flush()
            }
        }
    }

    /**
     * 处理日志条目队列
     */
    private suspend fun processLogEntries() {
        logChannel.consumeEach { entry ->
            try {
                checkAndRotate()
                val logLine = entry.format() + "\n"
                fileWriter?.write(logLine)
                currentFileSize.addAndGet(logLine.toByteArray().size.toLong())
            } catch (e: IOException) {
                Log.e(TAG, "写入日志失败", e)
            }
        }
    }

    /**
     * 写入日志条目
     */
    fun log(entry: LogEntry) {
        if (!isInitialized.get()) return
        scope.launch {
            logChannel.send(entry)
        }
    }

    /**
     * 立即刷新缓冲区到文件
     */
    fun flush() {
        try {
            fileWriter?.flush()
        } catch (e: IOException) {
            Log.e(TAG, "刷新日志失败", e)
        }
    }

    /**
     * 获取日志文件列表
     */
    fun getLogFiles(): List<File> {
        return logDir.listFiles { file ->
            file.name.startsWith(LOG_FILE_PREFIX) && file.name.endsWith(LOG_FILE_SUFFIX)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * 获取最新日志文件内容
     */
    fun getLatestLogContent(): String {
        return try {
            getLogFiles().firstOrDefault()?.readText() ?: ""
        } catch (e: IOException) {
            Log.e(TAG, "读取日志内容失败", e)
            ""
        }
    }

    /**
     * 关闭文件日志
     */
    fun close() {
        scope.cancel()
        try {
            fileWriter?.close()
        } catch (e: IOException) {
            Log.e(TAG, "关闭日志文件失败", e)
        }
        instance = null
    }

    private inline fun <T> List<T>.firstOrDefault(default: T? = null): T? {
        return if (isNotEmpty()) this[0] else default
    }
}
