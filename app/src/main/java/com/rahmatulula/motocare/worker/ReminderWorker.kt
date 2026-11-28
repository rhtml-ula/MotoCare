package com.rahmatulula.motocare.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.auth.FirebaseAuth
import com.rahmatulula.motocare.R
import com.rahmatulula.motocare.data.local.AppDatabase
import com.rahmatulula.motocare.data.repository.MotoRepository
import com.rahmatulula.motocare.util.Formatters
import com.rahmatulula.motocare.util.NotificationHelper
import com.rahmatulula.motocare.util.Prefs
import com.rahmatulula.motocare.util.ReminderLogic
import com.rahmatulula.motocare.util.ReminderStatus
import java.util.concurrent.TimeUnit

/** Pekerjaan terjadwal: cek pengingat jatuh tempo lalu kirim notifikasi lokal. */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val force = inputData.getBoolean(KEY_FORCE, false)
        val repo = MotoRepository(AppDatabase.get(applicationContext), uid)
        val today = Formatters.todayUtc() / Formatters.DAY_MS

        var sent = 0
        repo.activeReminders()
            .map { it to ReminderLogic.status(it) }
            .filter { it.second == ReminderStatus.OVERDUE || it.second == ReminderStatus.SOON }
            .forEach { (item, status) ->
                // Satu notifikasi per pengingat per hari untuk tiap status (Segera / Terlambat).
                // Jika status naik dari Segera ke Terlambat di hari yang sama, tetap dikirim.
                val stamp = today * 10 + status.ordinal
                if (!force && Prefs.lastNotifiedDay(applicationContext, item.reminder.id) == stamp) return@forEach
                val body = buildBody(item.motorNama, item.reminder.jenis, status)
                val title = applicationContext.getString(
                    if (status == ReminderStatus.OVERDUE) R.string.notif_title_overdue else R.string.notif_title_soon
                )
                if (NotificationHelper.show(applicationContext, item.reminder.id.toInt(), title, body)) {
                    Prefs.setNotifiedDay(applicationContext, item.reminder.id, stamp)
                    sent++
                }
            }

        if (force && sent == 0) {
            NotificationHelper.show(
                applicationContext, 9999,
                applicationContext.getString(R.string.app_name),
                applicationContext.getString(R.string.notif_test_body)
            )
        }
        return Result.success()
    }

    private fun buildBody(motor: String, jenis: String, status: ReminderStatus): String =
        applicationContext.getString(
            if (status == ReminderStatus.OVERDUE) R.string.notif_body_overdue else R.string.notif_body_soon,
            jenis, motor
        )

    companion object {
        private const val UNIQUE = "motocare_daily_reminder"
        private const val KEY_FORCE = "force"

        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(ctx)
                .enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.UPDATE, req)
        }

        /** Cek pengingat SEKARANG (tanpa menunggu jadwal). Dipanggil setelah data berubah / aplikasi dibuka. */
        fun checkNow(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<ReminderWorker>().build()
            WorkManager.getInstance(ctx)
                .enqueueUniqueWork("motocare_check_now", ExistingWorkPolicy.REPLACE, req)
        }

        /** Dipakai tombol "Tes notifikasi" di Profil. */
        fun runNow(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInputData(workDataOf(KEY_FORCE to true)).build()
            WorkManager.getInstance(ctx).enqueue(req)
        }
    }
}
