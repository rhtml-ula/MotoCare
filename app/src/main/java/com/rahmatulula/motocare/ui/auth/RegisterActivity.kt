package com.rahmatulula.motocare.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.rahmatulula.motocare.R
import com.rahmatulula.motocare.databinding.ActivityRegisterBinding
import com.rahmatulula.motocare.ui.MainActivity
import com.rahmatulula.motocare.ui.common.clearErrorOnType
import com.rahmatulula.motocare.ui.common.hideKeyboard
import com.rahmatulula.motocare.ui.common.value
import com.rahmatulula.motocare.ui.common.visible
import com.rahmatulula.motocare.util.UiState
import com.rahmatulula.motocare.viewmodel.AuthViewModel

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val vm: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listOf(binding.tilName, binding.tilEmail, binding.tilPassword, binding.tilConfirm)
            .forEach { it.clearErrorOnType() }

        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.btnToLogin.setOnClickListener { finish() }

        vm.state.observe(this) { state ->
            val loading = state is UiState.Loading
            binding.progress.visible(loading)
            binding.btnRegister.isEnabled = !loading
            when (state) {
                is UiState.Success -> {
                    vm.consumeState()
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity()
                }
                is UiState.Error -> {
                    vm.consumeState()
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                else -> Unit
            }
        }
    }

    private fun attemptRegister() {
        binding.root.hideKeyboard()
        val name = binding.tilName.value()
        val email = binding.tilEmail.value()
        val pass = binding.tilPassword.value()
        val confirm = binding.tilConfirm.value()
        var valid = true
        if (name.length < 3) { binding.tilName.error = getString(R.string.err_name_short); valid = false }
        if (email.isEmpty()) { binding.tilEmail.error = getString(R.string.err_email_empty); valid = false }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.err_email_format); valid = false
        }
        if (pass.length < 6) { binding.tilPassword.error = getString(R.string.err_password_short); valid = false }
        if (confirm != pass) { binding.tilConfirm.error = getString(R.string.err_password_mismatch); valid = false }
        if (valid) vm.register(name, email, pass)
    }
}
