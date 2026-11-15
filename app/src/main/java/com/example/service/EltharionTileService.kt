package com.example.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.GuardStateBus
import com.example.data.LogLevel
import com.example.data.SessionStatus

class EltharionTileService : TileService() {

    companion object {
        fun requestTileUpdate(context: Context) {
            try {
                requestListeningState(
                    context,
                    ComponentName(context, EltharionTileService::class.java)
                )
            } catch (_: Exception) {
            }
        }
    }

    override fun onTileAdded() {
        super.onTileAdded()
        GuardStateBus.appendLog("Eltharion Quick Settings tile added.", LogLevel.INFO)
        syncTileState()
    }

    override fun onStartListening() {
        super.onStartListening()
        syncTileState()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) {
            unlockAndRun { toggleGuardFromTile() }
        } else {
            toggleGuardFromTile()
        }
    }

    private fun toggleGuardFromTile() {
        val telemetry = GuardStateBus.telemetry.value
        if (telemetry.isGuardRunning) {
            WifiGuardService.stop(applicationContext)
            GuardStateBus.appendLog("Eltharion turned OFF from Quick Settings tile.", LogLevel.INFO)
            applyTileVisuals(isRunning = false, status = telemetry.sessionStatus, countdownSeconds = 0, rssiDbm = telemetry.rssiDbm)
            return
        }

        if (!hasLocationPermission()) {
            GuardStateBus.appendLog(
                "Quick Settings tap needs Location permission—opening Eltharion.",
                LogLevel.WARN
            )
            launchMainActivityForAutoStart()
            return
        }

        try {
            WifiGuardService.start(applicationContext, fromTile = true)
            GuardStateBus.appendLog("Eltharion turned ON from Quick Settings tile.", LogLevel.SUCCESS)
            applyTileVisuals(
                isRunning = true,
                status = telemetry.sessionStatus,
                countdownSeconds = telemetry.pauseCountdownSeconds,
                rssiDbm = telemetry.rssiDbm
            )
        } catch (_: Exception) {
            // Android 12+/14+ background FGS restriction fallback: collapse shade & start via visible activity
            GuardStateBus.appendLog(
                "Opening Eltharion to start WiFi protection cleanly.",
                LogLevel.INFO
            )
            launchMainActivityForAutoStart()
        }
    }

    private fun syncTileState() {
        val telemetry = GuardStateBus.telemetry.value
        applyTileVisuals(
            isRunning = telemetry.isGuardRunning,
            status = telemetry.sessionStatus,
            countdownSeconds = telemetry.pauseCountdownSeconds,
            rssiDbm = telemetry.rssiDbm
        )
    }

    private fun applyTileVisuals(
        isRunning: Boolean,
        status: SessionStatus,
        countdownSeconds: Int,
        rssiDbm: Int?
    ) {
        val tile = qsTile ?: return
        val label = getString(R.string.quick_tile_label)
        val hasPerm = hasLocationPermission()

        tile.label = label
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_eltharion)
        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

        val subtitleText = when {
            isRunning && status == SessionStatus.PAUSED && countdownSeconds > 0 -> {
                val mins = countdownSeconds / 60
                val secs = countdownSeconds % 60
                "Paused • %02d:%02d".format(mins, secs)
            }
            isRunning && status == SessionStatus.DISCONNECTED -> "Waiting WiFi"
            isRunning && rssiDbm != null -> "On • $rssiDbm dBm"
            isRunning -> "Protection On"
            !hasPerm -> "Setup Needed"
            else -> "Off • Tap to start"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = subtitleText
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = subtitleText
        }
        tile.contentDescription = "$label, $subtitleText"
        tile.updateTile()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launchMainActivityForAutoStart() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            putExtra(MainActivity.EXTRA_AUTO_START_GUARD, true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
