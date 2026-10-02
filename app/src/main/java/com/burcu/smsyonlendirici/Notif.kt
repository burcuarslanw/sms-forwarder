package com.burcu.smsyonlendirici

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Her yönlendirme için görünür bir bildirim (şeffaflık + geri bildirim). */
object Notif {
    private const val CH = "forwards"
    private var id = 1

    fun ensureChannel(c: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CH, "Yönlendirmeler", NotificationManager.IMPORTANCE_LOW)
            c.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    fun show(c: Context, title: String, text: String) {
        ensureChannel(c)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val n = NotificationCompat.Builder(c, CH)
            .setSmallIcon(R.drawable.ic_stat_fwd)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(c).notify(id++, n)
    }
}
