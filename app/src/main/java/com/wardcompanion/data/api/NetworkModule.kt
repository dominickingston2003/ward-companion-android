package com.wardcompanion.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.wardcompanion.data.local.SettingsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Provides a single Retrofit instance whose base URL is read from
 * SettingsStore. If the user changes the server URL at runtime, the app
 * needs to recreate this — currently a process restart is fine because
 * we only allow this from a settings screen.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides @Singleton
    fun provideOkHttp(settings: SettingsStore): OkHttpClient {
        val log = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)   // photo uploads
            .addInterceptor { chain ->
                val token = runBlocking { settings.currentToken() }
                val req = chain.request().newBuilder()
                    .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(log)
            .build()
    }

    @Provides @Singleton
    fun provideRetrofit(
        settings: SettingsStore,
        client: OkHttpClient,
        json: Json,
    ): Retrofit {
        val baseUrl = runBlocking { settings.currentServer() }
            .let { if (it.endsWith('/')) it else "$it/" }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides @Singleton
    fun provideWardApi(retrofit: Retrofit): WardApi = retrofit.create(WardApi::class.java)
}
