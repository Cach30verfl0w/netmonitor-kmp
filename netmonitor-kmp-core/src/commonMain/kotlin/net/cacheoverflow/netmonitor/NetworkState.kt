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
import kotlin.jvm.JvmStatic

/**
 * @author Cedric Hammes
 * @since  07/04/2026
 */
@Immutable
sealed interface NetworkState {
    fun asPacked(): Int

    @Immutable
    data class Online(val isMetered: Boolean, val canReachRemoteDevices: Boolean) : NetworkState {
        override fun asPacked(): Int {
            var value = 0b0000_0001_0000_0000
            if (isMetered) value = value or (1 shl 7)
            if (canReachRemoteDevices) value = value or (1 shl 6)
            return value
        }
    }

    data object Offline : NetworkState {
        override fun asPacked(): Int = 0b0000_00010_0000_0000
    }

    data object CaptivePortal : NetworkState {
        override fun asPacked(): Int = 0b0000_00011_0000_0000
    }

    data object Unknown : NetworkState {
        override fun asPacked(): Int = 0b0000_00100_0000_0000
    }

    companion object {
        @JvmStatic
        fun fromPacked(value: Int): NetworkState {
            val id = (value shr 8) and 0xFF

            return when (id) {
                1 -> {
                    val isMetered = (value and (1 shl 7)) != 0
                    val canReachRemoteDevices = (value and (1 shl 6)) != 0
                    Online(isMetered, canReachRemoteDevices)
                }
                2 -> Offline
                3 -> CaptivePortal
                else -> Unknown
            }
        }
    }
}
