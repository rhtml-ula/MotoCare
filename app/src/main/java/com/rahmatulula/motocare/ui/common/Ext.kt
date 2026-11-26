package com.rahmatulula.motocare.ui.common

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.widget.doOnTextChanged
import com.google.android.material.textfield.TextInputLayout

fun TextInputLayout.value(): String = editText?.text?.toString()?.trim().orEmpty()

/** Hapus pesan error otomatis begitu pengguna mulai mengetik. */
fun TextInputLayout.clearErrorOnType() {
    editText?.doOnTextChanged { _, _, _, _ -> error = null }
}

fun View.visible(show: Boolean) {
    visibility = if (show) View.VISIBLE else View.GONE
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun AutoCompleteTextView.setItems(items: List<String>) {
    setAdapter(ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, items))
}
