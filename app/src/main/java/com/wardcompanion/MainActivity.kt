package com.wardcompanion

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wardcompanion.ui.auth.AuthState
import com.wardcompanion.ui.auth.AuthViewModel
import com.wardcompanion.ui.auth.LoginScreen
import com.wardcompanion.ui.notes.DailyNoteEditorScreen
import com.wardcompanion.ui.patients.PatientDetailScreen
import com.wardcompanion.ui.patients.PatientListScreen
import com.wardcompanion.ui.photos.PhotoViewerScreen
import com.wardcompanion.ui.theme.WardCompanionTheme
import com.wardcompanion.util.BiometricLock
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WardCompanionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

object Routes {
    const val PATIENTS = "patients"
    const val PATIENT_DETAIL = "patient/{id}"
    const val PHOTO_VIEWER = "photo/{patientId}/{index}"
    const val NOTE_EDITOR = "note_editor/{patientId}?noteId={noteId}"
    fun patientDetail(id: String) = "patient/$id"
    fun photoViewer(patientId: String, index: Int) = "photo/$patientId/$index"
    fun noteEditor(patientId: String, noteId: String? = null) =
        "note_editor/$patientId" + (noteId?.let { "?noteId=$it" } ?: "")
}

@Composable
fun AppRoot() {
    val authVm: AuthViewModel = hiltViewModel()
    val state by authVm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val activity = LocalContext.current as? MainActivity
    var unlocked by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is AuthState.LoggedIn && !unlocked && activity != null) {
            if (BiometricLock.isAvailable(activity)) {
                BiometricLock.authenticate(
                    activity,
                    onSuccess = { unlocked = true },
                    onCancel = {},
                )
            } else {
                unlocked = true
            }
        }
        if (state is AuthState.LoggedOut) unlocked = false
    }

    when (val s = state) {
        AuthState.Loading -> {}
        is AuthState.LoggedOut -> LoginScreen(
            initialError = s.error,
            onLogin = authVm::login,
        )
        is AuthState.LoggedIn -> {
            if (unlocked) AppNavHost(nav, onLogout = authVm::logout)
            else LockScreen(
                onUnlock = {
                    val a = activity ?: return@LockScreen
                    BiometricLock.authenticate(a, onSuccess = { unlocked = true })
                },
                onSignOut = authVm::logout,
            )
        }
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit, onSignOut: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp),
            )
            Text("Locked", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Unlock to view patient data.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onUnlock) { Text("Unlock") }
            TextButton(onClick = onSignOut) { Text("Sign out") }
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController, onLogout: () -> Unit) {
    NavHost(navController = nav, startDestination = Routes.PATIENTS) {
        composable(Routes.PATIENTS) {
            PatientListScreen(
                onPatient = { id -> nav.navigate(Routes.patientDetail(id)) },
                onLogout = onLogout,
            )
        }
        composable(Routes.PATIENT_DETAIL,
            arguments = listOf(
                androidx.navigation.navArgument("id") {
                    type = androidx.navigation.NavType.StringType
                },
            ),
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            PatientDetailScreen(
                patientId = id,
                onBack = { nav.popBackStack() },
                onPhoto = { idx -> nav.navigate(Routes.photoViewer(id, idx)) },
                onAddNote = { nav.navigate(Routes.noteEditor(id)) },
                onEditNote = { noteId -> nav.navigate(Routes.noteEditor(id, noteId)) },
            )
        }
        composable(Routes.PHOTO_VIEWER,
            arguments = listOf(
                androidx.navigation.navArgument("patientId") {
                    type = androidx.navigation.NavType.StringType
                },
                androidx.navigation.navArgument("index") {
                    type = androidx.navigation.NavType.StringType
                },
            ),
        ) { entry ->
            val patientId = entry.arguments?.getString("patientId") ?: return@composable
            val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
            PhotoViewerScreen(
                patientId = patientId,
                initialIndex = index,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.NOTE_EDITOR,
            arguments = listOf(
                androidx.navigation.navArgument("patientId") {
                    type = androidx.navigation.NavType.StringType
                },
                androidx.navigation.navArgument("noteId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val patientId = entry.arguments?.getString("patientId") ?: return@composable
            val noteId = entry.arguments?.getString("noteId")
            DailyNoteEditorScreen(
                patientId = patientId,
                noteId = noteId,
                onDone = { nav.popBackStack() },
            )
        }
    }
}
