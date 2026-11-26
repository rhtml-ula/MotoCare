package com.rahmatulula.motocare.data.repository

import com.rahmatulula.motocare.data.local.AppDatabase
import com.rahmatulula.motocare.data.local.Motor
import com.rahmatulula.motocare.data.local.Reminder
import com.rahmatulula.motocare.data.local.ReminderItem
import com.rahmatulula.motocare.data.local.ServiceItem
import com.rahmatulula.motocare.data.local.ServiceRecord
import com.rahmatulula.motocare.util.Formatters
import com.rahmatulula.motocare.util.PhotoHelper
import kotlinx.coroutines.flow.Flow

/** Satu-satunya pintu akses data; semua query otomatis dibatasi per pengguna (uid). */
class MotoRepository(db: AppDatabase, private val uid: String) {
    private val motorDao = db.motorDao()
    private val serviceDao = db.serviceDao()
    private val reminderDao = db.reminderDao()

    // ---------- Motor ----------
    val motors: Flow<List<Motor>> = motorDao.observeAll(uid)
    suspend fun getMotor(id: Long): Motor? = motorDao.getById(id, uid)
    suspend fun saveMotor(m: Motor) {
        if (m.id == 0L) motorDao.insert(m.copy(userId = uid)) else motorDao.update(m.copy(userId = uid))
    }
    suspend fun deleteMotor(m: Motor) {
        // Hapus foto motor dari penyimpanan; servis & pengingat ikut terhapus (CASCADE)
        PhotoHelper.delete(m.fotoPath)
        motorDao.delete(m)
    }

    // ---------- Servis ----------
    val services: Flow<List<ServiceItem>> = serviceDao.observeAll(uid)
    val totalCost: Flow<Long> = serviceDao.observeTotalCost(uid)
    fun observeService(id: Long): Flow<ServiceItem?> = serviceDao.observeById(id, uid)
    suspend fun getService(id: Long): ServiceRecord? = serviceDao.getById(id, uid)

    suspend fun saveService(s: ServiceRecord) {
        val record = s.copy(userId = uid)
        if (record.id == 0L) serviceDao.insert(record) else serviceDao.update(record)
        // Odometer motor ikut naik bila servis dicatat di km yang lebih besar
        motorDao.raiseOdometer(record.motorId, record.odometer)
        // Perpanjang pengingat sejenis secara otomatis
        reminderDao.findByMotorAndJenis(record.motorId, record.jenis, uid).forEach { r ->
            val nextDate = r.intervalBulan?.let { Formatters.addMonths(record.tanggal, it) } ?: r.jatuhTempoTanggal
            val nextKm = r.intervalKm?.let { record.odometer + it } ?: r.jatuhTempoKm
            reminderDao.update(r.copy(jatuhTempoTanggal = nextDate, jatuhTempoKm = nextKm))
        }
    }

    suspend fun deleteService(s: ServiceRecord) {
        PhotoHelper.delete(s.fotoPath)
        serviceDao.delete(s)
    }

    // ---------- Pengingat ----------
    val reminders: Flow<List<ReminderItem>> = reminderDao.observeAll(uid)
    suspend fun getReminder(id: Long): Reminder? = reminderDao.getById(id, uid)
    suspend fun saveReminder(r: Reminder) {
        if (r.id == 0L) reminderDao.insert(r.copy(userId = uid)) else reminderDao.update(r.copy(userId = uid))
    }
    suspend fun deleteReminder(r: Reminder) = reminderDao.delete(r)
    suspend fun setReminderActive(r: Reminder, active: Boolean) = reminderDao.update(r.copy(aktif = active))
    suspend fun activeReminders(): List<ReminderItem> = reminderDao.getActive(uid)
}
