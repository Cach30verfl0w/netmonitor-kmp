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

/**
 * All possible connectivity states of the network manager.
 *
 * @see <a href="https://networkmanager.dev/docs/api/latest/nm-dbus-types.html#NMConnectivityState">Network Manager D-Bus Connectivity State</a>
 *
 * @author Cedric Hammes
 * @since  10.04.2026
 */
enum class NetworkManagerConnectivity(private val value: Int, private val literal: String) {
    UNKNOWN(0, "Unknown"),
    None(1, "None"),
    Portal(2, "Portal"),
    Limited(3, "Limited"),
    Full(4, "Full");

    override fun toString(): String = literal

    companion object {
        @JvmStatic
        fun fromInt(value: Int): NetworkManagerConnectivity = entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}
