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

@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package net.cacheoverflow.netmonitor

import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.invoke

typealias CallbackFunction = CFunction<(Int) -> Unit>

@CName("netmonitor_network_monitor_create")
fun allocateNetworkMonitor(): COpaquePointer = StableRef.create(NetworkMonitor()).asCPointer()

@CName("netmonitor_network_monitor_dispose")
fun disposeNetworkMonitor(addr: COpaquePointer) {
    val networkMonitor = addr.asStableRef<NetworkMonitor>()
    networkMonitor.get().close()
    networkMonitor.dispose()
}

@CName("netmonitor_network_monitor_register_callback")
fun registerNetworkMonitorCallback(handle: COpaquePointer, closure: CPointer<CallbackFunction>): COpaquePointer {
    val networkMonitor = handle.asStableRef<NetworkMonitor>().get()
    val callback = NetworkStateCallback { newNetworkState ->
        closure.invoke(newNetworkState.asPacked())
    }

    networkMonitor.registerCallback(callback)
    return StableRef.create(callback).asCPointer()
}

@CName("netmonitor_network_monitor_unregister_callback")
fun unregisterNetworkMonitorCallback(handle: COpaquePointer, callback: COpaquePointer) {
    val networkMonitor = handle.asStableRef<NetworkMonitor>().get()
    val callback = callback.asStableRef<NetworkStateCallback>()
    networkMonitor.unregisterCallback(callback.get())
    callback.dispose()
}
