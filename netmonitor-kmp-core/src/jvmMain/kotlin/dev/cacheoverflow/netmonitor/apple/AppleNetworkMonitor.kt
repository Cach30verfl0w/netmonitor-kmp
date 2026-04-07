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

package dev.cacheoverflow.netmonitor.apple

import dev.cacheoverflow.netmonitor.NetworkMonitor
import dev.cacheoverflow.netmonitor.NetworkState
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.foreign.Arena

internal class AppleNetworkMonitor : NetworkMonitor {
    private val arena: Arena = Arena.ofAuto()
    private val systemLibrary: AppleSystemLibrary = AppleSystemLibrary.load(arena).getOrThrow()
    private val networkLibrary: AppleNetworkLibrary = AppleNetworkLibrary.load(arena).getOrThrow()

    private val networkPathMonitor: NetworkPathMonitor = networkLibrary.createPathMonitor()
    private val dispatchQueue: AppleDispatchQueue = systemLibrary.createDispatchQueue(requireNotNull(this::class.qualifiedName))

    override val isAvailable: Boolean = true
    override val state: MutableStateFlow<NetworkState> = MutableStateFlow(NetworkState.Unknown)

    init {
        networkPathMonitor.setQueue(dispatchQueue)
        networkPathMonitor.setHandler {
            state.value = when (it.getStatus()) {
                NetworkPath.Status.SATISFIED -> NetworkState.Online(it.getInterfaceType(), it.isExpensive())
                NetworkPath.Status.UNSATISFIED, NetworkPath.Status.SATISFIABLE -> NetworkState.Offline
                else -> NetworkState.Unknown
            }
        }

        networkPathMonitor.start()
    }

    override fun close() {
        networkPathMonitor.close()
        dispatchQueue.close()
        networkLibrary.close()
        systemLibrary.close()
    }
}
