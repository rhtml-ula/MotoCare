package com.rahmatulula.motocare.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.rahmatulula.motocare.R
import com.rahmatulula.motocare.databinding.FragmentHistoryBinding
import com.rahmatulula.motocare.ui.common.ServiceAdapter
import com.rahmatulula.motocare.ui.common.setItems
import com.rahmatulula.motocare.ui.common.visible
import com.rahmatulula.motocare.viewmodel.MotoViewModel
import com.rahmatulula.motocare.viewmodel.SortOption

class HistoryFragment : Fragment() {
    private var _b: FragmentHistoryBinding? = null
    private val b get() = _b!!
    private val vm: MotoViewModel by activityViewModels()
    private var motorIds: List<Long?> = listOf(null)

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentHistoryBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = ServiceAdapter { item ->
            findNavController().navigate(R.id.serviceDetailFragment, bundleOf("serviceId" to item.service.id))
        }
        b.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        b.rvHistory.adapter = adapter

        // ----- Pencarian (hasil berubah mengikuti ketikan) -----
        b.searchView.setQuery(vm.currentQuery, false)
        b.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?) = true
            override fun onQueryTextChange(q: String?): Boolean { vm.setQuery(q.orEmpty()); return true }
        })

        // ----- Urutkan -----
        val sortLabels = resources.getStringArray(R.array.sort_options).toList()
        b.ddSort.setItems(sortLabels)
        b.ddSort.setText(sortLabels[vm.currentSort.ordinal], false)
        b.ddSort.setOnItemClickListener { _, _, pos, _ -> vm.setSort(SortOption.values()[pos]) }

        // ----- Filter motor -----
        vm.motors.observe(viewLifecycleOwner) { motors ->
            val all = getString(R.string.all_motors)
            motorIds = listOf<Long?>(null) + motors.map { it.id }
            b.ddMotor.setItems(listOf(all) + motors.map { it.nama })
            val sel = motors.firstOrNull { it.id == vm.currentMotorFilter }
            if (sel == null) vm.setMotorFilter(null)
            b.ddMotor.setText(sel?.nama ?: all, false)
        }
        b.ddMotor.setOnItemClickListener { _, _, pos, _ -> vm.setMotorFilter(motorIds.getOrNull(pos)) }

        // ----- State: loading / empty / not found / list -----
        vm.history.observe(viewLifecycleOwner) { ui ->
            b.progress.visible(false)
            adapter.submitList(ui.items)
            val empty = ui.items.isEmpty()
            b.rvHistory.visible(!empty)
            b.empty.root.visible(empty)
            if (empty) {
                val noData = ui.totalCount == 0
                b.empty.tvEmptyTitle.setText(if (noData) R.string.history_empty_title else R.string.search_not_found_title)
                b.empty.tvEmptyMessage.setText(if (noData) R.string.history_empty_msg else R.string.search_not_found_msg)
            }
        }

        b.fabAdd.setOnClickListener {
            if (vm.motors.value.isNullOrEmpty()) {
                Snackbar.make(b.root, R.string.need_motor_first, Snackbar.LENGTH_LONG).show()
                findNavController().navigate(R.id.motorFormFragment, bundleOf("motorId" to 0L))
            } else {
                findNavController().navigate(R.id.serviceFormFragment, bundleOf("serviceId" to 0L))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
