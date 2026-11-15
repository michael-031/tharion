package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class DeviceFeedbackHelper(private val context: Context) {

    companion object {
        const val CHANNEL_GUARD_ID = "lpb_guard_service"
        const val CHANNEL_ALERT_ID = "lpb_guard_expiry_alert"
        const val CHANNEL_BATTERY_ID = "eltharion_battery_guidance"
        const val GUARD_NOTIFICATION_ID = 1001
        const val EXPIRY_ALERT_NOTIFICATION_ID = 1002
        const val BATTERY_GUIDE_NOTIFICATION_ID = 1003
        const val TILE_FALLBACK_NOTIFICATION_ID = 1004
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val guardChannel = NotificationChannel(
                CHANNEL_GUARD_ID,
                context.getString(R.string.notification_channel_guard_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_guard_desc)
                setShowBadge(false)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                context.getString(R.string.notification_channel_alert_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_alert_desc)
                enableVibration(true)
            }

            val batteryChannel = NotificationChannel(
                CHANNEL_BATTERY_ID,
                context.getString(R.string.notification_channel_battery_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_battery_desc)
            }

            notificationManager.createNotificationChannel(guardChannel)
            notificationManager.createNotificationChannel(alertChannel)
            notificationManager.createNotificationChannel(batteryChannel)
        }
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } catch (_: Exception) {
            false
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun vibrateDouble() {
        vibratePattern(longArrayOf(0, 140, 100, 180))
    }

    fun vibrateSingle() {
        vibratePattern(longArrayOf(0, 140))
    }

    @Suppress("DEPRECATION")
    private fun vibratePattern(timings: LongArray) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                mgr.defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                vibrator.vibrate(timings, -1)
            }
        } catch (_: Exception) {
            // Vibrator may be unavailable on some devices/emulators
        }
    }

    fun sendExpiryWarningNotification(minutesRemaining: Int) {
        if (!hasNotificationPermission()) return

        ensureNotificationChannels()

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            20,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERT_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("LPB Pause Expiring Soon ($minutesRemaining min left)")
            .setContentText("Your paused Piso WiFi session expires in $minutesRemaining minutes. Return near the vendo or resume to keep your time.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        notificationManager.notify(EXPIRY_ALERT_NOTIFICATION_ID, notification)
        vibrateDouble()
    }

    fun sendBatteryOptimizationGuideNotification(): Boolean {
        if (!hasNotificationPermission()) return false

        ensureNotificationChannels()

        val settingsIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_BATTERY_SETTINGS, true)
        }
        val openBatteryPendingIntent = PendingIntent.getActivity(
            context,
            30,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.battery_guide_notif_title)
        val shortText = context.getString(R.string.battery_guide_notif_short)
        val fullGuide = context.getString(R.string.battery_guide_notif_body)

        val notification = NotificationCompat.Builder(context, CHANNEL_BATTERY_ID)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle(title)
            .setContentText(shortText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullGuide))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openBatteryPendingIntent)
            .addAction(0, "Open Battery Settings", openBatteryPendingIntent)
            .build()

        notificationManager.notify(BATTERY_GUIDE_NOTIFICATION_ID, notification)
        return true
    }

    fun sendTapToStartGuardNotification(): Boolean {
        if (!hasNotificationPermission()) return false

        ensureNotificationChannels()

        val startIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_AUTO_START_GUARD, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            40,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERT_ID)
            .setSmallIcon(R.drawable.ic_qs_eltharion)
            .setContentTitle("Tap to Start Eltharion")
            .setContentText("Android requires opening Eltharion once to start location-based WiFi checks.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(TILE_FALLBACK_NOTIFICATION_ID, notification)
        return true
    }
}
