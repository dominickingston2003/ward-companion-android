package com.wardcompanion.ui.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardcompanion.data.model.CreatePatientRequest
import com.wardcompanion.data.model.Patient
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

data class PatientListUi(
    val patients: List<Patient> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val showDischarged: Boolean = false,
)

@HiltViewModel
class PatientListViewModel @Inject constructor(
    private val repo: WardRepository,
    live: LiveSocket,
) : ViewModel() {

    private val _ui = MutableStateFlow(PatientListUi(loading = true))
    val ui: StateFlow<PatientListUi> = _ui.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            live.events.collect { ev ->
                if (ev is LiveEvent.Patient) refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val list = repo.listPatients(includeDischarged = _ui.value.showDischarged)
                _ui.update { it.copy(patients = list.sortedBy { p -> p.bedNumber ?: p.name }, loading = false) }
            } catch (t: Throwable) {
                _ui.update { it.copy(loading = false, error = t.message ?: "Failed to load") }
            }
        }
    }

    fun toggleDischarged() {
        _ui.update { it.copy(showDischarged = !it.showDischarged) }
        refresh()
    }

    fun createPatient(name: String, bed: String?, mrn: String?, diagnosis: String?) {
        viewModelScope.launch {
            try {
                repo.createPatient(CreatePatientRequest(name, bed, mrn, diagnosis))
                refresh()
            } catch (t: Throwable) {
                _ui.update { it.copy(error = t.message) }
            }
        }
    }
}
