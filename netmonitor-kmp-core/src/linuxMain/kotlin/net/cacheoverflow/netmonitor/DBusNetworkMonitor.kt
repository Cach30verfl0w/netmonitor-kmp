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

import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import net.cacheoverflow.netmonitor.dbus.DBusConnection
import net.cacheoverflow.netmonitor.dbus.DBusMatchRule
import net.cacheoverflow.netmonitor.dbus.DBusMessage
import net.cacheoverflow.netmonitor.dbus.DBusSharedLibrary
import net.cacheoverflow.netmonitor.dbus.EnumBusType
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration.Companion.seconds

/**
 * @author Cedric Hammes
 * @since  28/04/2026
 */
@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class, ExperimentalAtomicApi::class)
internal class DBusNetworkMonitor(private val library: DBusSharedLibrary) : AbstractObservable<NetworkMonitor.Callback>(), NetworkMonitor {
    private val connection: DBusConnection = library.createConnection(EnumBusType.SYSTEM).getOrThrow()
    private val isClosed: AtomicBoolean = AtomicBoolean(false)

    private val coroutineContext: CloseableCoroutineDispatcher = newSingleThreadContext("DBusNetworkMonitor")
    private val supervisorJob: Job = SupervisorJob()
    private val coroutineScope: CoroutineScope = CoroutineScope(coroutineContext + supervisorJob)

    init {
        coroutineScope.launch {
            while (!isClosed.load()) {
                if (connection.readWriteDispatch(10.seconds)) {
                    continue
                }

                yield()
            }
        }

        connection.addMatch(DBusMatchRule.NETWORK_MANAGER_STATE_CHANGED)
        connection.addFilter { _, message ->
            message.allocateIteratorWithPlacement(true, nativeHeap).use { iterator ->
                val networkStatus = computeNetworkState(iterator) ?: return@use

                notifyCallbacks { callback ->
                    callback.networkStateChanged(networkStatus)
                }
            }

            false
        }
    }

    override fun close() = runBlocking {
        if (isClosed.compareAndExchange(expectedValue = false, newValue = true)) return@runBlocking

        supervisorJob.join()
        this@DBusNetworkMonitor.coroutineContext.close()
        connection.close()
        library.close()
    }

    override fun registerCallback(callback: NetworkMonitor.Callback) {
        getProperty("State") { computeNetworkState(it) }.getOrNull()?.let { networkState ->
            callback.networkStateChanged(networkState)
        }

        super.registerCallback(callback)
    }

    private fun <T> getProperty(name: String, closure: (DBusMessage.Iterator) -> T): Result<T> = memScoped {
        library.allocateMessage("org.freedesktop.NetworkManager", "org.freedesktop.DBus.Properties", "Get")
            .use { request ->
                request.allocateIterator(false).use { requestIterator ->
                    requestIterator.appendString("org.freedesktop.NetworkManager")
                    requestIterator.appendString(name)
                }

                connection.sendWithReplyAndBlock(request, 10.seconds)
                    .fold(onFailure = { Result.failure(it) }, onSuccess = { response ->
                        response.use { response ->
                            response.allocateIterator(true, nativeHeap).let { responseIterator ->
                                responseIterator.recurse()?.let { Result.success(closure(it)) } ?: Result.failure(
                                    IllegalArgumentException("Unable to create content iterator")
                                )
                            }
                        }
                    })
            }
    }

    private fun isConnectionMetered(): Result<Boolean> = getProperty("Metered") { iterator ->
        iterator.readUInt()?.rem(4U)?.let { value ->
            value != 0U
        } ?: false
    }

    private fun computeNetworkState(messageIterator: DBusMessage.Iterator): NetworkState? {
        val networkState = messageIterator.readUInt()?.let { NetworkManagerState.fromInt(it.toInt()) } ?: return null
        return when (networkState) {
            NetworkManagerState.CONNECTED_LOCAL, NetworkManagerState.CONNECTED_SITE, NetworkManagerState.CONNECTED_GLOBAL -> {
                val isMetered = isConnectionMetered().getOrNull() ?: return null
                NetworkState.Online(isMetered, networkState != NetworkManagerState.CONNECTED_LOCAL)
            }

            else -> NetworkState.Offline
        }
    }
}
