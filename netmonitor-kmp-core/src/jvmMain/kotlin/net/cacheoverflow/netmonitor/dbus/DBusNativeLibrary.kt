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

package net.cacheoverflow.netmonitor.dbus

import net.cacheoverflow.netmonitor.foreign.NativeLibrary
import net.cacheoverflow.netmonitor.foreign.asCString
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle

/**
 * @author Cedric Hammes
 * @since  03/04/2026
 */
internal class DBusNativeLibrary(private val library: NativeLibrary) : NativeLibrary by library {
    private val busGet: MethodHandle = find("dbus_bus_get", BUS_GET)
    private val errorFree: MethodHandle = find("dbus_error_free", ERROR_FREE)
    private val errorIsSet: MethodHandle = find("dbus_error_is_set", ERROR_IS_SET)
    private val arena: Arena = Arena.ofAuto()

    fun freeError(error: MemorySegment): Unit = errorFree.invokeExact(error) as Unit
    fun isErrorSet(error: MemorySegment): Boolean = errorIsSet.invokeExact(error) as Boolean

    /**
     * @return the newly created D-Bus connection or an error
     *
     * @author Cedric Hammes
     * @since  03/04/2026
     */
    fun createConnection(): Result<DBusConnection> {
        val error = arena.allocate(256).fill(0)
        val connection = busGet.invoke(1, error) as MemorySegment
        if (connection == MemorySegment.NULL) {
            val exception = errorToException(error)
            errorFree.invokeExact(error)
            return Result.failure(exception)
        }

        return Result.success(DBusConnection(this, connection))
    }

    fun errorToException(error: MemorySegment) = when (isErrorSet(error)) {
        false -> RuntimeException("Unexpected error occurred while initializing D-Bus connection")
        true -> {
            val name = error.get(ValueLayout.ADDRESS, 0).asCString
            val message = error.get(ValueLayout.ADDRESS, 8).asCString
            RuntimeException("Error ($name): $message")
        }
    }

    override fun close() {
        library.close()
    }

    companion object {
        private val BUS_GET = FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
        private val ERROR_FREE = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        private val ERROR_IS_SET = FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS)

        @JvmStatic
        fun from(library: NativeLibrary): DBusNativeLibrary = DBusNativeLibrary(library)

        @JvmStatic
        fun load(arena: Arena = Arena.global(), linker: Linker = Linker.nativeLinker()): Result<DBusNativeLibrary> =
            NativeLibrary.load(arena, arrayOf("libdbus-1.so.3"), linker).map(DBusNativeLibrary::from)
    }
}