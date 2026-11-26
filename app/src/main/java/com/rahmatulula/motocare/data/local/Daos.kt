package com.rahmatulula.motocare.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MotorDao {
    @Query("SELECT * FROM motors WHERE userId = :uid ORDER BY nama COLLATE NOCASE")
    fun observeAll(uid: String): Flow<List<Motor>>

    @Query("SELECT * FROM motors WHERE id = :id AND userId = :uid")
    suspend fun getById(id: Long, uid: String): Motor?

    @Insert suspend fun insert(m: Motor): Long
    @Update suspend fun update(m: Motor)
    @Delete suspend fun delete(m: Motor)

    @Query("UPDATE motors SET odometer = :km WHERE id = :id AND odometer < :km")
    suspend fun raiseOdometer(id: Long, km: Int)
}

@Dao
interface ServiceDao {
    @Query(
        """SELECT s.*, m.nama AS motorNama, m.plat AS motorPlat
           FROM service_records s INNER JOIN motors m ON s.motorId = m.id
           WHERE s.userId = :uid ORDER BY s.tanggal DESC, s.id DESC"""
    )
    fun observeAll(uid: String): Flow<List<ServiceItem>>

    @Query(
        """SELECT s.*, m.nama AS motorNama, m.plat AS motorPlat
           FROM service_records s INNER JOIN motors m ON s.motorId = m.id
           WHERE s.id = :id AND s.userId = :uid"""
    )
    fun observeById(id: Long, uid: String): Flow<ServiceItem?>

    @Query("SELECT * FROM service_records WHERE id = :id AND userId = :uid")
    suspend fun getById(id: Long, uid: String): ServiceRecord?

    @Query("SELECT COALESCE(SUM(biaya), 0) FROM service_records WHERE userId = :uid")
    fun observeTotalCost(uid: String): Flow<Long>

    @Insert suspend fun insert(s: ServiceRecord): Long
    @Update suspend fun update(s: ServiceRecord)
    @Delete suspend fun delete(s: ServiceRecord)
}

@Dao
interface ReminderDao {
    @Query(
        """SELECT r.*, m.nama AS motorNama, m.odometer AS motorOdometer
           FROM reminders r INNER JOIN motors m ON r.motorId = m.id
           WHERE r.userId = :uid"""
    )
    fun observeAll(uid: String): Flow<List<ReminderItem>>

    @Query(
        """SELECT r.*, m.nama AS motorNama, m.odometer AS motorOdometer
           FROM reminders r INNER JOIN motors m ON r.motorId = m.id
           WHERE r.userId = :uid AND r.aktif = 1"""
    )
    suspend fun getActive(uid: String): List<ReminderItem>

    @Query("SELECT * FROM reminders WHERE id = :id AND userId = :uid")
    suspend fun getById(id: Long, uid: String): Reminder?

    @Query("SELECT * FROM reminders WHERE motorId = :motorId AND jenis = :jenis AND userId = :uid")
    suspend fun findByMotorAndJenis(motorId: Long, jenis: String, uid: String): List<Reminder>

    @Insert suspend fun insert(r: Reminder): Long
    @Update suspend fun update(r: Reminder)
    @Delete suspend fun delete(r: Reminder)
}
