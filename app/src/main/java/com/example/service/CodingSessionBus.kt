package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CodingSession(
    val id: String,
    val title: String,
    val fileName: String,
    val language: String,
    val code: String,
    val savedPath: String = "",
    val isSaved: Boolean = false,
    val pendingSaveConfirmation: Boolean = true,
    val isDiscarded: Boolean = false
)

/** Drives the full-screen "Argon 4 live typing" page with mandatory user confirmation before saving to any folder. */
object CodingSessionBus {
    private val _session = MutableStateFlow<CodingSession?>(null)
    val session: StateFlow<CodingSession?> = _session.asStateFlow()

    @Volatile
    var onConfirmSaveRequested: (() -> Unit)? = null

    @Volatile
    var onDiscardRequested: (() -> Unit)? = null

    fun start(s: CodingSession) {
        _session.value = s
    }

    fun hasPendingUnsavedCode(): Boolean {
        val cur = _session.value ?: return false
        return cur.pendingSaveConfirmation && !cur.isSaved && !cur.isDiscarded
    }

    fun markSaved(path: String) {
        _session.value = _session.value?.copy(
            savedPath = path,
            isSaved = true,
            pendingSaveConfirmation = false,
            isDiscarded = false
        )
    }

    fun markDiscarded() {
        _session.value = _session.value?.copy(
            isSaved = false,
            pendingSaveConfirmation = false,
            isDiscarded = true
        )
    }

    fun requestConfirmSave() {
        onConfirmSaveRequested?.invoke()
    }

    fun requestDiscard() {
        onDiscardRequested?.invoke()
        _session.value = null
    }

    fun dismiss() {
        _session.value = null
    }
}

