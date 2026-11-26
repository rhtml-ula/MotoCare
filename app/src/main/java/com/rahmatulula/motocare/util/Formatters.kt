package com.rahmatulula.motocare.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Semua tanggal disimpan sebagai "UTC midnight millis" supaya konsisten dengan
 * MaterialDatePicker dan tidak bergeser karena zona waktu.
 */
object Formatters {
    private val localeId = Locale("id", "ID")
    private val utc = TimeZone.getTimeZone("UTC")
    const val DAY_MS = 86_400_000L

    fun rupiah(value: Long): String = "Rp " + NumberFormat.getInstance(localeId).format(value)

    fun km(value: Int): String = NumberFormat.getInstance(localeId).format(value) + " km"

    fun date(utcMillis: Long): String =
        SimpleDateFormat("dd MMM yyyy", localeId).apply { timeZone = utc }.format(Date(utcMillis))

    /** UTC-midnight untuk "hari ini" menurut zona waktu perangkat. */
    fun todayUtc(): Long {
        val local = Calendar.getInstance()
        return Calendar.getInstance(utc).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    fun addMonths(utcMillis: Long, months: Int): Long =
        Calendar.getInstance(utc).apply {
            timeInMillis = utcMillis
            add(Calendar.MONTH, months)
        }.timeInMillis

    fun daysUntil(utcMillis: Long): Long = Math.floorDiv(utcMillis - todayUtc(), DAY_MS)

    fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
}
