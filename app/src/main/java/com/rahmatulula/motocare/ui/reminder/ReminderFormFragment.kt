package com.rahmatulula.motocare.ui.reminder

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
import com.google.android.material.datepicker.MaterialDatePicker
import com.rahmatulula.motocare.R
import com.rahmatulula.motocare.data.local.Motor
import com.rahmatulula.motocare.data.local.Reminder
import com.rahmatulula.motocare.databinding.FragmentReminderFormBinding
import com.rahmatulula.motocare.ui.common.clearErrorOnType
import com.rahmatulula.motocare.ui.common.hideKeyboard
import com.rahmatulula.motocare.ui.common.setItems
import com.rahmatulula.motocare.ui.common.value
import com.rahmatulula.motocare.ui.common.visible
import com.rahmatulula.motocare.util.Formatters
import com.rahmatulula.motocare.viewmodel.MotoViewModel
import kotlinx.coroutines.launch

class ReminderFormFragment : Fragment() {
    private var _b: FragmentReminderFormBinding? = null
    private val b get() = _b!!
    private val vm: MotoViewModel by activityViewModels()

    private var reminderId = 0L
    private var motorId: Long? = null
    private var dueDate: Long? = null
    private var motors: List<Motor> = emptyList()
    private var existing: Reminder? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentReminderFormBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        reminderId = requireArguments().getLong("reminderId")
        listOf(b.tilMotor, b.tilJenis, b.tilDueDate, b.tilDueKm, b.tilIntMonth, b.tilIntKm).forEach { it.clearErrorOnType() }
        b.ddJenis.setItems(resources.getStringArray(R.array.service_types).toList())
        b.btnDelete.visible(reminderId != 0L)

        if (savedInstanceState != null) {
            motorId = savedInstanceState.getLong("motorId", -1L).takeIf { it >= 0 }
            dueDate = savedInstanceState.getLong("due", -1L).takeIf { it >= 0 }
        } else if (reminderId != 0L) {
            loadExisting()
        }
        showDate()
        if (savedInstanceState != null && reminderId != 0L) {
            viewLifecycleOwner.lifecycleScope.launch { existing = vm.getReminder(reminderId) }
        }

        vm.motors.observe(viewLifecycleOwner) { list ->
            motors = list
            b.ddMotor.setItems(list.map { "${it.nama} (${it.plat})" })
            if (motorId == null && list.size == 1 && reminderId == 0L) motorId = list[0].id
            list.firstOrNull { it.id == motorId }?.let { b.ddMotor.setText("${it.nama} (${it.plat})", false) }
        }
        b.ddMotor.setOnItemClickListener { _, _, pos, _ -> motorId = motors.getOrNull(pos)?.id; b.tilMotor.error = null }

        b.etDueDate.setOnClickListener { pickDate() }
        // Ikon di kanan kolom tanggal = kosongkan tanggal
        b.tilDueDate.setEndIconOnClickListener { dueDate = null; showDate() }
        b.btnSave.setOnClickListener { save() }
        b.btnDelete.setOnClickListener { confirmDelete() }
    }

    private fun loadExisting() {
        viewLifecycleOwner.lifecycleScope.launch {
            val r = vm.getReminder(reminderId) ?: run { findNavController().popBackStack(); return@launch }
            existing = r
            motorId = r.motorId
            dueDate = r.jatuhTempoTanggal
            b.ddJenis.setText(r.jenis, false)
            b.etDueKm.setText(r.jatuhTempoKm?.toString().orEmpty())
            b.etIntMonth.setText(r.intervalBulan?.toString().orEmpty())
            b.etIntKm.setText(r.intervalKm?.toString().orEmpty())
            b.swActive.isChecked = r.aktif
            motors.firstOrNull { it.id == r.motorId }?.let { b.ddMotor.setText("${it.nama} (${it.plat})", false) }
            showDate()
        }
    }

    private fun showDate() { b.etDueDate.setText(dueDate?.let { Formatters.date(it) }.orEmpty()) }

    private fun pickDate() {
        val dp = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.hint_due_date)
            .setSelection(dueDate ?: Formatters.todayUtc())
            .build()
        dp.addOnPositiveButtonClickListener { dueDate = it; b.tilDueDate.error = null; showDate() }
        dp.show(childFragmentManager, "due")
    }

    private fun optInt(raw: String, til: com.google.android.material.textfield.TextInputLayout, min: Int = 1): Pair<Boolean, Int?> {
        if (raw.isEmpty()) return true to null
        val v = raw.toIntOrNull()
        if (v == null || v < min) { til.error = getString(R.string.err_number_positive); return false to null }
        return true to v
    }

    private fun save() {
        b.root.hideKeyboard()
        var ok = true
        val jenis = b.ddJenis.text?.toString()?.trim().orEmpty()
        if (motorId == null) { b.tilMotor.error = getString(R.string.err_motor_required); ok = false }
        if (jenis.isEmpty()) { b.tilJenis.error = getString(R.string.err_type_required); ok = false }

        val (okKm, dueKmRaw) = optInt(b.tilDueKm.value(), b.tilDueKm)
        val (okM, intMonth) = optInt(b.tilIntMonth.value(), b.tilIntMonth)
        val (okIk, intKm) = optInt(b.tilIntKm.value(), b.tilIntKm)
        if (!okKm || !okM || !okIk) ok = false

        // Wajib ada salah satu: tanggal atau km jatuh tempo (boleh dihitung dari interval)
        var dueKm = dueKmRaw
        var date = dueDate
        if (date == null && intMonth != null) date = Formatters.addMonths(Formatters.todayUtc(), intMonth)
        if (dueKm == null && intKm != null) {
            val odo = motors.firstOrNull { it.id == motorId }?.odometer ?: 0
            dueKm = odo + intKm
        }
        if (date == null && dueKm == null) { b.tilDueDate.error = getString(R.string.err_due_required); ok = false }
        if (!ok) return

        b.btnSave.isEnabled = false
        val r = Reminder(
            id = reminderId, userId = "", motorId = motorId!!, jenis = jenis,
            intervalBulan = intMonth, intervalKm = intKm,
            jatuhTempoTanggal = date, jatuhTempoKm = dueKm, aktif = b.swActive.isChecked
        )
        vm.saveReminder(r) {
            if (isAdded) {
                Toast.makeText(requireContext(), R.string.saved_ok, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun confirmDelete() {
        val r = existing ?: return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_reminder_title)
            .setMessage(getString(R.string.delete_reminder_msg, r.jenis))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                vm.deleteReminder(r) {
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
        out.putLong("motorId", motorId ?: -1L)
        out.putLong("due", dueDate ?: -1L)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
