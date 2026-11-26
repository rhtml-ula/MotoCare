package com.rahmatulula.motocare.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.rahmatulula.motocare.R
import com.rahmatulula.motocare.databinding.ActivityLoginBinding
import com.rahmatulula.motocare.ui.MainActivity
import com.rahmatulula.motocare.ui.common.clearErrorOnType
import com.rahmatulula.motocare.ui.common.hideKeyboard
import com.rahmatulula.motocare.ui.common.value
import com.rahmatulula.motocare.ui.common.visible
import com.rahmatulula.motocare.util.UiState
import com.rahmatulula.motocare.viewmodel.AuthViewModel

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val vm: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sesi tersimpan: langsung ke beranda tanpa login ulang
        if (vm.isLoggedIn) {
            goToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tilEmail.clearErrorOnType()
        binding.tilPassword.clearErrorOnType()

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.btnToRegister.setOnClickListener { startActivity(Intent(this, RegisterActivity::class.java)) }
        binding.btnForgot.setOnClickListener { showForgotDialog() }

        vm.state.observe(this) { state ->
            val loading = state is UiState.Loading
            binding.progress.visible(loading)
            binding.btnLogin.isEnabled = !loading
            when (state) {
                is UiState.Success -> { vm.consumeState(); goToMain() }
                is UiState.Error -> {
                    vm.consumeState()
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                else -> Unit
            }
        }

        vm.resetResult.observe(this) { event ->
            event.getIfNotHandled()?.let { (ok, err) ->
                val msg = if (ok) getString(R.string.reset_sent) else err.orEmpty()
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun attemptLogin() {
        binding.root.hideKeyboard()
        val email = binding.tilEmail.value()
        val pass = binding.tilPassword.value()
        var valid = true
        if (email.isEmpty()) { binding.tilEmail.error = getString(R.string.err_email_empty); valid = false }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.err_email_format); valid = false
        }
        if (pass.isEmpty()) { binding.tilPassword.error = getString(R.string.err_password_empty); valid = false }
        if (valid) vm.login(email, pass)
    }

    private fun showForgotDialog() {
        val til = TextInputLayout(this).apply {
            hint = getString(R.string.hint_email)
            setPadding(48, 24, 48, 0)
        }
        val et = TextInputEditText(til.context).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(binding.tilEmail.value())
        }
        til.addView(et)
        AlertDialog.Builder(this)
            .setTitle(R.string.forgot_title)
            .setMessage(R.string.forgot_message)
            .setView(til)
            .setPositiveButton(R.string.action_send) { _, _ ->
                val email = et.text?.toString()?.trim().orEmpty()
                if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) vm.resetPassword(email)
                else Snackbar.make(binding.root, R.string.err_email_format, Snackbar.LENGTH_LONG).show()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
