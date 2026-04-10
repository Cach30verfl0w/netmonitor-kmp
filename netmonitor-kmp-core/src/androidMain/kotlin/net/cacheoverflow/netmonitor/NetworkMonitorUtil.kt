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

import android.net.NetworkCapabilities

/**
 * @param capabilities     the current network's capabilities
 * @return                 the current network's state
 *
 * @author Cedric Hammes
 * @since  07/04/2026
 */
internal fun getNetworkStateFromCapabilities(
    capabilities: NetworkCapabilities,
    getNetworkType: (NetworkCapabilities) -> NetworkType
): NetworkState = when {
    !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> NetworkState.CaptivePortal
    else -> {
        val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        NetworkState.Online(getNetworkType(capabilities), isMetered)
    }
}
