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

package dev.cacheoverflow.netmonitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dev.cacheoverflow.netmonitor.NetworkMonitor
import dev.cacheoverflow.netmonitor.NetworkState
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
fun NetworkMonitor(context: Context): NetworkMonitor = AndroidNetworkMonitor(context)

/**
 * @param context the android application's context for the connectivity manager
 *
 * @author Cedric Hammes
 * @since  07/04/2026
 */
private class AndroidNetworkMonitor(context: Context) : NetworkMonitor {
    private val connectivityManager: ConnectivityManager = context.getSystemService(ConnectivityManager::class.java)

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
        return getNetworkStateFromCapabilities(capabilities)
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
