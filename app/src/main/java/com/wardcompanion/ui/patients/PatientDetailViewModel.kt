package com.wardcompanion.ui.patients

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardcompanion.data.model.DailyNote
import com.wardcompanion.data.model.Patient
import com.wardcompanion.data.model.PhotoItem
import com.wardcompanion.data.repo.WardRepository
import com.wardcompanion.data.socket.LiveEvent
import com.wardcompanion.data.socket.LiveSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientDetailUi(
    val patient: Patient? = null,
    val photos: List<PhotoItem> = emptyList(),
    val notes: List<DailyNote> = emptyList(),
    val baseUrl: String = "",
    val authHeader: String? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val uploading: Int = 0,        // remaining uploads
)

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val repo: WardRepository,
    private val live: LiveSocket,
) : ViewModel() {

    private val _ui = MutableStateFlow(PatientDetailUi())
    val ui: StateFlow<PatientDetailUi> = _ui.asStateFlow()

    private var patientId: String = ""

    fun start(id: String) {
        if (id == patientId) return
        patientId = id
        load()
        viewModelScope.launch {
            live.events.collect { ev ->
                val mine = when (ev) {
                    is LiveEvent.Patient -> ev.id == patientId
                    is LiveEvent.Photo -> ev.patientId == patientId
                    is LiveEvent.Note -> ev.patientId == patientId
                }
                if (mine) load()
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val p = repo.getPatient(patientId)
                val photos = repo.listPhotos(patientId).sortedByDescending { it.takenAt }
                val notes = repo.listNotes(patientId).sortedByDescending { it.date }
                _ui.update {
                    it.copy(
                        patient = p,
                        photos = photos,
                        notes = notes,
                        baseUrl = repo.serverUrl(),
                        authHeader = repo.authHeader(),
                        loading = false,
                    )
                }
            } catch (t: Throwable) {
                _ui.update { it.copy(loading = false, error = t.message ?: "Failed to load") }
            }
        }
    }

    fun uploadPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _ui.update { it.copy(uploading = it.uploading + uris.size) }
        viewModelScope.launch {
            for (uri in uris) {
                try {
                    repo.uploadPhoto(patientId, uri, takenAt = System.currentTimeMillis())
                } catch (t: Throwable) {
                    _ui.update { it.copy(error = t.message) }
                } finally {
                    _ui.update { it.copy(uploading = (it.uploading - 1).coerceAtLeast(0)) }
                }
            }
            load()
        }
    }

    fun deletePhoto(id: String) {
        viewModelScope.launch {
            try { repo.deletePhoto(id); load() }
            catch (t: Throwable) { _ui.update { it.copy(error = t.message) } }
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            try { repo.deleteNote(id); load() }
            catch (t: Throwable) { _ui.update { it.copy(error = t.message) } }
        }
    }
}
