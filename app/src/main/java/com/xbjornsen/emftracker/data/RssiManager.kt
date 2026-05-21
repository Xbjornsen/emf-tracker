package com.xbjornsen.emftracker.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import com.xbjornsen.emftracker.data.models.RssiState
import com.xbjornsen.emftracker.data.models.WifiPowerEstimate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.pow

class RssiManager(context: Context) {

    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _rssiState = MutableStateFlow(RssiState())
    val rssiState: StateFlow<RssiState> = _rssiState.asStateFlow()

    private val _powerHistory = MutableStateFlow<List<Float>>(emptyList())
    val powerHistory: StateFlow<List<Float>> = _powerHistory.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var currentWifiRssi: Int? = null
    private var lastKnownFreqMhz: Int = 2400

    // Power density from received signal using antenna effective aperture formula:
    // PD = Pr * 4π / λ²   where Pr = 10^((dBm-30)/10) W, λ = c/f
    // This gives the field power density at the phone without assuming transmitter power.
    private fun estimatePower(rssiDbm: Int, freqMhz: Int): WifiPowerEstimate {
        val freqHz = freqMhz.toDouble() * 1e6
        val lambda = 3e8 / freqHz
        val prW = 10.0.pow((rssiDbm.toDouble() - 30.0) / 10.0)
        val pdWm2 = prW * 4.0 * PI / (lambda * lambda)
        val pdUwM2 = (pdWm2 * 1_000_000.0).toFloat().coerceAtLeast(0f)
        return WifiPowerEstimate(rssiDbm, freqMhz, pdUwM2)
    }

    fun startWifiCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            val callback = object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    val signal = caps.signalStrength
                    currentWifiRssi = if (signal == Int.MIN_VALUE) null else signal
                    val wifiInfo = caps.transportInfo as? android.net.wifi.WifiInfo
                    lastKnownFreqMhz = wifiInfo?.frequency?.takeIf { it > 0 } ?: 2400
                    refresh(currentWifiRssi)
                }
                override fun onLost(network: Network) {
                    currentWifiRssi = null
                    refresh(null)
                }
            }
            connectivityManager.registerNetworkCallback(request, callback)
            networkCallback = callback
        }
    }

    fun stopWifiCallback() {
        networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
        networkCallback = null
        currentWifiRssi = null
    }

    @SuppressLint("MissingPermission")
    fun getWifiRssi(): Int? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return currentWifiRssi
        @Suppress("DEPRECATION")
        val info = wifiManager.connectionInfo
        @Suppress("DEPRECATION")
        if (info.networkId == -1) return null
        @Suppress("DEPRECATION")
        lastKnownFreqMhz = info.frequency.takeIf { it > 0 } ?: 2400
        @Suppress("DEPRECATION")
        return info.rssi
    }

    fun refresh(wifiRssi: Int?) {
        val estimate = wifiRssi?.let { estimatePower(it, lastKnownFreqMhz) }
        _rssiState.value = RssiState(wifiPower = estimate)
        estimate?.let {
            _powerHistory.value = (_powerHistory.value + it.powerDensityUwM2).takeLast(60)
        }
    }

    fun clearHistory() {
        _powerHistory.value = emptyList()
    }
}
