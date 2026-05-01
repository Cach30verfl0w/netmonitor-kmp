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

import platform.Network.nw_path_get_status
import platform.Network.nw_path_is_constrained
import platform.Network.nw_path_is_expensive
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_status_unsatisfied
import platform.Network.nw_path_t
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_queue_t
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
internal class AppleNetworkMonitor : AbstractObservable<NetworkMonitor.Callback>(), NetworkMonitor {
    private val monitor = nw_path_monitor_create()
    private val isClosed: AtomicBoolean = AtomicBoolean(false)
    private val networkQueue: dispatch_queue_t = dispatch_queue_create(
        label = "net.cacheoverflow.netmonitor.NetworkMonitor",
        attr = null
    )

    init {
        nw_path_monitor_set_queue(monitor, networkQueue)
        nw_path_monitor_set_update_handler(monitor) { path ->
            if (isClosed.load() || path == null) return@nw_path_monitor_set_update_handler

            val newState = mapPathToNetworkState(path)
            notifyCallbacks { callback ->
                callback.networkStateChanged(newState)
            }
        }

        nw_path_monitor_start(monitor)
    }

    override fun close() {
        if (isClosed.compareAndExchange(expectedValue = false, newValue = true)) {
            return
        }

        nw_path_monitor_cancel(monitor)
    }

    private fun mapPathToNetworkState(path: nw_path_t): NetworkState {
        val status = nw_path_get_status(path)

        return when (status) {
            nw_path_status_unsatisfied -> NetworkState.Offline
            nw_path_status_satisfied -> {
                NetworkState.Online(
                    isMetered = nw_path_is_expensive(path) || nw_path_is_constrained(path), canReachRemoteDevices = true
                )
            }

            else -> NetworkState.Unknown
        }
    }
}
