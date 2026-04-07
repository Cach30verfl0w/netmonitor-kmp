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

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.invoke.MethodHandle

internal data class MethodCall(
    val service: String,
    val path: String = "/${service.replace(".", "/")}",
    val iface: String,
    val method: String
) {
    internal fun createMessage(create: MethodHandle, library: DBusNativeLibrary, arena: Arena): DBusMessage = DBusMessage.of(
        library = library,
        handle = create.invokeExact(
            arena.allocateFrom(service),
            arena.allocateFrom(path),
            arena.allocateFrom(iface),
            arena.allocateFrom(method)
        ) as MemorySegment
    )

    companion object {
        @JvmStatic
        val GET_NETWORK_MANAGER_PROPERTIES: MethodCall = MethodCall(
            service = "org.freedesktop.NetworkManager",
            iface = "org.freedesktop.DBus.Properties",
            method = "Get"
        )
    }
}