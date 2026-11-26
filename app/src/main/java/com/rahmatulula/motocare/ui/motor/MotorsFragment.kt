package com.rahmatulula.motocare.ui.motor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rahmatulula.motocare.R
import com.rahmatulula.motocare.databinding.FragmentMotorsBinding
import com.rahmatulula.motocare.ui.common.MotorAdapter
import com.rahmatulula.motocare.ui.common.visible
import com.rahmatulula.motocare.viewmodel.MotoViewModel

class MotorsFragment : Fragment() {
    private var _b: FragmentMotorsBinding? = null
    private val b get() = _b!!
    private val vm: MotoViewModel by activityViewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentMotorsBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = MotorAdapter {
            findNavController().navigate(R.id.motorFormFragment, bundleOf("motorId" to it.id))
        }
        b.rvList.layoutManager = LinearLayoutManager(requireContext())
        b.rvList.adapter = adapter
        b.empty.tvEmptyTitle.setText(R.string.motors_empty_title)
        b.empty.tvEmptyMessage.setText(R.string.motors_empty_msg)

        vm.motors.observe(viewLifecycleOwner) { list ->
            b.progress.visible(false)
            adapter.submitList(list)
            b.rvList.visible(list.isNotEmpty())
            b.empty.root.visible(list.isEmpty())
        }
        b.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.motorFormFragment, bundleOf("motorId" to 0L))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
