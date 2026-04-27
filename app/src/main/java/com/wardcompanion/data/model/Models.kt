package com.wardcompanion.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppUser(
    val id: String,
    val username: String,
    val displayName: String? = null,
)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(val token: String, val user: AppUser)

@Serializable
data class Patient(
    val id: String,
    val name: String,
    val bedNumber: String? = null,
    val mrn: String? = null,           // medical record number
    val diagnosis: String? = null,
    val admittedAt: Long? = null,      // epoch ms
    val dischargedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class CreatePatientRequest(
    val name: String,
    val bedNumber: String? = null,
    val mrn: String? = null,
    val diagnosis: String? = null,
)

@Serializable
data class PhotoItem(
    val id: String,
    val patientId: String,
    val filename: String,
    val mimeType: String,
    val width: Int? = null,
    val height: Int? = null,
    val takenAt: Long,
    val uploadedAt: Long,
    val caption: String? = null,
) {
    /** Append to base URL → full URL of the JPEG. */
    val urlPath: String get() = "/photos/$id"
}

@Serializable
data class DailyNote(
    val id: String,
    val patientId: String,
    val date: Long,           // epoch ms — the date the note is *for*
    val text: String,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class CreateDailyNoteRequest(
    val patientId: String,
    val date: Long,
    val text: String,
)

@Serializable
data class UpdateDailyNoteRequest(
    val date: Long,
    val text: String,
)

@Serializable
data class HealthResponse(
    val ok: Boolean,
    @SerialName("version") val version: String? = null,
)
