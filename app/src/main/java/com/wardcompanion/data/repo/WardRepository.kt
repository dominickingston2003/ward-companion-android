package com.wardcompanion.data.repo

import android.content.ContentResolver
import android.net.Uri
import com.wardcompanion.data.api.WardApi
import com.wardcompanion.data.local.SettingsStore
import com.wardcompanion.data.model.CreateDailyNoteRequest
import com.wardcompanion.data.model.CreatePatientRequest
import com.wardcompanion.data.model.DailyNote
import com.wardcompanion.data.model.LoginRequest
import com.wardcompanion.data.model.Patient
import com.wardcompanion.data.model.PhotoItem
import com.wardcompanion.data.model.UpdateDailyNoteRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WardRepository @Inject constructor(
    private val api: WardApi,
    private val settings: SettingsStore,
    @ApplicationContext private val ctx: android.content.Context,
) {
    suspend fun login(email: String, password: String) {
        val resp = api.login(LoginRequest(email, password))
        settings.saveAuth(resp.token, resp.user.id, resp.user.displayName ?: resp.user.email)
    }

    suspend fun logout() {
        settings.clearAuth()
    }

    // patients
    suspend fun listPatients(includeDischarged: Boolean = false): List<Patient> =
        api.listPatients(includeDischarged)
    suspend fun getPatient(id: String): Patient = api.getPatient(id)
    suspend fun createPatient(req: CreatePatientRequest): Patient = api.createPatient(req)
    suspend fun updatePatient(id: String, req: CreatePatientRequest): Patient =
        api.updatePatient(id, req)
    suspend fun discharge(id: String): Patient = api.dischargePatient(id)
    suspend fun deletePatient(id: String) = api.deletePatient(id)

    // photos
    suspend fun listPhotos(patientId: String): List<PhotoItem> = api.listPhotos(patientId)

    suspend fun uploadPhoto(
        patientId: String,
        uri: Uri,
        takenAt: Long,
        caption: String? = null,
    ): PhotoItem = withContext(Dispatchers.IO) {
        val cr: ContentResolver = ctx.contentResolver
        val mime = cr.getType(uri) ?: "image/jpeg"
        val bytes = cr.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not open photo")
        val body: RequestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val ext = if (mime.contains("png")) "png" else "jpg"
        val part = MultipartBody.Part.createFormData("file", "upload.$ext", body)
        val takenAtPart = takenAt.toString()
            .toRequestBody("text/plain".toMediaTypeOrNull())
        val captionPart = caption?.toRequestBody("text/plain".toMediaTypeOrNull())
        api.uploadPhoto(patientId, part, takenAtPart, captionPart)
    }

    suspend fun deletePhoto(id: String) = api.deletePhoto(id)

    // notes
    suspend fun listNotes(patientId: String): List<DailyNote> = api.listNotes(patientId)
    suspend fun createNote(patientId: String, date: Long, text: String): DailyNote =
        api.createNote(CreateDailyNoteRequest(patientId, date, text))
    suspend fun updateNote(id: String, date: Long, text: String): DailyNote =
        api.updateNote(id, UpdateDailyNoteRequest(date, text))
    suspend fun deleteNote(id: String) = api.deleteNote(id)

    // utility
    suspend fun serverUrl(): String = settings.currentServer().trimEnd('/')

    suspend fun fullPhotoUrl(photo: PhotoItem): String =
        "${serverUrl()}${photo.urlPath}"

    suspend fun authHeader(): String? =
        settings.currentToken()?.let { "Bearer $it" }
}
