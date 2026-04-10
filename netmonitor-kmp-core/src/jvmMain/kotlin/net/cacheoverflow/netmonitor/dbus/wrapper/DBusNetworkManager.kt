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

package net.cacheoverflow.netmonitor.dbus.wrapper

import net.cacheoverflow.netmonitor.NetworkType
import net.cacheoverflow.netmonitor.dbus.DBusConnection
import net.cacheoverflow.netmonitor.dbus.MethodCall

@JvmInline
internal value class DBusNetworkManager(private val connection: DBusConnection) {
    fun isNetworkMetered(): Result<Boolean> {
        val reply = connection.call(MethodCall.GET_NETWORK_MANAGER_PROPERTIES) {
            appendString("org.freedesktop.NetworkManager")
            appendString("Metered")
        }.getOrNull() ?: return Result.failure(RuntimeException("Unable to read metering info"))

        return reply.use { reply ->
            reply.iterator.recurse()?.readInt()?.let { Result.success(it.rem(4) != 0) }
                ?: return Result.failure(RuntimeException("Unable to read metering info"))
        }
    }

    fun readPrimaryConnectionType(): Result<NetworkType> {
        val reply = connection.call(MethodCall.GET_NETWORK_MANAGER_PROPERTIES) {
            appendString("org.freedesktop.NetworkManager")
            appendString("PrimaryConnectionType")
        }.getOrNull() ?: return Result.failure(RuntimeException("Unable to read primary connection type"))

        return reply.use { reply ->
            reply.iterator.recurse()?.readString()?.let {
                Result.success(
                    when (it) {
                        "802-3-ethernet" -> NetworkType.Ethernet
                        "802-11-wireless" -> NetworkType.WiFi
                        "gsm", "cdma" -> NetworkType.Cellular(NetworkType.Cellular.Generation.UNKNOWN, null)
                        "bluetooth" -> NetworkType.Bluetooth
                        else -> NetworkType.Unknown
                    }
                )
            } ?: return Result.failure(RuntimeException("Unable to read primary connection type"))
        }
    }
}
