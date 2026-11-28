package com.rahmatulula.motocare.ui.profile

import android.Manifest
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.rahmatulula.motocare.BuildConfig
import com.rahmatulula.motocare.R
import com.rahmatulula.motocare.databinding.FragmentProfileBinding
import com.rahmatulula.motocare.ui.auth.LoginActivity
import com.rahmatulula.motocare.util.NotificationHelper
import com.rahmatulula.motocare.util.PhotoHelper
import com.rahmatulula.motocare.util.PhotoPickerHelper
import com.rahmatulula.motocare.util.Prefs
import com.rahmatulula.motocare.viewmodel.AuthViewModel
import com.rahmatulula.motocare.worker.ReminderWorker
import java.io.File

class ProfileFragment : Fragment() {
    private var _b: FragmentProfileBinding? = null
    private val b get() = _b!!
    private val auth: AuthViewModel by activityViewModels()

    private val uid: String get() = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) sendTest()
            else Snackbar.make(b.root, R.string.notif_denied_info, Snackbar.LENGTH_LONG).show()
        }

    // Kamera + galeri untuk foto profil (dibuat sebagai properti agar terdaftar sebelum STARTED)
    private val picker = PhotoPickerHelper(this) { path -> onPhotoPicked(path) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        picker.restoreState(savedInstanceState)
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentProfileBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.tvName.text = auth.userName.ifBlank { getString(R.string.default_user) }
        b.tvEmail.text = auth.userEmail
        b.tvVersion.text = getString(R.string.version_fmt, BuildConfig.VERSION_NAME)

        // ----- Foto profil -----
        showAvatar()
        b.ivAvatar.setOnClickListener { showPhotoOptions() }
        b.btnChangePhoto.setOnClickListener { showPhotoOptions() }

        // ----- Tema (mode gelap) -----
        val checked = when (Prefs.themeMode(requireContext())) {
            AppCompatDelegate.MODE_NIGHT_NO -> R.id.btn_theme_light
            AppCompatDelegate.MODE_NIGHT_YES -> R.id.btn_theme_dark
            else -> R.id.btn_theme_system
        }
        b.toggleTheme.check(checked)
        b.toggleTheme.addOnButtonCheckedListener { _, id, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (id) {
                R.id.btn_theme_light -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.btn_theme_dark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            if (mode != Prefs.themeMode(requireContext())) Prefs.setThemeMode(requireContext(), mode)
        }

        // ----- Tes notifikasi (izin runtime) -----
        b.btnTestNotif.setOnClickListener {
            if (NotificationHelper.canNotify(requireContext())) sendTest()
            else if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        b.btnLogout.setOnClickListener { confirmLogout() }
    }

    /** Tampilkan foto profil bulat; bila belum ada tampilkan ikon orang bawaan. */
    private fun showAvatar() {
        val path = Prefs.profilePhoto(requireContext(), uid)
        if (path != null && File(path).exists()) {
            b.ivAvatar.imageTintList = null
            b.ivAvatar.setPadding(0, 0, 0, 0)
            b.ivAvatar.load(File(path)) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
        } else {
            val pad = (24 * resources.displayMetrics.density).toInt()
            b.ivAvatar.setPadding(pad, pad, pad, pad)
            b.ivAvatar.setImageResource(R.drawable.ic_person)
            b.ivAvatar.imageTintList = ColorStateList.valueOf(Color.WHITE)
        }
    }

    private fun showPhotoOptions() {
        val hasPhoto = Prefs.profilePhoto(requireContext(), uid) != null
        val labels = mutableListOf(getString(R.string.action_camera), getString(R.string.action_gallery))
        if (hasPhoto) labels += getString(R.string.action_remove_photo)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.profile_photo_title)
            .setItems(labels.toTypedArray()) { _, which ->
                when (which) {
                    0 -> picker.launchCamera()
                    1 -> picker.launchGallery()
                    2 -> removePhoto()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun onPhotoPicked(path: String) {
        val ctx = requireContext()
        val old = Prefs.profilePhoto(ctx, uid)
        Prefs.setProfilePhoto(ctx, uid, path)
        if (old != null && old != path) PhotoHelper.delete(old)   // buang foto lama
        _b?.let {
            showAvatar()
            Snackbar.make(it.root, R.string.photo_updated, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun removePhoto() {
        val ctx = requireContext()
        PhotoHelper.delete(Prefs.profilePhoto(ctx, uid))
        Prefs.setProfilePhoto(ctx, uid, null)
        showAvatar()
        Snackbar.make(b.root, R.string.photo_removed, Snackbar.LENGTH_SHORT).show()
    }

    private fun sendTest() {
        ReminderWorker.runNow(requireContext())
        Snackbar.make(b.root, R.string.notif_test_sent, Snackbar.LENGTH_SHORT).show()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.logout_title)
            .setMessage(R.string.logout_message)
            .setPositiveButton(R.string.action_logout) { _, _ ->
                auth.logout()
                startActivity(
                    Intent(requireContext(), LoginActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        picker.saveState(out)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
