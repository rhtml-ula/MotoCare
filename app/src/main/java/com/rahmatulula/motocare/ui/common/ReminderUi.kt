package com.rahmatulula.motocare.ui.common

import android.content.Context
import androidx.core.content.ContextCompat
import com.rahmatulula.motocare.R
import com.rahmatulula.motocare.data.local.ReminderItem
import com.rahmatulula.motocare.util.Formatters
import com.rahmatulula.motocare.util.ReminderStatus
import kotlin.math.abs

object ReminderUi {
    fun statusLabel(ctx: Context, s: ReminderStatus): String = ctx.getString(
        when (s) {
            ReminderStatus.OVERDUE -> R.string.status_overdue
            ReminderStatus.SOON -> R.string.status_soon
            ReminderStatus.OK -> R.string.status_ok
            ReminderStatus.INACTIVE -> R.string.status_inactive
        }
    )

    fun statusColor(ctx: Context, s: ReminderStatus): Int = ContextCompat.getColor(
        ctx,
        when (s) {
            ReminderStatus.OVERDUE -> R.color.status_overdue
            ReminderStatus.SOON -> R.color.status_soon
            ReminderStatus.OK -> R.color.status_ok
            ReminderStatus.INACTIVE -> android.R.color.darker_gray
        }
    )

    /** Contoh: "12 Des 2026 (5 hari lagi) • 12.000 km (sisa 300 km)" */
    fun dueText(ctx: Context, item: ReminderItem): String {
        val parts = mutableListOf<String>()
        item.reminder.jatuhTempoTanggal?.let {
            val d = Formatters.daysUntil(it)
            val rel = when {
                d > 0 -> ctx.getString(R.string.days_left, d)
                d == 0L -> ctx.getString(R.string.due_today)
                else -> ctx.getString(R.string.days_late, abs(d))
            }
            parts += ctx.getString(R.string.due_date_fmt, Formatters.date(it), rel)
        }
        item.reminder.jatuhTempoKm?.let {
            val left = it - item.motorOdometer
            val rel = if (left > 0) ctx.getString(R.string.km_left, Formatters.km(left))
            else ctx.getString(R.string.km_over, Formatters.km(-left))
            parts += ctx.getString(R.string.due_km_fmt, Formatters.km(it), rel)
        }
        return parts.joinToString("\n")
    }
}
