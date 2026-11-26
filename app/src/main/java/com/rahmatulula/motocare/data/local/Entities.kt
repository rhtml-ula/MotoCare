package com.rahmatulula.motocare.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Entitas Motor milik satu pengguna (userId = UID Firebase). */
@Entity(tableName = "motors", indices = [Index("userId")])
data class Motor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val nama: String,          // contoh: Honda Vario 125
    val plat: String,          // contoh: BL 1234 AB
    val tahun: Int,
    val odometer: Int,         // km saat ini
    val fotoPath: String? = null
)

/** DATA UTAMA: riwayat servis (>= 4 atribut selain ID). */
@Entity(
    tableName = "service_records",
    foreignKeys = [ForeignKey(
        entity = Motor::class, parentColumns = ["id"], childColumns = ["motorId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("motorId"), Index("userId")]
)
data class ServiceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val motorId: Long,
    val jenis: String,         // jenis servis
    val tanggal: Long,         // UTC midnight millis
    val odometer: Int,         // km saat servis
    val biaya: Long,           // rupiah
    val bengkel: String,
    val catatan: String = "",
    val fotoPath: String? = null   // foto struk / bukti servis
)

/** Pengingat perawatan berbasis tanggal dan/atau jarak tempuh. */
@Entity(
    tableName = "reminders",
    foreignKeys = [ForeignKey(
        entity = Motor::class, parentColumns = ["id"], childColumns = ["motorId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("motorId"), Index("userId")]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val motorId: Long,
    val jenis: String,
    val intervalBulan: Int? = null,     // untuk perpanjangan otomatis setelah servis
    val intervalKm: Int? = null,
    val jatuhTempoTanggal: Long? = null,
    val jatuhTempoKm: Int? = null,
    val aktif: Boolean = true
)

/** Hasil JOIN untuk ditampilkan di daftar. */
data class ServiceItem(
    @Embedded val service: ServiceRecord,
    val motorNama: String,
    val motorPlat: String
)

data class ReminderItem(
    @Embedded val reminder: Reminder,
    val motorNama: String,
    val motorOdometer: Int
)
