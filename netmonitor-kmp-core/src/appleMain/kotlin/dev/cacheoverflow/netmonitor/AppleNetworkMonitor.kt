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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Network.nw_interface_type_cellular
import platform.Network.nw_interface_type_wifi
import platform.Network.nw_interface_type_wired
import platform.Network.nw_path_get_status
import platform.Network.nw_path_is_expensive
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_monitor_t
import platform.Network.nw_path_status_satisfiable
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_status_unsatisfied
import platform.Network.nw_path_uses_interface_type
import platform.darwin.dispatch_queue_create

/**
 * @author Cedric Hammes
 * @since  07/04/2026
 */
internal class AppleNetworkMonitor : NetworkMonitor {
    private val _state: MutableStateFlow<NetworkState> = MutableStateFlow(NetworkState.Unknown)
    private val monitorQueue = dispatch_queue_create("de.cacheoverflow.netmonitor.queue", null)
    private val pathMonitor: nw_path_monitor_t = nw_path_monitor_create()
    override val state: Flow<NetworkState> = _state.asStateFlow()
    override val isAvailable: Boolean = true

    init {
        nw_path_monitor_set_queue(pathMonitor, monitorQueue)
        nw_path_monitor_set_update_handler(pathMonitor) { networkPath ->
            val isMetered = nw_path_is_expensive(networkPath)
            val networkType = when {
                nw_path_uses_interface_type(networkPath, nw_interface_type_wifi) -> NetworkType.WIFI
                nw_path_uses_interface_type(networkPath, nw_interface_type_wired) -> NetworkType.ETHERNET
                nw_path_uses_interface_type(networkPath, nw_interface_type_cellular) -> NetworkType.CELLULAR
                else -> NetworkType.UNKNOWN
            }

            _state.value = when (nw_path_get_status(networkPath)) {
                nw_path_status_satisfied -> NetworkState.Online(networkType, isMetered)
                nw_path_status_unsatisfied, nw_path_status_satisfiable -> NetworkState.Offline
                else -> NetworkState.Unknown
            }
        }

        nw_path_monitor_start(pathMonitor)
    }

    override fun close() = nw_path_monitor_cancel(pathMonitor)
}