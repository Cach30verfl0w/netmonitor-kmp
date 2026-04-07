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

import androidx.compose.runtime.Immutable

/**
 * @author Cedric Hammes
 * @since  07/04/2026
 */
@Immutable
sealed interface NetworkType {
    object WiFi : NetworkType
    object Ethernet : NetworkType
    object Bluetooth : NetworkType
    object Unknown : NetworkType

    /**
     * Represents a cellular network connection.
     *
     * Note: To read generation and carrier name, you need to apply the permission `android.permission.READ_PHONE_STATE` to your application
     * and manually request the permission from the user. Otherwise, the generation field is unknown and the carrier name is null.
     *
     * @property generation the radio technology used (e.g. 5G, 4G). On Android, this data can be inaccurate.
     * @property carrier    the name of the service provider (e.g. "T-Mobile")
     *
     * @author Cedric Hammes
     * @since 07/04/2026
     */
    data class Cellular(val generation: Generation, val carrier: String?) : NetworkType {
        enum class Generation(private val label: String) {
            G2("2G"),
            G3("3G"),
            G4("4G"),
            G5("5G"),
            UNKNOWN("Unknown");

            override fun toString(): String = label
        }
    }
}
