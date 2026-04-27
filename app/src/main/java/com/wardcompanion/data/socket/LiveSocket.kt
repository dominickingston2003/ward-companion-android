package com.wardcompanion.data.socket

import android.util.Log
import com.wardcompanion.data.local.SettingsStore
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Socket.IO so the rest of the app can react to "patient.changed",
 * "photo.changed", "note.changed" via a Kotlin SharedFlow.
 *
 * The server emits messages like:
 *   { entity: "patient", id: "...", op: "create"|"update"|"delete" }
 */
sealed class LiveEvent {
    data class Patient(val id: String, val op: Op) : LiveEvent()
    data class Photo(val patientId: String, val id: String, val op: Op) : LiveEvent()
    data class Note(val patientId: String, val id: String, val op: Op) : LiveEvent()
    enum class Op { CREATE, UPDATE, DELETE }
}

@Singleton
class LiveSocket @Inject constructor(
    private val settings: SettingsStore,
) {
    private val tag = "LiveSocket"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var socket: Socket? = null

    private val _events = MutableSharedFlow<LiveEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<LiveEvent> = _events.asSharedFlow()

    fun connect() = scope.launch {
        if (socket?.connected() == true) return@launch
        val server = settings.currentServer()
        val token = settings.currentToken() ?: return@launch
        try {
            val opts = IO.Options.builder()
                .setAuth(mapOf("token" to token))
                .setReconnection(true)
                .setReconnectionDelay(1_000)
                .setReconnectionDelayMax(5_000)
                .setTransports(arrayOf("websocket"))
                .build()
            val s = IO.socket(URI.create(server), opts)
            socket = s

            s.on(Socket.EVENT_CONNECT) { Log.i(tag, "connected") }
            s.on(Socket.EVENT_DISCONNECT) { Log.i(tag, "disconnected") }
            s.on("patient.changed") { args -> emit(args, ::parsePatient) }
            s.on("photo.changed")   { args -> emit(args, ::parsePhoto) }
            s.on("note.changed")    { args -> emit(args, ::parseNote) }

            s.connect()
        } catch (t: Throwable) {
            Log.w(tag, "connect failed", t)
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }

    private fun emit(args: Array<Any?>, parser: (org.json.JSONObject) -> LiveEvent?) {
        val obj = args.firstOrNull() as? org.json.JSONObject ?: return
        parser(obj)?.let {
            scope.launch { _events.emit(it) }
        }
    }

    private fun parsePatient(o: org.json.JSONObject): LiveEvent? {
        val id = o.optString("id").ifBlank { return null }
        return LiveEvent.Patient(id, op(o.optString("op")))
    }

    private fun parsePhoto(o: org.json.JSONObject): LiveEvent? {
        val patientId = o.optString("patientId").ifBlank { return null }
        val id = o.optString("id").ifBlank { return null }
        return LiveEvent.Photo(patientId, id, op(o.optString("op")))
    }

    private fun parseNote(o: org.json.JSONObject): LiveEvent? {
        val patientId = o.optString("patientId").ifBlank { return null }
        val id = o.optString("id").ifBlank { return null }
        return LiveEvent.Note(patientId, id, op(o.optString("op")))
    }

    private fun op(s: String): LiveEvent.Op = when (s) {
        "create" -> LiveEvent.Op.CREATE
        "delete" -> LiveEvent.Op.DELETE
        else     -> LiveEvent.Op.UPDATE
    }
}
