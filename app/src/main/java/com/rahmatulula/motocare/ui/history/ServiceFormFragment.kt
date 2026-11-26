package com.rahmatulula.motocare.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.rahmatulula.motocare.R
import com.rahmatulula.motocare.data.local.Motor
import com.rahmatulula.motocare.data.local.ServiceRecord
import com.rahmatulula.motocare.databinding.FragmentServiceFormBinding
import com.rahmatulula.motocare.ui.common.clearErrorOnType
import com.rahmatulula.motocare.ui.common.hideKeyboard
import com.rahmatulula.motocare.ui.common.loadPhoto
import com.rahmatulula.motocare.ui.common.setItems
import com.rahmatulula.motocare.ui.common.value
import com.rahmatulula.motocare.ui.common.visible
import com.rahmatulula.motocare.util.Formatters
import com.rahmatulula.motocare.util.PhotoHelper
import com.rahmatulula.motocare.util.PhotoPickerHelper
import com.rahmatulula.motocare.viewmodel.MotoViewModel
import kotlinx.coroutines.launch

/** Form tambah (serviceId = 0) atau edit servis. */
class ServiceFormFragment : Fragment() {
    private var _b: FragmentServiceFormBinding? = null
    private val b get() = _b!!
    private val vm: MotoViewModel by activityViewModels()

    private var serviceId = 0L
    private var motorId: Long? = null
    private var dateUtc: Long? = null
    private var photoPath: String? = null
    private var originalPhoto: String? = null
    private var motors: List<Motor> = emptyList()
    private var saved = false

    private val picker = PhotoPickerHelper(this) { path ->
        // Ganti foto: hapus foto sementara sebelumnya (kecuali foto asli milik data)
        if (photoPath != originalPhoto) PhotoHelper.delete(photoPath)
        photoPath = path
        _b?.let { showPhoto() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        picker.restoreState(savedInstanceState)
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentServiceFormBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        serviceId = requireArguments().getLong("serviceId")
        listOf(b.tilMotor, b.tilJenis, b.tilDate, b.tilKm, b.tilCost, b.tilWorkshop).forEach { it.clearErrorOnType() }

        b.ddJenis.setItems(resources.getStringArray(R.array.service_types).toList())

        if (savedInstanceState != null) {
            motorId = savedInstanceState.getLong("motorId", -1L).takeIf { it >= 0 }
            dateUtc = savedInstanceState.getLong("date", -1L).takeIf { it >= 0 }
            photoPath = savedInstanceState.getString("photo")
            originalPhoto = savedInstanceState.getString("origPhoto")
        } else if (serviceId != 0L) {
            loadExisting()
        } else {
            dateUtc = Formatters.todayUtc()
        }
        showDate()
        showPhoto()

        vm.motors.observe(viewLifecycleOwner) { list ->
            motors = list
            b.ddMotor.setItems(list.map { "${it.nama} (${it.plat})" })
            if (motorId == null && list.size == 1 && serviceId == 0L) motorId = list[0].id
            list.firstOrNull { it.id == motorId }?.let { b.ddMotor.setText("${it.nama} (${it.plat})", false) }
        }
        b.ddMotor.setOnItemClickListener { _, _, pos, _ ->
            motorId = motors.getOrNull(pos)?.id
            b.tilMotor.error = null
        }

        b.etDate.setOnClickListener { showDatePicker() }
        b.photo.btnCamera.setOnClickListener { picker.launchCamera() }
        b.photo.btnGallery.setOnClickListener { picker.launchGallery() }
        b.photo.btnRemovePhoto.setOnClickListener {
            if (photoPath != originalPhoto) PhotoHelper.delete(photoPath)
            photoPath = null
            showPhoto()
        }
        b.btnSave.setOnClickListener { save() }
    }

    private fun loadExisting() {
        viewLifecycleOwner.lifecycleScope.launch {
            val s = vm.getService(serviceId) ?: run { findNavController().popBackStack(); return@launch }
            motorId = s.motorId
            dateUtc = s.tanggal
            photoPath = s.fotoPath
            originalPhoto = s.fotoPath
            b.ddJenis.setText(s.jenis, false)
            b.etKm.setText(s.odometer.toString())
            b.etCost.setText(s.biaya.toString())
            b.etWorkshop.setText(s.bengkel)
            b.etNotes.setText(s.catatan)
            motors.firstOrNull { it.id == s.motorId }?.let { b.ddMotor.setText("${it.nama} (${it.plat})", false) }
            showDate()
            showPhoto()
        }
    }

    private fun showDate() { b.etDate.setText(dateUtc?.let { Formatters.date(it) }.orEmpty()) }

    private fun showPhoto() {
        b.photo.ivPreview.loadPhoto(photoPath)
        b.photo.btnRemovePhoto.visible(photoPath != null)
    }

    private fun showDatePicker() {
        val dp = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.hint_date)
            .setSelection(dateUtc ?: Formatters.todayUtc())
            .build()
        dp.addOnPositiveButtonClickListener { dateUtc = it; b.tilDate.error = null; showDate() }
        dp.show(childFragmentManager, "date")
    }

    private fun save() {
        b.root.hideKeyboard()
        var ok = true
        val jenis = b.ddJenis.text?.toString()?.trim().orEmpty()
        val km = b.tilKm.value().toIntOrNull()
        val cost = b.tilCost.value().toLongOrNull()
        val workshop = b.tilWorkshop.value()

        if (motorId == null) { b.tilMotor.error = getString(R.string.err_motor_required); ok = false }
        if (jenis.isEmpty()) { b.tilJenis.error = getString(R.string.err_type_required); ok = false }
        if (dateUtc == null) { b.tilDate.error = getString(R.string.err_date_required); ok = false }
        else if (dateUtc!! > Formatters.todayUtc()) { b.tilDate.error = getString(R.string.err_date_future); ok = false }
        if (km == null) { b.tilKm.error = getString(R.string.err_number_required); ok = false }
        if (cost == null) { b.tilCost.error = getString(R.string.err_number_required); ok = false }
        if (workshop.isEmpty()) { b.tilWorkshop.error = getString(R.string.err_workshop_required); ok = false }
        if (!ok) return

        b.btnSave.isEnabled = false
        val record = ServiceRecord(
            id = serviceId, userId = "", motorId = motorId!!, jenis = jenis, tanggal = dateUtc!!,
            odometer = km!!, biaya = cost!!, bengkel = workshop,
            catatan = b.tilNotes.value(), fotoPath = photoPath
        )
        // Foto lama yang diganti dihapus dari penyimpanan
        if (originalPhoto != null && originalPhoto != photoPath) PhotoHelper.delete(originalPhoto)
        vm.saveService(record) {
            saved = true
            if (isAdded) {
                Toast.makeText(requireContext(), R.string.saved_ok, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        picker.saveState(out)
        out.putLong("motorId", motorId ?: -1L)
        out.putLong("date", dateUtc ?: -1L)
        out.putString("photo", photoPath)
        out.putString("origPhoto", originalPhoto)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Batal tanpa menyimpan: buang foto baru yang belum dipakai
        if (!saved && !requireActivity().isChangingConfigurations && photoPath != originalPhoto) {
            PhotoHelper.delete(photoPath)
        }
    }
}
