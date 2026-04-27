package com.wardcompanion.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardcompanion.data.repo.WardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DailyNoteEditorUi(
    val date: Long = System.currentTimeMillis(),
    val text: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DailyNoteEditorViewModel @Inject constructor(
    private val repo: WardRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(DailyNoteEditorUi())
    val ui: StateFlow<DailyNoteEditorUi> = _ui.asStateFlow()

    private var patientId: String = ""
    private var noteId: String? = null

    fun start(patientId: String, noteId: String?) {
        if (patientId == this.patientId && noteId == this.noteId) return
        this.patientId = patientId
        this.noteId = noteId
        if (noteId != null) {
            viewModelScope.launch {
                try {
                    val all = repo.listNotes(patientId)
                    val n = all.firstOrNull { it.id == noteId }
                    if (n != null) _ui.value = DailyNoteEditorUi(date = n.date, text = n.text)
                } catch (t: Throwable) {
                    _ui.update { it.copy(error = t.message) }
                }
            }
        }
    }

    fun onTextChange(s: String) = _ui.update { it.copy(text = s, error = null) }
    fun onDateChange(d: Long) = _ui.update { it.copy(date = d) }

    fun save() {
        val cur = _ui.value
        if (cur.text.isBlank()) return
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null) }
            try {
                if (noteId == null) {
                    repo.createNote(patientId, cur.date, cur.text)
                } else {
                    repo.updateNote(noteId!!, cur.date, cur.text)
                }
                _ui.update { it.copy(saving = false, saved = true) }
            } catch (t: Throwable) {
                _ui.update { it.copy(saving = false, error = t.message ?: "Save failed") }
            }
        }
    }
}
