package com.rahmatulula.motocare.util

/** Status layar: loading, sukses, atau error (dipakai untuk loading/error state UI). */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

/** Event sekali pakai (mis. pesan snackbar) agar tidak muncul ulang saat layar diputar. */
class Event<out T>(private val content: T) {
    private var handled = false
    fun getIfNotHandled(): T? = if (handled) null else { handled = true; content }
}
