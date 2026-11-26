package com.rahmatulula.motocare.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.rahmatulula.motocare.data.repository.AuthRepository
import com.rahmatulula.motocare.util.Event
import com.rahmatulula.motocare.util.UiState

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AuthRepository(app)

    private val _state = MutableLiveData<UiState<Unit>?>(null)
    val state: LiveData<UiState<Unit>?> = _state

    /** (berhasil, pesan) untuk reset password */
    private val _resetResult = MutableLiveData<Event<Pair<Boolean, String?>>>()
    val resetResult: LiveData<Event<Pair<Boolean, String?>>> = _resetResult

    val isLoggedIn: Boolean get() = repo.currentUser != null
    val userName: String get() = repo.currentUser?.displayName.orEmpty()
    val userEmail: String get() = repo.currentUser?.email.orEmpty()

    fun login(email: String, password: String) {
        _state.value = UiState.Loading
        repo.login(email, password) { err ->
            _state.value = if (err == null) UiState.Success(Unit) else UiState.Error(err)
        }
    }

    fun register(name: String, email: String, password: String) {
        _state.value = UiState.Loading
        repo.register(name, email, password) { err ->
            _state.value = if (err == null) UiState.Success(Unit) else UiState.Error(err)
        }
    }

    fun resetPassword(email: String) {
        repo.resetPassword(email) { err -> _resetResult.value = Event((err == null) to err) }
    }

    fun consumeState() { _state.value = null }

    fun logout() = repo.logout()
}
