package com.gallery.cleaner.domain.undo

interface UndoCommand {
    val description: String
    suspend fun execute()
    suspend fun undo()
}
