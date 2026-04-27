package com.wardcompanion.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardcompanion.data.local.SettingsStore
import com.wardcompanion.data.repo.WardRepository
import com.wardcompanion.data.socket.LiveSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    data object Loading : AuthState()
    data class LoggedOut(val error: String? = null) : AuthState()
    data class LoggedIn(val username: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: WardRepository,
    private val settings: SettingsStore,
    private val live: LiveSocket,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val token = settings.currentToken()
            val user = settings.username.first()
            _state.value = if (!token.isNullOrBlank()) {
                live.connect()
                AuthState.LoggedIn(user ?: "—")
            } else AuthState.LoggedOut()
        }
    }

    fun login(email: String, password: String, server: String?) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                if (!server.isNullOrBlank()) settings.setServerUrl(server)
                repo.login(email.trim(), password)
                live.connect()
                _state.value = AuthState.LoggedIn(email)
            } catch (t: Throwable) {
                _state.value = AuthState.LoggedOut(error = t.userMessage())
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            live.disconnect()
            repo.logout()
            _state.value = AuthState.LoggedOut()
        }
    }
}

private fun Throwable.userMessage(): String =
    when (this) {
        is retrofit2.HttpException -> when (code()) {
            401 -> "Wrong username or password."
            else -> "Server error (${code()})."
        }
        is java.net.UnknownHostException -> "Can't reach server. Is Tailscale on?"
        is java.net.SocketTimeoutException -> "Server didn't respond."
        else -> message ?: "Something went wrong."
    }
