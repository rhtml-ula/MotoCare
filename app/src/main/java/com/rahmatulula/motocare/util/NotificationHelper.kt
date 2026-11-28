package com.rahmatulula.motocare.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rahmatulula.motocare.R
import com.rahmatulula.motocare.ui.MainActivity

object NotificationHelper {
    const val CHANNEL_ID = "service_reminder"

    fun createChannel(ctx: Context) {
        val ch = NotificationChannel(
            CHANNEL_ID, ctx.getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = ctx.getString(R.string.channel_desc) }
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun canNotify(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Menampilkan notifikasi; diam-diam dilewati bila izin ditolak (aplikasi tetap normal). */
    fun show(ctx: Context, id: Int, title: String, text: String): Boolean {
        if (!canNotify(ctx)) return false
        val open = PendingIntent.getActivity(
            ctx, id, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        return try {
            NotificationManagerCompat.from(ctx).notify(id, n)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
