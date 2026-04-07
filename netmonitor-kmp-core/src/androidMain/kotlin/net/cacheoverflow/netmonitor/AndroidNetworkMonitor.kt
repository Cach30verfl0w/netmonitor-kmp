/*
 * Copyright 2026 Cedric Hammes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.cacheoverflow.netmonitor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * @param context the android application's context for the connectivity manager
 * @return        a new network monitor capable of monitoring the device's network state
 *
 * @author Cedric Hammes
 * @since  07/04/2026
 */
fun NetworkMonitor(contextGetter: () -> Context): NetworkMonitor = AndroidNetworkMonitor(contextGetter)

/**
 * @param context the android application's context for the connectivity manager
 *
 * @author Cedric Hammes
 * @since  07/04/2026
 */
private class AndroidNetworkMonitor(private val getContext: () -> Context) : NetworkMonitor {
    private val connectivityManager: ConnectivityManager = getContext().getSystemService(ConnectivityManager::class.java)
    private val telephonyManager: TelephonyManager = getContext().getSystemService(TelephonyManager::class.java)

    override val isAvailable: Boolean = true
    override val state: Flow<NetworkState> = callbackFlow {
        val callback = NetworkStateEmitCallback(this)
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        send(determineNetworkState())
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    override fun close() = Unit

    private fun determineNetworkState(network: Network? = null, capabilities: NetworkCapabilities? = null): NetworkState {
        val capabilities = capabilities
            ?: ((network ?: connectivityManager.activeNetwork)?.let { connectivityManager.getNetworkCapabilities(it) })
            ?: return NetworkState.Offline // When no capabilities can be acquired, we are offline

        return getNetworkStateFromCapabilities(capabilities) {
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> NetworkType.Bluetooth
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> getCellularInformation()
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.Ethernet
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WiFi
                else -> NetworkType.Unknown
            }
        }
    }

    private fun getCellularInformation(): NetworkType.Cellular {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.w("AndroidNetworkMonitor", "Unable to read information of cellular information because of old device version")
            return NetworkType.Cellular(NetworkType.Cellular.Generation.UNKNOWN, null)
        }

        if (getContext().checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.w("AndroidNetworkMonitor", "Unable to read information of cellular network because of missing permissions")
            return NetworkType.Cellular(NetworkType.Cellular.Generation.UNKNOWN, null)
        }

        return NetworkType.Cellular(
            carrier = telephonyManager.networkOperatorName.takeIf { it.isNotBlank() },
            generation = when (telephonyManager.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_NR -> NetworkType.Cellular.Generation.G5
                TelephonyManager.NETWORK_TYPE_LTE -> NetworkType.Cellular.Generation.G4
                TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSPAP -> NetworkType.Cellular.Generation.G3
                TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS -> NetworkType.Cellular.Generation.G2
                else -> NetworkType.Cellular.Generation.UNKNOWN
            }
        )
    }

    /**
     * @param scope the producer scope of the callback flow used for sending network state
     *
     * @author Cedric Hammes
     * @since  07/04/2026
     */
    private inner class NetworkStateEmitCallback(private val scope: ProducerScope<NetworkState>) : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            scope.trySend(determineNetworkState(network))
        }

        override fun onLost(network: Network) {
            scope.trySend(NetworkState.Offline)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            scope.trySend(determineNetworkState(network, networkCapabilities))
        }
    }
}
