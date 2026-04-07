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

import java.lang.AutoCloseable
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/**
 * @author Cedric Hammes
 * @since  03/04/2026
 */
internal class DBusMessage private constructor(
    internal val library: DBusNativeLibrary,
    internal val native: MemorySegment
) : AutoCloseable {
    internal val messageIterInitAppend = library.find("dbus_message_iter_init_append", MESSAGE_ITER_INIT_APPEND)
    internal val messageIterInit = library.find("dbus_message_iter_init", MESSAGE_ITER_INIT)
    private val messageUnref = library.find("dbus_message_unref", MESSAGE_UNREF)

    val iterator: DBusMessageIterator = DBusMessageIterator.read(this) ?: DBusMessageIterator.create(this)

    override fun close() {
        if (native != MemorySegment.NULL) {
            messageUnref.invokeExact(native)
        }
    }

    companion object {
        private val MESSAGE_ITER_INIT = FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        private val MESSAGE_ITER_INIT_APPEND = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        private val MESSAGE_UNREF = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)

        @JvmStatic
        fun of(library: DBusNativeLibrary, handle: MemorySegment): DBusMessage = DBusMessage(library, handle)
    }
}
