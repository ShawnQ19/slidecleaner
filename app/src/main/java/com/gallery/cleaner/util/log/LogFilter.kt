package com.gallery.cleaner.util.log

/**
 * 日志过滤器接口
 */
interface LogFilter {
    fun filter(entry: LogEntry): Boolean
}

/**
 * 按日志级别过滤
 */
class LevelFilter(private val minLevel: LogLevel) : LogFilter {
    override fun filter(entry: LogEntry): Boolean {
        return entry.level.isAtLeast(minLevel)
    }
}

/**
 * 按标签过滤（支持多个标签，满足其一即可）
 */
class TagFilter(private vararg val tags: String) : LogFilter {
    override fun filter(entry: LogEntry): Boolean {
        if (tags.isEmpty()) return true
        return tags.any { entry.tag.contains(it, ignoreCase = true) }
    }
}

/**
 * 按关键词过滤（消息内容包含关键词）
 */
class KeywordFilter(private vararg val keywords: String) : LogFilter {
    override fun filter(entry: LogEntry): Boolean {
        if (keywords.isEmpty()) return true
        return keywords.any { entry.message.contains(it, ignoreCase = true) }
    }
}

/**
 * 组合过滤器（所有条件都必须满足）
 */
class CompositeFilter(private val filters: List<LogFilter>) : LogFilter {
    constructor(vararg filters: LogFilter) : this(filters.toList())

    override fun filter(entry: LogEntry): Boolean {
        return filters.all { it.filter(entry) }
    }
}
