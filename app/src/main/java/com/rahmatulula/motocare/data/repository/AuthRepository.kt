package com.rahmatulula.motocare.data.repository

import android.content.Context
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.rahmatulula.motocare.R

/**
 * Firebase Authentication (email + password).
 * Sesi disimpan otomatis oleh Firebase SDK, jadi pengguna tidak perlu login ulang.
 */
class AuthRepository(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    fun login(email: String, password: String, onResult: (String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { t ->
            onResult(if (t.isSuccessful) null else errorMessage(t.exception))
        }
    }

    fun register(name: String, email: String, password: String, onResult: (String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { t ->
            if (!t.isSuccessful) {
                onResult(errorMessage(t.exception))
                return@addOnCompleteListener
            }
            val req = UserProfileChangeRequest.Builder().setDisplayName(name).build()
            t.result?.user?.updateProfile(req)?.addOnCompleteListener { onResult(null) } ?: onResult(null)
        }
    }

    fun resetPassword(email: String, onResult: (String?) -> Unit) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener { t ->
            onResult(if (t.isSuccessful) null else errorMessage(t.exception))
        }
    }

    fun logout() = auth.signOut()

    /** Pesan error yang jelas untuk pengguna (bukan stack trace). */
    private fun errorMessage(e: Exception?): String = context.getString(
        when (e) {
            is FirebaseNetworkException -> R.string.err_network
            is FirebaseAuthInvalidUserException -> R.string.err_user_not_found
            is FirebaseAuthInvalidCredentialsException -> R.string.err_wrong_credentials
            is FirebaseAuthUserCollisionException -> R.string.err_email_used
            is FirebaseAuthWeakPasswordException -> R.string.err_weak_password
            is FirebaseTooManyRequestsException -> R.string.err_too_many
            else -> R.string.err_generic
        }
    )
}
