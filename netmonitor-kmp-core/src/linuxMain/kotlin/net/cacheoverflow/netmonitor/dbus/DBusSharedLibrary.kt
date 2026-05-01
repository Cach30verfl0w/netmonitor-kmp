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

package net.cacheoverflow.netmonitor.dbus

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKStringFromUtf8
import kotlinx.cinterop.value
import net.cacheoverflow.netmonitor.SharedLibrary

/**
 * @author Cedric Hammes
 * @since  28/04/2026
 */
internal class DBusSharedLibrary(library: SharedLibrary) : SharedLibrary(library.handle) {

    /**
     * @author Cedric Hammes
     * @since  29/04/2026
     *
     * @see [dbus_message_new_method_call, D-Bus: DBusMessage](https://dbus.freedesktop.org/doc/api/html/group__DBusMessage.html#ga98ddc82450d818138ef326a284201ee0)
     */
    context(scope: MemScope)
    fun allocateMessage(
        destination: String,
        iface: String,
        method: String,
        path: String = "/${destination.replace(".", "/")}",
    ): DBusMessage = with(scope) {
        val newMethodCall = findFunctionOrThrow<MessageNewMethodCall>("dbus_message_new_method_call")
        DBusMessage.owned(
            library = this@DBusSharedLibrary,
            handle = newMethodCall.invoke(destination.cstr.ptr, path.cstr.ptr, iface.cstr.ptr, method.cstr.ptr)
        )
    }


    /**
     * @author Cedric Hammes
     * @since  01/05/2026
     *
     * @see [dbus_bus_get, D-Bus: Message bus APIs](https://dbus.freedesktop.org/doc/api/html/group__DBusBus.html#ga77ba5250adb84620f16007e1b023cf26)
     */
    fun createConnection(type: EnumBusType): Result<DBusConnection> = memScoped {
        val error = allocPointerTo<DBusError>()
        val connection = findFunctionOrThrow<BusGet>("dbus_bus_get").invoke(type.value, error.ptr) ?: run {
            val errorPointer = requireNotNull(error.value)
            return Result.failure(toException(errorPointer))
        }

        return Result.success(DBusConnection(this@DBusSharedLibrary, connection))
    }

    // /**
    //  * @param type the type of the bus to connect to.
    //  * @return     the pointer to the connection or an error.
    //  *
    //  * @see [dbus_bus_get, D-Bus: Message bus APIs](https://dbus.freedesktop.org/doc/api/html/group__DBusBus.html#ga77ba5250adb84620f16007e1b023cf26)
    //  *
    //  * @author Cedric Hammes
    //  * @since  28/04/2026
    //  */
    // fun busGet(type: EnumBusType): Result<COpaquePointer> = memScoped {
    //     val error = allocPointerTo<DBusError>()
    //     val connection = handle.findFunctionOrThrow<BusGet>("dbus_bus_get").invoke(type.value, error.ptr) ?: run {
    //         val errorPointer = requireNotNull(error.value)
    //         return Result.failure(toException(errorPointer))
    //     }
//
    //     return Result.success(connection)
    // }

    fun errorFree(addr: CPointer<DBusError>): Unit = findFunctionOrThrow<ErrorFree>("dbus_error_free").invoke(addr)
    fun errorIsSet(addr: CPointer<DBusError>): Boolean =
        findFunctionOrThrow<ErrorIsSet>("dbus_error_is_set").invoke(addr)

    fun toException(error: CPointer<DBusError>): RuntimeException = when (errorIsSet(error)) {
        false -> RuntimeException("Unexpected error occurred while initializing D-Bus connection")
        true -> error.pointed.let { error ->
            val name = error.name.value?.toKStringFromUtf8() ?: "Unknown"
            val message = error.message.value?.toKStringFromUtf8() ?: "Unexpected error"
            RuntimeException("${name}: $message")
        }
    }

    companion object {
        typealias BusGet = (Int, CPointer<CPointerVar<DBusError>>) -> COpaquePointer?
        typealias MessageNewMethodCall = (CPointer<ByteVar>, CPointer<ByteVar>, CPointer<ByteVar>, CPointer<ByteVar>) -> COpaquePointer

        typealias ErrorFree = (CPointer<DBusError>) -> Unit
        typealias ErrorIsSet = (CPointer<DBusError>) -> Boolean

        val libraries: Array<String> = arrayOf("libdbus-1.so.3", "libdbus-1.so")
        fun open(): Result<DBusSharedLibrary> = open(libraries).map(::DBusSharedLibrary)
    }
}
