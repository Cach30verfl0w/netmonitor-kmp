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
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized

interface Observable<T> {
    fun registerCallback(callback: T)
    fun unregisterCallback(callback: T)
}

@OptIn(InternalCoroutinesApi::class)
internal abstract class AbstractObservable<T> : Observable<T> {
    private val lock: SynchronizedObject = SynchronizedObject()
    protected val callbacks: MutableList<T> = ArrayList()

    override fun registerCallback(callback: T): Unit = synchronized(lock) {
        if (!callbacks.contains(callback))
            callbacks.add(callback)
    }

    override fun unregisterCallback(callback: T): Unit = synchronized(lock) {
        callbacks.remove(callback)
    }

    protected fun notifyCallbacks(closure: (T) -> Unit): Unit = synchronized(lock) {
        callbacks.forEach(closure)
    }
}
