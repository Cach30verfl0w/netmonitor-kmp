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
sealed interface NetworkState {
    data class Online(val type: NetworkType, val isMetered: Boolean) : NetworkState
    object Offline : NetworkState
    object CaptivePortal : NetworkState
    object Unknown : NetworkState
}