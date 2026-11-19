package com.devicexpert.app.data.collector

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.Inet4Address
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkCollector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var lastSampleTimeMs: Long = 0L
    private var lastRxBytes: Long = -1L
    private var lastTxBytes: Long = -1L

    @Synchronized
    fun getNetworkMetrics(): NetworkResult {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val capabilities = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val linkProperties = activeNetwork?.let { cm.getLinkProperties(it) }

        val netType = when {
            capabilities == null -> "OFFLINE"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }

        val ipv4 = linkProperties?.linkAddresses
            ?.map { it.address }
            ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
        val fallbackIp = linkProperties?.linkAddresses
            ?.map { it.address }
            ?.firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress

        val now = SystemClock.elapsedRealtime()
        val currentRx = TrafficStats.getTotalRxBytes().takeIf { it >= 0L } ?: 0L
        val currentTx = TrafficStats.getTotalTxBytes().takeIf { it >= 0L } ?: 0L

        var rxPerSec = 0L
        var txPerSec = 0L

        if (lastSampleTimeMs > 0L && lastRxBytes >= 0L && lastTxBytes >= 0L) {
            val deltaMs = (now - lastSampleTimeMs).coerceAtLeast(1L)
            val deltaRx = (currentRx - lastRxBytes).coerceAtLeast(0L)
            val deltaTx = (currentTx - lastTxBytes).coerceAtLeast(0L)
            rxPerSec = (deltaRx * 1000L) / deltaMs
            txPerSec = (deltaTx * 1000L) / deltaMs
        }

        lastSampleTimeMs = now
        lastRxBytes = currentRx
        lastTxBytes = currentTx

        return NetworkResult(
            netType = netType,
            ipAddress = ipv4 ?: fallbackIp,
            rxBytesPerSec = rxPerSec,
            txBytesPerSec = txPerSec,
            totalRxBytes = currentRx,
            totalTxBytes = currentTx,
            linkDownstreamKbps = capabilities?.linkDownstreamBandwidthKbps?.takeIf { it > 0 },
            linkUpstreamKbps = capabilities?.linkUpstreamBandwidthKbps?.takeIf { it > 0 }
        )
    }

    data class NetworkResult(
        val netType: String,
        val ipAddress: String?,
        val rxBytesPerSec: Long,
        val txBytesPerSec: Long,
        val totalRxBytes: Long,
        val totalTxBytes: Long,
        val linkDownstreamKbps: Int?,
        val linkUpstreamKbps: Int?
    )
}
