package com.rahmatulula.motocare.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.rahmatulula.motocare.R
import com.rahmatulula.motocare.data.local.ServiceItem
import com.rahmatulula.motocare.databinding.FragmentServiceDetailBinding
import com.rahmatulula.motocare.ui.common.loadPhoto
import com.rahmatulula.motocare.ui.common.visible
import com.rahmatulula.motocare.util.Formatters
import com.rahmatulula.motocare.viewmodel.MotoViewModel

class ServiceDetailFragment : Fragment() {
    private var _b: FragmentServiceDetailBinding? = null
    private val b get() = _b!!
    private val vm: MotoViewModel by activityViewModels()
    private var current: ServiceItem? = null
    private var leaving = false

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentServiceDetailBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val id = requireArguments().getLong("serviceId")

        vm.observeService(id).observe(viewLifecycleOwner) { item ->
            if (item == null) {
                if (!leaving) { leaving = true; findNavController().popBackStack() }
                return@observe
            }
            current = item
            val s = item.service
            b.tvJenis.text = s.jenis
            b.tvCost.text = Formatters.rupiah(s.biaya)
            b.tvMotor.text = "${item.motorNama} • ${item.motorPlat}"
            b.tvDate.text = Formatters.date(s.tanggal)
            b.tvKm.text = Formatters.km(s.odometer)
            b.tvWorkshop.text = s.bengkel
            b.tvNotes.text = s.catatan.ifBlank { getString(R.string.no_notes) }
            b.ivPhoto.loadPhoto(s.fotoPath)
        }

        b.btnEdit.setOnClickListener {
            findNavController().navigate(R.id.serviceFormFragment, bundleOf("serviceId" to id))
        }
        b.btnDelete.setOnClickListener { confirmDelete() }
    }

    /** Hapus dengan dialog konfirmasi. */
    private fun confirmDelete() {
        val item = current ?: return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_service_title)
            .setMessage(getString(R.string.delete_service_msg, item.service.jenis))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                leaving = true
                vm.deleteService(item.service) {
                    if (isAdded) {
                        android.widget.Toast.makeText(requireContext(), R.string.deleted_ok, android.widget.Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
