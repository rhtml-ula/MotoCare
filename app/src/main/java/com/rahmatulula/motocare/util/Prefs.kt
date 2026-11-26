package com.rahmatulula.motocare.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** Pengaturan sederhana (mode tema) di SharedPreferences. */
object Prefs {
    private const val NAME = "motocare_prefs"
    private const val KEY_THEME = "theme_mode"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun themeMode(ctx: Context): Int =
        sp(ctx).getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    fun setThemeMode(ctx: Context, mode: Int) {
        sp(ctx).edit().putInt(KEY_THEME, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun lastNotifiedDay(ctx: Context, reminderId: Long): Long = sp(ctx).getLong("notified_$reminderId", -1)
    fun setNotifiedDay(ctx: Context, reminderId: Long, day: Long) {
        sp(ctx).edit().putLong("notified_$reminderId", day).apply()
    }

    fun notifPermissionAsked(ctx: Context): Boolean = sp(ctx).getBoolean("notif_asked", false)
    fun setNotifPermissionAsked(ctx: Context) { sp(ctx).edit().putBoolean("notif_asked", true).apply() }
}
