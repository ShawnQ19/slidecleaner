package com.gallery.cleaner.domain.undo

import com.gallery.cleaner.domain.model.MediaItem

class QueueForDeleteCommand(
    private val item: MediaItem,
    private val removedIndex: Int,
    private val oldIndex: Int,
    private val onQueue: (MediaItem) -> Unit,
    private val onRestore: (MediaItem, Int, Int) -> Unit
) : UndoCommand {
    override val description: String = "标记删除 ${item.name}"

    override suspend fun execute() {
        onQueue(item)
    }

    override suspend fun undo() {
        onRestore(item, removedIndex, oldIndex)
    }
}
