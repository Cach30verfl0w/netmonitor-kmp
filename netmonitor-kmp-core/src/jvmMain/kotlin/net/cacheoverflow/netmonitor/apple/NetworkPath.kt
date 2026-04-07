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

package net.cacheoverflow.netmonitor.apple

import net.cacheoverflow.netmonitor.NetworkType
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle

internal class NetworkPath internal constructor(library: AppleNetworkLibrary, private val native: MemorySegment) {
    private val getStatus: MethodHandle = library.find("nw_path_get_status", GET_STATUS)
    private val isExpensive: MethodHandle = library.find("nw_path_is_expensive", IS_EXPENSIVE)
    private val usesInterfaceType: MethodHandle = library.find("nw_path_uses_interface_type", USES_INTERFACE_TYPE)

    fun getStatus(): Status = Status.fromInt(getStatus.invokeExact(native) as Int)
    fun isExpensive(): Boolean = isExpensive.invokeExact(native) as Boolean
    fun getInterfaceType(): NetworkType = when {
        usesInterfaceType.invokeExact(native, 1) as Boolean -> NetworkType.WIFI
        usesInterfaceType.invokeExact(native, 2) as Boolean -> NetworkType.CELLULAR
        usesInterfaceType.invokeExact(native, 3) as Boolean -> NetworkType.ETHERNET
        else -> NetworkType.UNKNOWN
    }

    enum class Status(val value: Int) {
        INVALID(0),
        SATISFIED(1),
        UNSATISFIED(2),
        SATISFIABLE(3);

        companion object {
            fun fromInt(v: Int): Status = entries.find { it.value == v } ?: INVALID
        }
    }

    companion object {
        private val GET_STATUS = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
        private val IS_EXPENSIVE = FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS)
        private val USES_INTERFACE_TYPE = FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
    }
}