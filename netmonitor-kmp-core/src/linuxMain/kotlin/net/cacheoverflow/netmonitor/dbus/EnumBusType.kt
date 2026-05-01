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

package net.cacheoverflow.netmonitor.dbus

/**
 * @param value raw value passed to the D-Bus api
 *
 * @author Cedric Hammes
 * @since  28/04/2026
 *
 * @see [DBusBusType, D-Bus: Shared constants](https://dbus.freedesktop.org/doc/api/html/group__DBusShared.html#ga980320deb96476bee7555edcdebc3528)
 */
internal enum class EnumBusType(internal val value: Int) {
    SESSION(0),
    SYSTEM(1),
    STARTER(2);
}
