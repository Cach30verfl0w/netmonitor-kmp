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

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface Observable {
    fun registerCallback(callback: NetworkStateCallback)
    fun unregisterCallback(callback: NetworkStateCallback)
}

fun interface NetworkStateCallback {
    fun networkStateChanged(state: NetworkState)
}

@OptIn(InternalCoroutinesApi::class, ExperimentalAtomicApi::class)
internal abstract class AbstractObservable : Observable {
    private val state: AtomicReference<NetworkState> = AtomicReference(NetworkState.Unknown)
    private val lock: SynchronizedObject = SynchronizedObject()
    private val callbacks: ArrayList<NetworkStateCallback> = ArrayList()

    override fun registerCallback(callback: NetworkStateCallback): Unit = synchronized(lock) {
        if (!callbacks.contains(callback)) {
            callback.networkStateChanged(state.load())
            callbacks.add(callback)
        }
    }

    override fun unregisterCallback(callback: NetworkStateCallback): Unit = synchronized(lock) {
        callbacks.remove(callback)
    }

    protected fun notifyCallbacksNoDelay(state: NetworkState) {
        if (this.state.exchange(state) == state) {
            return
        }

        synchronized(lock) {
            callbacks.forEach {
                it.networkStateChanged(state)
            }
        }
    }

    protected suspend fun notifyCallbacks(state: NetworkState, pollingRate: Duration = 5.seconds) {
        if (this.state.exchange(state) == state) {
            delay(pollingRate)
            return
        }

        synchronized(lock) {
            callbacks.forEach {
                it.networkStateChanged(state)
            }
        }
    }
}
