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

import dev.karmakrafts.cominterop.ComRuntime
import dev.karmakrafts.cominterop.new
import kotlinx.cinterop.convert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import net.cacheoverflow.netmonitor.nlm.INetworkCostManager
import net.cacheoverflow.netmonitor.nlm.INetworkListManager
import net.cacheoverflow.netmonitor.nlm.NetworkListManager
import net.cacheoverflow.netmonitor.nlm.NlmConnectionCost
import net.cacheoverflow.netmonitor.nlm.NlmConnectivity
import net.cacheoverflow.netmonitor.nlm.NlmEnumNetwork
import net.cacheoverflow.netmonitor.nlm.NlmNetworkCategory
import platform.windows.CLSCTX_ALL
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * @author Alexander Hinze
 * @since 01/05/2026
 */
@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class, ExperimentalAtomicApi::class)
internal class NlmNetworkMonitor : AbstractObservable(), NetworkMonitor {
    private val coroutineScope: CoroutineScope = CoroutineScope(newSingleThreadContext("NLMNetworkMonitor") + SupervisorJob())
    private val isRunning: AtomicBoolean = AtomicBoolean(true)

    private fun determineState(manager: INetworkListManager, costManager: INetworkCostManager): NetworkState {
        val enumNetworks = manager.getNetworks(NlmEnumNetwork.CONNECTED)
        val networks = enumNetworks.toList()
        enumNetworks.release()
        // We prefer the first connection with internet as the default, otherwise use the first connected
        val network = networks.find { network ->
            val connectivity = network.connectivity
            connectivity and NlmConnectivity.IPV4_INTERNET != 0 || connectivity and NlmConnectivity.IPV6_INTERNET != 0
        } ?: networks.firstOrNull() ?: return NetworkState.Offline
        // If we are online, construct an appropriate state
        val category = network.category
        val canReachRemoteDevices = category and NlmNetworkCategory.PRIVATE != 0 || category and NlmNetworkCategory.DOMAIN_AUTHENTICATED != 0
        val state = NetworkState.Online(
            isMetered = costManager.cost.toInt() and NlmConnectionCost.UNRESTRICTED == 0,
            canReachRemoteDevices = canReachRemoteDevices
        )
        network.release()
        return state
    }

    private val pollingJob: Job = coroutineScope.launch {
        ComRuntime.init()
        val manager = NetworkListManager.new<INetworkListManager, _, _>(
            clsContext = CLSCTX_ALL.convert()
        )
        val costManager = manager.asCom<INetworkCostManager, _>(INetworkCostManager)
        while (isRunning.load()) {
            if (!notifyCallbacks(determineState(manager, costManager)))
                delay(5.seconds)
        }
        costManager.release()
        manager.release()
        ComRuntime.uninit()
    }

    override fun close() {
        isRunning.store(false)
        runBlocking {
            pollingJob.join()
        }
        coroutineScope.cancel()
    }
}
