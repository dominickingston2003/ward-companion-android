package com.wardcompanion.data.api

import com.wardcompanion.data.model.CreateDailyNoteRequest
import com.wardcompanion.data.model.CreatePatientRequest
import com.wardcompanion.data.model.DailyNote
import com.wardcompanion.data.model.HealthResponse
import com.wardcompanion.data.model.LoginRequest
import com.wardcompanion.data.model.LoginResponse
import com.wardcompanion.data.model.Patient
import com.wardcompanion.data.model.PhotoItem
import com.wardcompanion.data.model.UpdateDailyNoteRequest
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface WardApi {

    @GET("/health")
    suspend fun health(): HealthResponse

    // ---- auth ----
    @POST("/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    // ---- patients ----
    @GET("/patients")
    suspend fun listPatients(
        @Query("includeDischarged") includeDischarged: Boolean = false,
    ): List<Patient>

    @GET("/patients/{id}")
    suspend fun getPatient(@Path("id") id: String): Patient

    @POST("/patients")
    suspend fun createPatient(@Body body: CreatePatientRequest): Patient

    @PATCH("/patients/{id}")
    suspend fun updatePatient(
        @Path("id") id: String,
        @Body body: CreatePatientRequest,
    ): Patient

    @PUT("/patients/{id}/discharge")
    suspend fun dischargePatient(@Path("id") id: String): Patient

    @DELETE("/patients/{id}")
    suspend fun deletePatient(@Path("id") id: String)

    // ---- photos ----
    @GET("/patients/{id}/photos")
    suspend fun listPhotos(@Path("id") patientId: String): List<PhotoItem>

    @Multipart
    @POST("/patients/{id}/photos")
    suspend fun uploadPhoto(
        @Path("id") patientId: String,
        @Part file: MultipartBody.Part,
        @Part("takenAt") takenAt: okhttp3.RequestBody,
        @Part("caption") caption: okhttp3.RequestBody?,
    ): PhotoItem

    @DELETE("/photos/{id}")
    suspend fun deletePhoto(@Path("id") id: String)

    // ---- daily notes ----
    @GET("/patients/{id}/notes")
    suspend fun listNotes(@Path("id") patientId: String): List<DailyNote>

    @POST("/notes")
    suspend fun createNote(@Body body: CreateDailyNoteRequest): DailyNote

    @PATCH("/notes/{id}")
    suspend fun updateNote(
        @Path("id") id: String,
        @Body body: UpdateDailyNoteRequest,
    ): DailyNote

    @DELETE("/notes/{id}")
    suspend fun deleteNote(@Path("id") id: String)
}
