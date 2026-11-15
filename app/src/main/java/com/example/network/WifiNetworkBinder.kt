package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

/**
 * Forces HTTP & WebView traffic onto the active WiFi Network even when
 * the Piso WiFi portal has no internet (unvalidated captive network) and
 * Android would otherwise route fetch() over 4G/5G cellular data.
 */
class WifiNetworkBinder(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun findActiveWifiNetwork(): Network? {
        @Suppress("DEPRECATION")
        val networks = connectivityManager.allNetworks
        return networks.firstOrNull { network ->
            val caps = connectivityManager.getNetworkCapabilities(network)
            caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
    }

    fun bindProcessToWifi(): Network? {
        val wifiNetwork = findActiveWifiNetwork()
        return if (wifiNetwork != null) {
            try {
                connectivityManager.bindProcessToNetwork(wifiNetwork)
                wifiNetwork
            } catch (_: SecurityException) {
                null
            }
        } else {
            null
        }
    }

    fun clearProcessBinding() {
        try {
            connectivityManager.bindProcessToNetwork(null)
        } catch (_: Exception) {
            // Ignore cleanup failure
        }
    }
}
