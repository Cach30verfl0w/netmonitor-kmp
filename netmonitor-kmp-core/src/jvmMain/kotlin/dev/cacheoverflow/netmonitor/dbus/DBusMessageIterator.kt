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

package dev.cacheoverflow.netmonitor.dbus

import dev.cacheoverflow.netmonitor.foreign.asCString
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class DBusMessageIterator private constructor(
    private val library: DBusNativeLibrary,
    private val arena: Arena,
    private val native: MemorySegment,
    private val canAppend: Boolean
) {
    private val getArgType = library.find("dbus_message_iter_get_arg_type", GET_TYPE)
    private val appendBasic = library.find("dbus_message_iter_append_basic", APPEND)
    private val getBasic = library.find("dbus_message_iter_get_basic", GET_BASIC)
    private val recurse = library.find("dbus_message_iter_recurse", RECURSE)
    private val next = library.find("dbus_message_iter_next", NEXT)

    private fun next(): Boolean = next.invokeExact(native) as Boolean
    fun recurse(): DBusMessageIterator? {
        if ((getArgType.invokeExact(native) as Int) != 'v'.code) {
            return null
        }

        val subIter = arena.allocate(64)
        recurse.invokeExact(native, subIter) as Unit
        next()

        return DBusMessageIterator(library, arena, subIter, canAppend)
    }

    fun appendString(string: String): DBusMessageIterator {
        val ptr = arena.allocateFrom(string)
        val pointerToPtr = arena.allocate(ValueLayout.ADDRESS)
        pointerToPtr.set(ValueLayout.ADDRESS, 0, ptr)
        appendBasic.invokeExact(native, 's'.code, pointerToPtr) as Boolean
        return this
    }

    fun readInt(): Int? = read(arrayOf('i'.code, 'u'.code), ValueLayout.JAVA_INT) { ptr -> ptr.get(ValueLayout.JAVA_INT, 0) }
    fun readString(): String? = read(arrayOf('s'.code), ValueLayout.ADDRESS) { ptr -> ptr.get(ValueLayout.ADDRESS, 0).asCString }

    private fun <T> read(codes: Array<Int>, layoutType: ValueLayout, closure: (MemorySegment) -> T): T? {
        if (!codes.contains(getArgType.invokeExact(native) as Int)) {
            return null
        }

        return Arena.ofConfined().use { arena ->
            val valuePtr = arena.allocate(layoutType)
            getBasic.invokeExact(native, valuePtr)
            next()

            closure(valuePtr)
        }
    }

    companion object {
        private val APPEND = FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
        private val GET_BASIC = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        private val RECURSE = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        private val GET_TYPE = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
        private val NEXT = FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS)

        @JvmStatic
        fun read(message: DBusMessage): DBusMessageIterator? {
            val arena = Arena.ofAuto()
            val memory = arena.allocate(64)
            if (!(message.messageIterInit.invokeExact(message.native, memory) as? Boolean ?: false)) {
                return null
            }

            return DBusMessageIterator(message.library, arena, memory, false)
        }

        @JvmStatic
        fun create(message: DBusMessage): DBusMessageIterator {
            val arena = Arena.ofAuto()
            val memory = arena.allocate(64)
            message.messageIterInitAppend.invokeExact(message.native, memory)
            return DBusMessageIterator(message.library, arena, memory, true)
        }
    }
}
