package com.rahmatulula.motocare.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rahmatulula.motocare.R
import com.rahmatulula.motocare.data.local.Motor
import com.rahmatulula.motocare.data.local.Reminder
import com.rahmatulula.motocare.data.local.ReminderItem
import com.rahmatulula.motocare.data.local.ServiceItem
import com.rahmatulula.motocare.databinding.ItemMotorBinding
import com.rahmatulula.motocare.databinding.ItemReminderBinding
import com.rahmatulula.motocare.databinding.ItemServiceBinding
import com.rahmatulula.motocare.util.Formatters
import com.rahmatulula.motocare.util.ReminderLogic
import com.rahmatulula.motocare.util.ReminderStatus
import java.io.File

/** Memuat foto dari path file internal; bila kosong tampilkan ikon motor. */
fun android.widget.ImageView.loadPhoto(path: String?) {
    if (path.isNullOrBlank() || !File(path).exists()) {
        setImageResource(R.drawable.ic_motor)
        scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
        setPadding(16, 16, 16, 16)
    } else {
        setPadding(0, 0, 0, 0)
        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        load(File(path)) { crossfade(true) }
    }
}

class ServiceAdapter(private val onClick: (ServiceItem) -> Unit) :
    ListAdapter<ServiceItem, ServiceAdapter.VH>(object : DiffUtil.ItemCallback<ServiceItem>() {
        override fun areItemsTheSame(a: ServiceItem, b: ServiceItem) = a.service.id == b.service.id
        override fun areContentsTheSame(a: ServiceItem, b: ServiceItem) = a == b
    }) {

    inner class VH(val b: ItemServiceBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemServiceBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = getItem(pos)
        val s = item.service
        h.b.tvJenis.text = s.jenis
        h.b.tvCost.text = Formatters.rupiah(s.biaya)
        h.b.tvMotor.text = "${item.motorNama} • ${item.motorPlat}"
        h.b.tvMeta.text = "${Formatters.date(s.tanggal)} • ${Formatters.km(s.odometer)}"
        h.b.ivPhoto.loadPhoto(s.fotoPath)
        h.b.root.setOnClickListener { onClick(item) }
    }
}

class ReminderAdapter(
    private val onClick: (ReminderItem) -> Unit,
    private val onToggle: ((Reminder, Boolean) -> Unit)? = null   // null = mode baca saja (beranda)
) : ListAdapter<ReminderItem, ReminderAdapter.VH>(object : DiffUtil.ItemCallback<ReminderItem>() {
    override fun areItemsTheSame(a: ReminderItem, b: ReminderItem) = a.reminder.id == b.reminder.id
    override fun areContentsTheSame(a: ReminderItem, b: ReminderItem) = a == b
}) {

    inner class VH(val b: ItemReminderBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemReminderBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = getItem(pos)
        val ctx = h.b.root.context
        val status: ReminderStatus = ReminderLogic.status(item)
        h.b.tvJenis.text = item.reminder.jenis
        h.b.tvMotor.text = item.motorNama
        h.b.tvDue.text = ReminderUi.dueText(ctx, item)
        h.b.tvStatus.text = ReminderUi.statusLabel(ctx, status)
        h.b.tvStatus.setTextColor(ReminderUi.statusColor(ctx, status))
        h.b.swActive.visible(onToggle != null)
        h.b.swActive.setOnCheckedChangeListener(null)
        h.b.swActive.isChecked = item.reminder.aktif
        h.b.swActive.setOnCheckedChangeListener { _, checked -> onToggle?.invoke(item.reminder, checked) }
        h.b.root.setOnClickListener { onClick(item) }
    }
}

class MotorAdapter(private val onClick: (Motor) -> Unit) :
    ListAdapter<Motor, MotorAdapter.VH>(object : DiffUtil.ItemCallback<Motor>() {
        override fun areItemsTheSame(a: Motor, b: Motor) = a.id == b.id
        override fun areContentsTheSame(a: Motor, b: Motor) = a == b
    }) {

    inner class VH(val b: ItemMotorBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemMotorBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val m = getItem(pos)
        h.b.tvName.text = m.nama
        h.b.tvPlate.text = m.plat
        h.b.tvInfo.text = h.b.root.context.getString(R.string.motor_info_fmt, m.tahun, Formatters.km(m.odometer))
        h.b.ivPhoto.loadPhoto(m.fotoPath)
        h.b.root.setOnClickListener { onClick(m) }
    }
}
