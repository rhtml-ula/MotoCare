package com.rahmatulula.motocare.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.rahmatulula.motocare.R
import com.rahmatulula.motocare.databinding.ActivityMainBinding
import com.rahmatulula.motocare.ui.auth.LoginActivity
import com.rahmatulula.motocare.ui.common.visible
import com.rahmatulula.motocare.util.NotificationHelper
import com.rahmatulula.motocare.util.Prefs
import com.rahmatulula.motocare.viewmodel.AuthViewModel
import com.rahmatulula.motocare.viewmodel.MotoViewModel
import com.rahmatulula.motocare.worker.ReminderWorker

/** Host navigasi: BottomNavigationView + Navigation Component. */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfig: AppBarConfiguration
    private val auth: AuthViewModel by viewModels()
    @Suppress("unused") private val data: MotoViewModel by viewModels() // dibuat di level activity

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                // Izin ditolak: aplikasi tetap berjalan normal, hanya tanpa notifikasi
                Snackbar.make(binding.root, R.string.notif_denied_info, Snackbar.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!auth.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHost.navController
        val topLevel = setOf(
            R.id.homeFragment, R.id.historyFragment, R.id.remindersFragment,
            R.id.motorsFragment, R.id.profileFragment
        )
        appBarConfig = AppBarConfiguration(topLevel)
        setupActionBarWithNavController(navController, appBarConfig)
        binding.bottomNav.setupWithNavController(navController)

        // Bottom navigation hanya tampil di layar utama
        navController.addOnDestinationChangedListener { _, dest, _ ->
            binding.bottomNav.visible(dest.id in topLevel)
        }

        askNotificationPermissionOnce()

        // Saat aplikasi dibuka, cek pengingat langsung (hanya sekali per pembukaan)
        if (savedInstanceState == null) ReminderWorker.checkNow(this)
    }

    private fun askNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT >= 33 && !NotificationHelper.canNotify(this) &&
            !Prefs.notifPermissionAsked(this)
        ) {
            Prefs.setNotifPermissionAsked(this)
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val nav = (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment).navController
        return nav.navigateUp(appBarConfig) || super.onSupportNavigateUp()
    }
}
