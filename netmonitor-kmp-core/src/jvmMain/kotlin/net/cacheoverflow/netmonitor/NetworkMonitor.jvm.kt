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

import net.cacheoverflow.netmonitor.apple.AppleNetworkMonitor
import net.cacheoverflow.netmonitor.dbus.DBusNetworkMonitor

actual fun NetworkMonitor(): NetworkMonitor {
    val system = System.getProperty("os.name").lowercase()
    return when {
        system.contains("nix") || system.contains("nux") || system.contains("aix") -> DBusNetworkMonitor()
        system.contains("mac") -> AppleNetworkMonitor()
        else -> throw UnsupportedOperationException("Unable to create a network monitor for '$system'")
    }
}
