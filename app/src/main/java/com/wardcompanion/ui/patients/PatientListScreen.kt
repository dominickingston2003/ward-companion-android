package com.wardcompanion.ui.patients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wardcompanion.data.model.Patient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    onPatient: (String) -> Unit,
    onLogout: () -> Unit,
    vm: PatientListViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patients") },
                actions = {
                    IconButton(onClick = { vm.toggleDischarged() }) {
                        Icon(
                            if (ui.showDischarged) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle discharged",
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add patient")
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                ui.loading && ui.patients.isEmpty() -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                ui.error != null && ui.patients.isEmpty() -> {
                    Text(
                        ui.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
                ui.patients.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("No patients yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap + to admit one.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(ui.patients, key = { it.id }) { p ->
                            PatientCard(p, onClick = { onPatient(p.id) })
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        NewPatientDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, bed, mrn, dx ->
                vm.createPatient(name, bed, mrn, dx)
                showCreate = false
            },
        )
    }
}

@Composable
private fun PatientCard(p: Patient, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = if (p.dischargedAt == null) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(44.dp).clip(CircleShape),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        p.bedNumber ?: p.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium)
                if (!p.diagnosis.isNullOrBlank()) {
                    Text(
                        p.diagnosis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!p.mrn.isNullOrBlank()) {
                    Text(
                        "MRN ${p.mrn}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (p.dischargedAt != null) {
                    Text(
                        "Discharged",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun NewPatientDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, bed: String?, mrn: String?, diagnosis: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var bed by remember { mutableStateOf("") }
    var mrn by remember { mutableStateOf("") }
    var dx by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Admit patient") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    name, { name = it },
                    label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    bed, { bed = it },
                    label = { Text("Bed (e.g. M-12)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    mrn, { mrn = it },
                    label = { Text("MRN") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    dx, { dx = it },
                    label = { Text("Diagnosis") }, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onCreate(
                        name.trim(),
                        bed.trim().ifBlank { null },
                        mrn.trim().ifBlank { null },
                        dx.trim().ifBlank { null },
                    )
                },
            ) { Text("Admit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
