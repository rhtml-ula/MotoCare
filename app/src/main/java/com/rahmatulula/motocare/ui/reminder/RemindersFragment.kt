package com.rahmatulula.motocare.ui.reminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.rahmatulula.motocare.R
import com.rahmatulula.motocare.databinding.FragmentRemindersBinding
import com.rahmatulula.motocare.ui.common.ReminderAdapter
import com.rahmatulula.motocare.ui.common.visible
import com.rahmatulula.motocare.viewmodel.MotoViewModel

class RemindersFragment : Fragment() {
    private var _b: FragmentRemindersBinding? = null
    private val b get() = _b!!
    private val vm: MotoViewModel by activityViewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentRemindersBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = ReminderAdapter(
            onClick = { findNavController().navigate(R.id.reminderFormFragment, bundleOf("reminderId" to it.reminder.id)) },
            onToggle = { r, active -> vm.setReminderActive(r, active) }
        )
        b.rvList.layoutManager = LinearLayoutManager(requireContext())
        b.rvList.adapter = adapter
        b.empty.tvEmptyTitle.setText(R.string.reminders_empty_title)
        b.empty.tvEmptyMessage.setText(R.string.reminders_empty_msg)

        vm.reminders.observe(viewLifecycleOwner) { list ->
            b.progress.visible(false)
            adapter.submitList(list)
            b.rvList.visible(list.isNotEmpty())
            b.empty.root.visible(list.isEmpty())
        }

        b.fabAdd.setOnClickListener {
            if (vm.motors.value.isNullOrEmpty()) {
                Snackbar.make(b.root, R.string.need_motor_first, Snackbar.LENGTH_LONG).show()
                findNavController().navigate(R.id.motorFormFragment, bundleOf("motorId" to 0L))
            } else {
                findNavController().navigate(R.id.reminderFormFragment, bundleOf("reminderId" to 0L))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
