package com.rahmatulula.motocare.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.rahmatulula.motocare.data.local.AppDatabase
import com.rahmatulula.motocare.data.local.Motor
import com.rahmatulula.motocare.data.local.Reminder
import com.rahmatulula.motocare.data.local.ReminderItem
import com.rahmatulula.motocare.data.local.ServiceItem
import com.rahmatulula.motocare.data.local.ServiceRecord
import com.rahmatulula.motocare.data.repository.MotoRepository
import com.rahmatulula.motocare.util.ReminderLogic
import com.rahmatulula.motocare.util.ReminderStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class SortOption { NEWEST, OLDEST, COST_HIGH, COST_LOW }

data class HistoryUi(val items: List<ServiceItem>, val totalCount: Int)

/** ViewModel bersama (activity-scoped) untuk seluruh data MotoCare. */
class MotoViewModel(app: Application) : AndroidViewModel(app) {
    private val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private val repo = MotoRepository(AppDatabase.get(app), uid)

    // ---------- Motor ----------
    val motors: LiveData<List<Motor>> = repo.motors.asLiveData()

    // ---------- Riwayat: pencarian + filter + pengurutan ----------
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(SortOption.NEWEST)
    private val motorFilter = MutableStateFlow<Long?>(null)

    val currentQuery: String get() = query.value
    val currentSort: SortOption get() = sort.value
    val currentMotorFilter: Long? get() = motorFilter.value

    fun setQuery(q: String) { query.value = q }
    fun setSort(s: SortOption) { sort.value = s }
    fun setMotorFilter(id: Long?) { motorFilter.value = id }

    val history: LiveData<HistoryUi> =
        combine(repo.services, query, sort, motorFilter) { all, q, s, mf ->
            val key = q.trim().lowercase()
            val filtered = all.filter { item ->
                (mf == null || item.service.motorId == mf) &&
                    (key.isEmpty() || listOf(
                        item.service.jenis, item.service.bengkel, item.service.catatan,
                        item.motorNama, item.motorPlat
                    ).any { it.lowercase().contains(key) })
            }
            val sorted = when (s) {
                SortOption.NEWEST -> filtered.sortedByDescending { it.service.tanggal }
                SortOption.OLDEST -> filtered.sortedBy { it.service.tanggal }
                SortOption.COST_HIGH -> filtered.sortedByDescending { it.service.biaya }
                SortOption.COST_LOW -> filtered.sortedBy { it.service.biaya }
            }
            HistoryUi(sorted, all.size)
        }.asLiveData()

    // ---------- Beranda ----------
    val totalCost: LiveData<Long> = repo.totalCost.asLiveData()
    val serviceCount: LiveData<Int> = repo.services.map { it.size }.asLiveData()
    val upcomingReminders: LiveData<List<ReminderItem>> = repo.reminders.map { list ->
        list.filter {
            val st = ReminderLogic.status(it)
            st == ReminderStatus.OVERDUE || st == ReminderStatus.SOON
        }.sortedBy { ReminderLogic.urgency(it) }.take(3)
    }.asLiveData()

    // ---------- Pengingat ----------
    val reminders: LiveData<List<ReminderItem>> = repo.reminders.map { list ->
        list.sortedWith(
            compareBy<ReminderItem> { ReminderLogic.status(it) == ReminderStatus.INACTIVE }
                .thenBy { ReminderLogic.urgency(it) }
        )
    }.asLiveData()

    // ---------- Akses satu data ----------
    fun observeService(id: Long): LiveData<ServiceItem?> = repo.observeService(id).asLiveData()
    suspend fun getMotor(id: Long) = repo.getMotor(id)
    suspend fun getService(id: Long) = repo.getService(id)
    suspend fun getReminder(id: Long) = repo.getReminder(id)

    // ---------- Aksi tulis (jalan di background lewat coroutine) ----------
    fun saveMotor(m: Motor, onDone: () -> Unit) = viewModelScope.launch { repo.saveMotor(m); onDone() }
    fun deleteMotor(m: Motor, onDone: () -> Unit) = viewModelScope.launch { repo.deleteMotor(m); onDone() }

    fun saveService(s: ServiceRecord, onDone: () -> Unit) = viewModelScope.launch { repo.saveService(s); onDone() }
    fun deleteService(s: ServiceRecord, onDone: () -> Unit) = viewModelScope.launch { repo.deleteService(s); onDone() }

    fun saveReminder(r: Reminder, onDone: () -> Unit) = viewModelScope.launch { repo.saveReminder(r); onDone() }
    fun deleteReminder(r: Reminder, onDone: () -> Unit) = viewModelScope.launch { repo.deleteReminder(r); onDone() }
    fun setReminderActive(r: Reminder, active: Boolean) = viewModelScope.launch { repo.setReminderActive(r, active) }
}
