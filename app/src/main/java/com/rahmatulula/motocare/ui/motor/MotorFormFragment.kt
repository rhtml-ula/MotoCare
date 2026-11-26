package com.rahmatulula.motocare.ui.motor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.rahmatulula.motocare.R
import com.rahmatulula.motocare.data.local.Motor
import com.rahmatulula.motocare.databinding.FragmentMotorFormBinding
import com.rahmatulula.motocare.ui.common.clearErrorOnType
import com.rahmatulula.motocare.ui.common.hideKeyboard
import com.rahmatulula.motocare.ui.common.loadPhoto
import com.rahmatulula.motocare.ui.common.value
import com.rahmatulula.motocare.ui.common.visible
import com.rahmatulula.motocare.util.Formatters
import com.rahmatulula.motocare.util.PhotoHelper
import com.rahmatulula.motocare.util.PhotoPickerHelper
import com.rahmatulula.motocare.viewmodel.MotoViewModel
import kotlinx.coroutines.launch

class MotorFormFragment : Fragment() {
    private var _b: FragmentMotorFormBinding? = null
    private val b get() = _b!!
    private val vm: MotoViewModel by activityViewModels()

    private var motorId = 0L
    private var photoPath: String? = null
    private var originalPhoto: String? = null
    private var existing: Motor? = null
    private var saved = false

    private val picker = PhotoPickerHelper(this) { path ->
        if (photoPath != originalPhoto) PhotoHelper.delete(photoPath)
        photoPath = path
        _b?.let { showPhoto() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        picker.restoreState(savedInstanceState)
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentMotorFormBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        motorId = requireArguments().getLong("motorId")
        listOf(b.tilName, b.tilPlate, b.tilYear, b.tilKm).forEach { it.clearErrorOnType() }
        b.btnDelete.visible(motorId != 0L)

        if (savedInstanceState != null) {
            photoPath = savedInstanceState.getString("photo")
            originalPhoto = savedInstanceState.getString("origPhoto")
            if (motorId != 0L) viewLifecycleOwner.lifecycleScope.launch { existing = vm.getMotor(motorId) }
        } else if (motorId != 0L) {
            viewLifecycleOwner.lifecycleScope.launch {
                val m = vm.getMotor(motorId) ?: run { findNavController().popBackStack(); return@launch }
                existing = m
                photoPath = m.fotoPath
                originalPhoto = m.fotoPath
                b.etName.setText(m.nama)
                b.etPlate.setText(m.plat)
                b.etYear.setText(m.tahun.toString())
                b.etKm.setText(m.odometer.toString())
                showPhoto()
            }
        }
        showPhoto()

        b.photo.btnCamera.setOnClickListener { picker.launchCamera() }
        b.photo.btnGallery.setOnClickListener { picker.launchGallery() }
        b.photo.btnRemovePhoto.setOnClickListener {
            if (photoPath != originalPhoto) PhotoHelper.delete(photoPath)
            photoPath = null
            showPhoto()
        }
        b.btnSave.setOnClickListener { save() }
        b.btnDelete.setOnClickListener { confirmDelete() }
    }

    private fun showPhoto() {
        b.photo.ivPreview.loadPhoto(photoPath)
        b.photo.btnRemovePhoto.visible(photoPath != null)
    }

    private fun save() {
        b.root.hideKeyboard()
        var ok = true
        val name = b.tilName.value()
        val plate = b.tilPlate.value()
        val year = b.tilYear.value().toIntOrNull()
        val km = b.tilKm.value().toIntOrNull()
        if (name.length < 2) { b.tilName.error = getString(R.string.err_motor_name); ok = false }
        if (plate.length < 3) { b.tilPlate.error = getString(R.string.err_plate); ok = false }
        if (year == null || year < 1980 || year > Formatters.currentYear() + 1) {
            b.tilYear.error = getString(R.string.err_year, 1980, Formatters.currentYear() + 1); ok = false
        }
        if (km == null) { b.tilKm.error = getString(R.string.err_number_required); ok = false }
        if (!ok) return

        b.btnSave.isEnabled = false
        if (originalPhoto != null && originalPhoto != photoPath) PhotoHelper.delete(originalPhoto)
        val m = Motor(
            id = motorId, userId = "", nama = name, plat = plate.uppercase(),
            tahun = year!!, odometer = km!!, fotoPath = photoPath
        )
        vm.saveMotor(m) {
            saved = true
            if (isAdded) {
                Toast.makeText(requireContext(), R.string.saved_ok, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun confirmDelete() {
        val m = existing ?: return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_motor_title)
            .setMessage(getString(R.string.delete_motor_msg, m.nama))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                saved = true
                vm.deleteMotor(m) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), R.string.deleted_ok, Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        picker.saveState(out)
        out.putString("photo", photoPath)
        out.putString("origPhoto", originalPhoto)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!saved && !requireActivity().isChangingConfigurations && photoPath != originalPhoto) {
            PhotoHelper.delete(photoPath)
        }
    }
}
