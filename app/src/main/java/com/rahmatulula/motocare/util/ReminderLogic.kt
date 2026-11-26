package com.rahmatulula.motocare.util

import com.rahmatulula.motocare.data.local.ReminderItem

enum class ReminderStatus { OVERDUE, SOON, OK, INACTIVE }

/** Aturan status pengingat: terlambat / segera / aman. */
object ReminderLogic {
    const val SOON_DAYS = 7
    const val SOON_KM = 200

    fun status(item: ReminderItem): ReminderStatus {
        val r = item.reminder
        if (!r.aktif) return ReminderStatus.INACTIVE
        val days = r.jatuhTempoTanggal?.let { Formatters.daysUntil(it) }
        val kmLeft = r.jatuhTempoKm?.let { it - item.motorOdometer }
        if ((days != null && days < 0) || (kmLeft != null && kmLeft <= 0)) return ReminderStatus.OVERDUE
        if ((days != null && days <= SOON_DAYS) || (kmLeft != null && kmLeft <= SOON_KM)) return ReminderStatus.SOON
        return ReminderStatus.OK
    }

    /** Skor urut: yang paling mendesak muncul lebih dulu. */
    fun urgency(item: ReminderItem): Long {
        val r = item.reminder
        val days = r.jatuhTempoTanggal?.let { Formatters.daysUntil(it) } ?: Long.MAX_VALUE
        val kmLeft = r.jatuhTempoKm?.let { (it - item.motorOdometer) / 30L } ?: Long.MAX_VALUE
        return minOf(days, kmLeft)
    }
}
