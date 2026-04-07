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
 * @author Cedric Hammes
 * @since  03/04/2026
 */
internal data class MatchRule(
    val type: String,
    val sender: String?,
    val interfaceName: String?,
    val member: String?,
    val path: String? = null) {
    override fun toString(): String = buildString {
        append("type='$type'")
        sender?.let { append(",sender='$it'") }
        interfaceName?.let { append(",interface='$it'") }
        member?.let { append(",member='$it'") }
        path?.let { append(",path='$it'") }
    }

    companion object {
        @JvmStatic
        val NETWORK_MANAGER_STATE_CHANGED = MatchRule(
            type = "signal",
            sender = "org.freedesktop.NetworkManager",
            interfaceName = "org.freedesktop.NetworkManager",
            member = "StateChanged"
        )
    }
}
