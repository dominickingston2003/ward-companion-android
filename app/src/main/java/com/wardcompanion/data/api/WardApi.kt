package com.wardcompanion.data.api

import com.wardcompanion.data.model.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface WardApi {

    @GET("/health")
    suspend fun health(): HealthResponse

    @POST("/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("/patients")
    suspend fun listPatients(
        
    ): PatientsResponse

    @GET("/patients/{id}")
    suspend fun getPatient(@Path("id") id: String): PatientResponse

    @POST("/patients")
    suspend fun createPatient(@Body body: CreatePatientRequest): PatientResponse

    @PATCH("/patients/{id}")
    suspend fun updatePatient(
        @Path("id") id: String,
        @Body body: CreatePatientRequest,
    ): PatientResponse

    @PUT("/patients/{id}/discharge")
    suspend fun dischargePatient(@Path("id") id: String): PatientResponse

    @DELETE("/patients/{id}")
    suspend fun deletePatient(@Path("id") id: String)

    @GET("/patients/{id}/photos")
    suspend fun listPhotos(@Path("id") patientId: String): PhotosResponse

    @Multipart
    @POST("/patients/{id}/photos")
    suspend fun uploadPhoto(
        @Path("id") patientId: String,
        @Part file: MultipartBody.Part,
        @Part("takenAt") takenAt: okhttp3.RequestBody,
        @Part("caption") caption: okhttp3.RequestBody?,
    ): PhotoResponse

    @DELETE("/photos/{id}")
    suspend fun deletePhoto(@Path("id") id: String)

    @GET("/patients/{id}/notes")
    suspend fun listNotes(@Path("id") patientId: String): NotesResponse

    @POST("/notes")
    suspend fun createNote(@Body body: CreateDailyNoteRequest): NoteResponse

    @PATCH("/notes/{id}")
    suspend fun updateNote(
        @Path("id") id: String,
        @Body body: UpdateDailyNoteRequest,
    ): NoteResponse

    @DELETE("/notes/{id}")
    suspend fun deleteNote(@Path("id") id: String)
}
