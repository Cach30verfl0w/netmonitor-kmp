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
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlin.time.Duration

/**
 * @param library the reference to the loaded D-Bus shared library
 * @param handle  the handle to the D-Bus connection
 *
 * @author Cedric Hammes
 * @since  01/05/2026
 *
 * @see [D-Bus: DBusConnection](https://dbus.freedesktop.org/doc/api/html/group__DBusConnection.html)
 */
internal class DBusConnection(
    private val library: DBusSharedLibrary,
    private val handle: COpaquePointer
) : AutoCloseable {

    /**
     * @author Cedric Hammes
     * @since  01/05/2026
     *
     * @see [dbus_connection_send_with_reply_and_block, D-Bus: DBusConnection](https://dbus.freedesktop.org/doc/api/html/group__DBusConnection.html#ga8d6431f17a9e53c9446d87c2ba8409f0)
     */
    fun sendWithReplyAndBlock(message: DBusMessage, timeout: Duration): Result<DBusMessage> = memScoped {
        val error = alloc<DBusError>()
        val response = library.findFunctionOrThrow<SendWithReplyAndBlock>("dbus_connection_send_with_reply_and_block")
            .invoke(handle, message.handle, timeout.inWholeMilliseconds.toInt(), error.ptr)
        if (response == null) {
            val exception = library.toException(error.ptr)
            library.errorFree(error.ptr)
            return Result.failure(exception)
        }

        Result.success(DBusMessage.owned(library, response))
    }

    /**
     * @author Cedric Hammes
     * @since  01/05/2026
     *
     * @see [dbus_bus_add_match, D-Bus: Message bus APIs](https://dbus.freedesktop.org/doc/api/html/group__DBusBus.html#ga4eb6401ba014da3dbe3dc4e2a8e5b3ef)
     */
    fun addMatch(matchRule: DBusMatchRule): Result<Unit> = memScoped {
        val error = alloc<DBusError>()
        val stringPtr = matchRule.toString().cstr.ptr
        library.findFunctionOrThrow<AddMatch>("dbus_bus_add_match").invoke(handle, stringPtr, error.ptr)
        if (library.errorIsSet(error.ptr)) {
            val exception = library.toException(error.ptr)
            library.errorFree(error.ptr)
            return Result.failure(exception)
        }

        Result.success(Unit)
    }


    /**
     * @author Cedric Hammes
     * @since  01/05/2026
     *
     * @see [dbus_connection_add_filter, D-Bus: DBusConnection](https://dbus.freedesktop.org/doc/api/html/group__DBusConnection.html#gae00f581e5487408cb294bf71826aff86)
     */
    fun addFilter(filter: (DBusConnection, DBusMessage) -> Boolean): Boolean {
        val filterRef = StableRef.create(this to filter)
        val messageHandler = staticCFunction { connection: COpaquePointer, message: COpaquePointer, userData: COpaquePointer ->
            val (thisClass, function) = userData.asStableRef<Pair<DBusConnection, (DBusConnection, DBusMessage) -> Boolean>>()
                .get()
            memScoped {
                when (function.invoke(
                    DBusConnection(thisClass.library, connection),
                    DBusMessage.owned(thisClass.library, message)
                )) {
                    false -> 0
                    true -> 1
                }
            }
        }

        val freeStableRef = staticCFunction { userData: COpaquePointer ->
            userData.asStableRef<Pair<DBusSharedLibrary, (COpaquePointer, COpaquePointer) -> Boolean>>().dispose()
        }

        val success = library.findFunctionOrThrow<AddFilter>("dbus_connection_add_filter")
            .invoke(handle, messageHandler, filterRef.asCPointer(), freeStableRef)
        if (!success) {
            filterRef.dispose()
        }

        return success
    }

    /**
     * @author Cedric Hammes
     * @since  01/05/2026
     *
     * @see [dbus_connection_read_write_dispatch, D-Bus: DBusConnection](https://dbus.freedesktop.org/doc/api/html/group__DBusConnection.html#ga580d8766c23fe5f49418bc7d87b67dc6)
     */
    fun readWriteDispatch(timeout: Duration): Boolean =
        library.findFunctionOrThrow<ReadWriteDispatch>("dbus_connection_read_write_dispatch")
            .invoke(handle, timeout.inWholeMilliseconds.toInt())

    override fun close(): Unit = library.findFunctionOrThrow<Unref>("dbus_connection_unref").invoke(handle)

    companion object {
        typealias DBusHandleMessageFunction = CPointer<CFunction<(COpaquePointer, COpaquePointer, COpaquePointer) -> Int>>
        typealias DBusFreeFunction = CPointer<CFunction<(COpaquePointer) -> Unit>>

        typealias AddMatch = (COpaquePointer, CPointer<ByteVar>, CPointer<DBusError>) -> Unit
        typealias AddFilter = (COpaquePointer, DBusHandleMessageFunction, COpaquePointer, DBusFreeFunction) -> Boolean
        typealias SendWithReplyAndBlock = (COpaquePointer, COpaquePointer, Int, COpaquePointer) -> COpaquePointer?
        typealias ReadWriteDispatch = (COpaquePointer, Int) -> Boolean
        typealias Unref = (COpaquePointer) -> Unit
    }
}
