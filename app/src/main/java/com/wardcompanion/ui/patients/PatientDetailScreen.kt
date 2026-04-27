package com.wardcompanion.ui.patients

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wardcompanion.data.model.DailyNote
import com.wardcompanion.data.model.PhotoItem
import com.wardcompanion.util.formatDay
import com.wardcompanion.util.formatRelative

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: String,
    onBack: () -> Unit,
    onPhoto: (Int) -> Unit,
    onAddNote: () -> Unit,
    onEditNote: (String) -> Unit,
    vm: PatientDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(patientId) { vm.start(patientId) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(0) }
    var photoToDelete by remember { mutableStateOf<PhotoItem?>(null) }
    var noteToDelete by remember { mutableStateOf<DailyNote?>(null) }

    val pickPhotos = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris -> if (uris.isNotEmpty()) vm.uploadPhotos(uris) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui.patient?.name ?: "Patient") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            if (tab == 0) {
                ExtendedFloatingActionButton(
                    text = { Text(if (ui.uploading > 0) "Uploading… ${ui.uploading}" else "Add photos") },
                    icon = { Icon(Icons.Default.AddAPhoto, contentDescription = null) },
                    onClick = {
                        pickPhotos.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    },
                )
            } else {
                ExtendedFloatingActionButton(
                    text = { Text("New note") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = onAddNote,
                )
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (ui.patient != null) PatientHeader(ui)

            SecondaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Photos (${ui.photos.size})") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Notes (${ui.notes.size})") })
            }

            when {
                ui.loading && ui.photos.isEmpty() && ui.notes.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                tab == 0 -> PhotoGrid(
                    ui = ui,
                    onPhoto = onPhoto,
                    onLongPress = { photoToDelete = it },
                )
                else -> NotesList(
                    notes = ui.notes,
                    onEdit = onEditNote,
                    onLongPress = { noteToDelete = it },
                )
            }
        }
    }

    photoToDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("Delete photo?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.deletePhoto(p.id); photoToDelete = null }) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { photoToDelete = null }) { Text("Cancel") } },
        )
    }
    noteToDelete?.let { n ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete note?") },
            text = { Text("Note for ${formatDay(n.date)} will be removed.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteNote(n.id); noteToDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { noteToDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PatientHeader(ui: PatientDetailUi) {
    val p = ui.patient ?: return
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (!p.bedNumber.isNullOrBlank() || !p.mrn.isNullOrBlank()) {
            Text(
                buildString {
                    if (!p.bedNumber.isNullOrBlank()) append("Bed ${p.bedNumber}")
                    if (!p.mrn.isNullOrBlank()) {
                        if (isNotEmpty()) append("  ·  ")
                        append("MRN ${p.mrn}")
                    }
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!p.diagnosis.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(p.diagnosis, style = MaterialTheme.typography.bodyLarge)
        }
        if (p.admittedAt != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Admitted ${formatRelative(p.admittedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
    }
}

@Composable
private fun PhotoGrid(
    ui: PatientDetailUi,
    onPhoto: (Int) -> Unit,
    onLongPress: (PhotoItem) -> Unit,
) {
    if (ui.photos.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No photos yet. Tap + to add case-sheet pictures.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        itemsIndexed(ui.photos, key = { _, p -> p.id }) { idx, p ->
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val req = ImageRequest.Builder(ctx)
                .data("${ui.baseUrl.trimEnd('/')}${p.urlPath}")
                .apply {
                    ui.authHeader?.let { addHeader("Authorization", it) }
                }
                .crossfade(true)
                .build()
            Box(
                Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPhoto(idx) },
            ) {
                AsyncImage(
                    model = req,
                    contentDescription = p.caption ?: "photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                IconButton(
                    onClick = { onLongPress(p) },
                    modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete photo",
                        tint = MaterialTheme.colorScheme.surface,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesList(
    notes: List<DailyNote>,
    onEdit: (String) -> Unit,
    onLongPress: (DailyNote) -> Unit,
) {
    if (notes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No daily notes yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(notes, key = { it.id }) { n ->
            ElevatedCard(Modifier.fillMaxWidth().clickable { onEdit(n.id) }) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            formatDay(n.date),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onEdit(n.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { onLongPress(n) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    Text(n.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
