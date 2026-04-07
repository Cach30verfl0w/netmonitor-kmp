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

package dev.cacheoverflow.netmonitor.dbus

/**
 * All possible states of the network manager.
 *
 * This enum represents all possible Network Manager D-Bus states and is used to read the current
 * network state for detecting changes. (like disconnect or general connectivity change)
 *
 * @see <a href="https://people.freedesktop.org/~lkundrak/nm-dbus-api/nm-dbus-types.html">NetworkManager D-Bus API Types</a>
 */
internal enum class NetworkManagerState(private val value: Int, private val literal: String, val isConnected: Boolean) {
    UNKNOWN(0, "Unknown", false),
    ASLEEP(10, "Asleep", false),
    DISCONNECTED(20, "Disconnected", false),
    DISCONNECTING(30, "Disconnecting", false),
    CONNECTING(40, "Connecting", false),
    CONNECTED_LOCAL(50, "Connected local", true),
    CONNECTED_SITE(60, "Connected site", true),
    CONNECTED_GLOBAL(70, "Connected global", true);

    override fun toString(): String = literal

    companion object {
        @JvmStatic
        fun fromInt(value: Int): NetworkManagerState = entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}
