package com.gallery.cleaner.domain.undo

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UndoManager(private val maxHistory: Int = 10) {

    companion object {
        private const val TAG = "UndoManager"
    }

    private val undoStack = ArrayDeque<UndoCommand>()
    private val redoStack = ArrayDeque<UndoCommand>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _lastAction = MutableStateFlow<String?>(null)
    val lastAction: StateFlow<String?> = _lastAction.asStateFlow()

    suspend fun execute(command: UndoCommand) {
        Log.d(TAG, "执行命令: ${command.description}")
        command.execute()
        undoStack.addLast(command)
        redoStack.clear()
        trimUndoStack()
        updateState()
        _lastAction.value = command.description
    }

    suspend fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        val command = undoStack.removeLast()
        Log.d(TAG, "撤销命令: ${command.description}")
        command.undo()
        redoStack.addLast(command)
        updateState()
        _lastAction.value = "撤销: ${command.description}"
        return true
    }

    suspend fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        val command = redoStack.removeLast()
        Log.d(TAG, "重做命令: ${command.description}")
        command.execute()
        undoStack.addLast(command)
        updateState()
        _lastAction.value = "重做: ${command.description}"
        return true
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        updateState()
        _lastAction.value = null
    }

    private fun trimUndoStack() {
        while (undoStack.size > maxHistory) {
            undoStack.removeFirst()
        }
    }

    private fun updateState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        Log.d(TAG, "状态更新: canUndo=${_canUndo.value}(${undoStack.size}), canRedo=${_canRedo.value}(${redoStack.size})")
    }
}
