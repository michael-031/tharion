package com.example.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat

data class WifiReading(
    val isConnected: Boolean,
    val ssid: String,
    val bssid: String,
    val rssiDbm: Int?
)

class WifiTelemetryReader(private val context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    @Suppress("DEPRECATION")
    fun readCurrentWifi(): WifiReading {
        val activeWifiNetwork = connectivityManager.allNetworks.firstOrNull { network ->
            val caps = connectivityManager.getNetworkCapabilities(network)
            caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }

        if (activeWifiNetwork == null) {
            return WifiReading(
                isConnected = false,
                ssid = "Not Connected",
                bssid = "--:--:--:--:--:--",
                rssiDbm = null
            )
        }

        val caps = connectivityManager.getNetworkCapabilities(activeWifiNetwork)
        var rssiFromCaps: Int? = null
        var transportWifiInfo: WifiInfo? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && caps != null) {
            val sig = caps.signalStrength
            if (sig != NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED && sig in -127..-1) {
                rssiFromCaps = sig
            }
            transportWifiInfo = caps.transportInfo as? WifiInfo
        }

        val info: WifiInfo? = transportWifiInfo ?: try {
            wifiManager.connectionInfo
        } catch (_: SecurityException) {
            null
        }

        val rawSsid = info?.ssid?.trim('"')
        val cleanSsid = when {
            !hasLocationPermission() -> "Location Perm Needed"
            rawSsid.isNullOrBlank() || rawSsid == "<unknown ssid>" -> "WiFi Connected"
            else -> rawSsid
        }

        val bssid = info?.bssid?.takeIf { it.isNotBlank() && it != "02:00:00:00:00:00" }
            ?: "--:--:--:--:--:--"

        val legacyRssi = info?.rssi?.takeIf { it in -127..-1 }
        val finalRssi = rssiFromCaps ?: legacyRssi

        return WifiReading(
            isConnected = true,
            ssid = cleanSsid,
            bssid = bssid,
            rssiDbm = finalRssi
        )
    }
}
