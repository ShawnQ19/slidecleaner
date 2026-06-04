package com.gallery.cleaner.util.log

import android.util.Log

enum class LogLevel(val priority: Int) {
    VERBOSE(Log.VERBOSE),
    DEBUG(Log.DEBUG),
    INFO(Log.INFO),
    WARN(Log.WARN),
    ERROR(Log.ERROR);

    fun isAtLeast(level: LogLevel): Boolean {
        return this.priority >= level.priority
    }

    companion object {
        fun fromPriority(priority: Int): LogLevel {
            return when (priority) {
                Log.VERBOSE -> VERBOSE
                Log.DEBUG -> DEBUG
                Log.INFO -> INFO
                Log.WARN -> WARN
                Log.ERROR -> ERROR
                else -> DEBUG
            }
        }
    }
}
