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

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlin.concurrent.atomics.ExperimentalAtomicApi

fun NetworkMonitor(contextGetter: () -> Context): NetworkMonitor = AndroidNetworkMonitor(contextGetter)

/**
 * @author Cedric Hammes
 * @since  07/04/2026
 */
@OptIn(ExperimentalAtomicApi::class)
private class AndroidNetworkMonitor(getContext: () -> Context) : AbstractObservable(), NetworkMonitor {
    private val connectivityManager: ConnectivityManager = getContext().getSystemService(ConnectivityManager::class.java)
    private val networkStateCallback: NetworkStateCallback = NetworkStateCallback { newState ->
        notifyCallbacksNoDelay(newState)
    }

    init {
        val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        connectivityManager.registerNetworkCallback(request, networkStateCallback)
    }

    override fun close() = connectivityManager.unregisterNetworkCallback(networkStateCallback)

    private fun determineNetworkState(
        network: Network? = null,
        capabilities: NetworkCapabilities? = null
    ): NetworkState {
        val capabilities = capabilities ?: ((network
            ?: connectivityManager.activeNetwork)?.let { connectivityManager.getNetworkCapabilities(it) })
        ?: return NetworkState.Offline // When no capabilities can be acquired, we are offline

        return getNetworkStateFromCapabilities(capabilities)
    }

    /**
     * @author Cedric Hammes
     * @since  07/04/2026
     */
    private inner class NetworkStateCallback(private val closure: (NetworkState) -> Unit) : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            closure(determineNetworkState(network))
        }

        override fun onLost(network: Network) {
            closure(NetworkState.Offline)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            closure(determineNetworkState(network, networkCapabilities))
        }
    }
}
