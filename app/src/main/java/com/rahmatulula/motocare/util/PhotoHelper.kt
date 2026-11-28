
package com.rahmatulula.motocare.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.rahmatulula.motocare.R
import java.io.File

/**
 * Utilitas file foto di penyimpanan internal aplikasi.
 */
object PhotoHelper {

    private fun dir(ctx: Context): File =
        File(ctx.filesDir, "photos").apply {
            mkdirs()
        }

    fun newFile(ctx: Context): File =
        File(dir(ctx), "img_${System.currentTimeMillis()}.jpg")

    fun copyToInternal(ctx: Context, uri: Uri): String? {
        return try {
            val file = newFile(ctx)

            val input = ctx.contentResolver.openInputStream(uri)
                ?: return null

            input.use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            file.absolutePath

        } catch (e: Exception) {
            null
        }
    }

    fun uriFor(ctx: Context, file: File): Uri =
        FileProvider.getUriForFile(
            ctx,
            "${ctx.packageName}.fileprovider",
            file
        )

    fun delete(path: String?) {
        if (!path.isNullOrBlank()) {
            runCatching {
                File(path).delete()
            }
        }
    }
}

/**
 * Kamera + Galeri.
 * Kamera memakai aplikasi kamera sistem (TakePicture).
 * Aplikasi tetap normal walaupun perangkat tidak punya kamera
 * atau izin ditolak.
 *
 * Harus dibuat sebagai properti Fragment (sebelum STARTED).
 */
class PhotoPickerHelper(
    private val fragment: Fragment,
    private val onPicked: (String) -> Unit
) {

    private var pendingPath: String? = null

    private val takePicture =
        fragment.registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { ok ->

            val path = pendingPath
            pendingPath = null

            if (ok && path != null) {
                onPicked(path)
            } else {
                PhotoHelper.delete(path)
            }
        }

    private val pickMedia =
        fragment.registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->

            if (uri != null) {
                val path = PhotoHelper.copyToInternal(
                    fragment.requireContext(),
                    uri
                )

                if (path != null) {
                    onPicked(path)
                } else {
                    Toast.makeText(
                        fragment.requireContext(),
                        R.string.photo_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    fun launchCamera() {
        val ctx = fragment.requireContext()

        try {
            val file = PhotoHelper.newFile(ctx)
            pendingPath = file.absolutePath

            takePicture.launch(
                PhotoHelper.uriFor(ctx, file)
            )

        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                ctx,
                R.string.camera_unavailable,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun launchGallery() {
        pickMedia.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    fun saveState(out: Bundle) {
        out.putString("pending_photo", pendingPath)
    }

    fun restoreState(saved: Bundle?) {
        pendingPath = saved?.getString("pending_photo")
    }
}