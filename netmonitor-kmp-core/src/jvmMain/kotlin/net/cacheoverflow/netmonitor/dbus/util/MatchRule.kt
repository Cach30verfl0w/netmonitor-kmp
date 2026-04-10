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

package net.cacheoverflow.netmonitor.dbus.util

/**
 * @author Cedric Hammes
 * @since  03/04/2026
 */
internal data class MatchRule(
    val type: MessageType,
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
        fun builder(type: MessageType) = Builder(type)

        val NETWORK_MANAGER_STATE_CHANGED: MatchRule =
            builder(MessageType.SIGNAL)
                .interfaceName("org.freedesktop.NetworkManager")
                .member("StateChanged")
                .build()

        val NETWORK_MANAGER_PROPERTIES_CHANGED: MatchRule =
            builder(MessageType.SIGNAL)
                .interfaceName("org.freedesktop.DBus.Properties")
                .member("PropertiesChanged")
                .path("/org/freedesktop/NetworkManager")
                .build()
    }

    class Builder(private val type: MessageType) {
        private var sender: String? = null
        private var interfaceName: String? = null
        private var member: String? = null
        private var path: String? = null

        fun sender(sender: String) = apply { this.sender = sender }
        fun interfaceName(name: String) = apply { this.interfaceName = name }
        fun member(member: String) = apply { this.member = member }
        fun path(path: String) = apply { this.path = path }

        fun build(): MatchRule = MatchRule(
            type = type,
            sender = sender,
            interfaceName = interfaceName,
            member = member,
            path = path
        )
    }
}
