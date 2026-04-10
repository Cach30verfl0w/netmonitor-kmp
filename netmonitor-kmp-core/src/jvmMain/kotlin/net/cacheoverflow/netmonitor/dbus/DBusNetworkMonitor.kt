/**
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

package net.cacheoverflow.netmonitor.dbus

import net.cacheoverflow.netmonitor.NetworkMonitor
import net.cacheoverflow.netmonitor.NetworkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import net.cacheoverflow.netmonitor.dbus.util.MatchRule
import net.cacheoverflow.netmonitor.dbus.wrapper.DBusNetworkManager
import net.cacheoverflow.netmonitor.dbus.wrapper.NetworkManagerState
import java.lang.foreign.Arena

internal class DBusNetworkMonitor : NetworkMonitor {
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val arena: Arena = Arena.ofAuto()
    private val nativeLibrary: DBusNativeLibrary? = DBusNativeLibrary.load(arena).getOrNull()
    private val connection: DBusConnection? = nativeLibrary?.createConnection()?.getOrNull()

    override val isAvailable: Boolean = nativeLibrary != null
    override val state: MutableStateFlow<NetworkState> = MutableStateFlow(NetworkState.Unknown)

    init {
        connection?.startEventLoop(coroutineScope)
        connection?.call(MethodCall.GET_NETWORK_MANAGER_PROPERTIES) {
            appendString("org.freedesktop.NetworkManager")
            appendString("State")
        }?.getOrNull()?.let { reply ->
            updateNetworkStatus(
                networkManager = DBusNetworkManager(connection),
                iterator = requireNotNull(reply.iterator.recurse())
            )
        }

        connection?.addMatchRule(MatchRule.NETWORK_MANAGER_STATE_CHANGED)
        connection?.addFilter { connection, message ->
            updateNetworkStatus(
                networkManager = DBusNetworkManager(connection),
                iterator = message.iterator
            )
            return@addFilter true
        }
    }

    override fun close() {
        connection?.close()
        nativeLibrary?.close()
    }

    private fun updateNetworkStatus(networkManager: DBusNetworkManager, iterator: DBusMessageIterator) {
        val networkState = NetworkManagerState.fromInt(requireNotNull(iterator.readInt()))
        when {
            networkState.isConnected -> {
                val networkType = networkManager.readPrimaryConnectionType().getOrThrow()
                val isMetered = networkManager.isNetworkMetered().getOrThrow()
                state.value = NetworkState.Online(networkType, isMetered)
            }

            networkState == NetworkManagerState.DISCONNECTED || networkState == NetworkManagerState.ASLEEP -> {
                state.value = NetworkState.Offline
            }
        }
    }
}
