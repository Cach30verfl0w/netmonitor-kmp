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
import net.cacheoverflow.netmonitor.NetworkType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.foreign.Arena
import kotlin.use

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
            updateNetworkStatus(connection, requireNotNull(reply.iterator.recurse()))
        }

        connection?.addMatchRule(MatchRule.NETWORK_MANAGER_STATE_CHANGED)
        connection?.addFilter { connection, message ->
            updateNetworkStatus(connection, message.iterator)
            return@addFilter true
        }
    }

    override fun close() {
        connection?.close()
        nativeLibrary?.close()
    }

    private fun updateNetworkStatus(connection: DBusConnection, iterator: DBusMessageIterator) {
        val networkState = NetworkManagerState.fromInt(requireNotNull(iterator.readInt()))
        when {
            networkState.isConnected -> {
                val networkType = connection.readPrimaryConnectionType().getOrThrow()
                val isMetered = connection.isNetworkMetered().getOrThrow()
                state.value = NetworkState.Online(networkType, isMetered)
            }

            networkState == NetworkManagerState.DISCONNECTED || networkState == NetworkManagerState.ASLEEP -> {
                state.value = NetworkState.Offline
            }
        }
    }

    private fun DBusConnection.readPrimaryConnectionType(): Result<NetworkType> {
        val reply = call(MethodCall.GET_NETWORK_MANAGER_PROPERTIES) {
            appendString("org.freedesktop.NetworkManager")
            appendString("PrimaryConnectionType")
        }.getOrNull() ?: return Result.failure(RuntimeException("Unable to read primary connection type"))

        return reply.use { reply ->
            reply.iterator.recurse()?.readString()?.let {
                Result.success(
                    when (it) {
                        "802-3-ethernet" -> NetworkType.ETHERNET
                        "802-11-wireless" -> NetworkType.WIFI
                        "gsm", "cdma" -> NetworkType.CELLULAR
                        "bluetooth" -> NetworkType.BLUETOOTH
                        else -> NetworkType.UNKNOWN
                    }
                )
            } ?: return Result.failure(RuntimeException("Unable to read primary connection type"))
        }
    }

    private fun DBusConnection.isNetworkMetered(): Result<Boolean> {
        val reply = call(MethodCall.GET_NETWORK_MANAGER_PROPERTIES) {
            appendString("org.freedesktop.NetworkManager")
            appendString("Metered")
        }.getOrNull() ?: return Result.failure(RuntimeException("Unable to read metering info"))

        return reply.use { reply ->
            reply.iterator.recurse()?.readInt()?.let { Result.success(it.rem(4) != 0) }
                ?: return Result.failure(RuntimeException("Unable to read metering info"))
        }
    }
}
