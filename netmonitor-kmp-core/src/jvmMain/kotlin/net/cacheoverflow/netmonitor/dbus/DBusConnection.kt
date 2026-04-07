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

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * @author Cedric Hammes
 * @since  03/04/2026
 *
 * @see <a href="https://dbus.freedesktop.org/doc/api/html/group__DBusConnection.html">D-Bus: DBusConnection</a>
 */
internal class DBusConnection(private val library: DBusNativeLibrary, private val native: MemorySegment) : AutoCloseable {
    private val sendWithReplyAndBlock: MethodHandle = library.find("dbus_connection_send_with_reply_and_block", SEND_WITH_REPLY_AND_BLOCK)
    private val readWriteDispatch: MethodHandle = library.find("dbus_connection_read_write_dispatch", READ_WRITE_DISPATCH)
    private val messageNewMethodCall = library.find("dbus_message_new_method_call", MESSAGE_NEW_METHOD_CALL)
    private val unref: MethodHandle = library.find("dbus_connection_unref", UNREF)
    private val busAddMatch = library.find("dbus_bus_add_match", BUS_ADD_MATCH)

    private val removeFilter: MethodHandle = library.find("dbus_connection_remove_filter", REMOVE_FILTER)
    private val addFilter: MethodHandle = library.find("dbus_connection_add_filter", ADD_FILTER)
    private val activeFilters = mutableMapOf<FilterHandler, Pair<FilterHandlerBridge, MemorySegment>>()

    private val filterArena: Arena = Arena.ofAuto()
    private var eventLoopJob: Job? = null

    /**
     * This function creates a stub for the handler and adds the handler to the D-Bus connection.
     *
     * @param closure the reference to the filter handler
     * @return        returns true if successfully added
     *
     * @author Cedric Hammes
     * @since  03/04/2026
     */
    fun addFilter(closure: FilterHandler): Boolean {
        if (activeFilters.containsKey(closure))
            return false

        val handler = FilterHandlerBridge(library, closure)
        val memSegmentClass = MemorySegment::class.java
        val methodType = MethodType.methodType(Boolean::class.java, memSegmentClass, memSegmentClass, memSegmentClass)
        val method = MethodHandles.lookup().findVirtual(FilterHandlerBridge::class.java, "stub", methodType).bindTo(handler)
        val stub = library.linker.upcallStub(method, HANDLER_DESC, filterArena)
        if (!(addFilter.invokeExact(native, stub, MemorySegment.NULL, MemorySegment.NULL) as Boolean)) {
            return false
        }

        activeFilters[closure] = Pair(handler, stub)
        return true
    }

    /**
     * This function loads the stub for the handler and removes the handler from the D-Bus connection.
     *
     * @param handler to reference to the filter handler
     * @return        returns true if successfully removed
     *
     * @author Cedric Hammes
     * @since  03/04/2026
     */
    fun removeFilter(handler: FilterHandler): Boolean {
        if (!activeFilters.containsKey(handler))
            return false

        val (_, stub) = requireNotNull(activeFilters.remove(handler))
        return removeFilter.invokeExact(native, stub, MemorySegment.NULL) as Boolean
    }

    /**
     * This function creates a new D-Bus method call and awaits the reply while blocking.
     *
     * @param metadata the metadata describes the method to call
     * @param timeout  the timeout duration of the send and reply operation
     * @return         the D-Bus reply message or an error
     *
     * @author Cedric Hammes
     * @since  03/04/2026
     */
    fun call(metadata: MethodCall, timeout: Duration = 5.seconds, argsBuilder: DBusMessageIterator.() -> Unit): Result<DBusMessage> =
        Arena.ofConfined().use { tempArena ->
            val message = metadata.createMessage(messageNewMethodCall, library, tempArena)
            argsBuilder(message.iterator)
            message.use { message ->
                val error = tempArena.allocate(64).fill(0)
                val timeoutMillis = timeout.inWholeMilliseconds.toInt()
                val reply = sendWithReplyAndBlock.invokeExact(native, message.native, timeoutMillis, error) as MemorySegment
                if (reply == MemorySegment.NULL) {
                    val exception = library.errorToException(error)
                    library.freeError(error)
                    return@use Result.failure(exception)
                }

                Result.success(DBusMessage.of(library, reply))
            }
        }

    /**
     * This function starts the event loop of the D-Bus connection on the specified coroutine scope. This job is being canceled when the
     * connection itself is being closed.
     *
     * @param scope           the coroutine scope to launch the event loop job in
     * @param dispatchTimeout the timeout duration value of the read-write dispatch call
     *
     * @author Cedric Hammes
     * @since  03/04/2026
     *
     * @see <a href="https://dbus.freedesktop.org/doc/api/html/group__DBusConnection.html#ga580d8766c23fe5f49418bc7d87b67dc6">dbus_connection_read_write_dispatch, D-Bus: DBusConnection</a>
     */
    fun startEventLoop(scope: CoroutineScope, dispatchTimeout: Duration = 100.milliseconds) {
        if (eventLoopJob != null) {
            throw IllegalStateException("Unable to start new event loop job: A job is already running")
        }

        scope.launch(CoroutineName("dbus-connection-event-loop")) {
            while (isActive) {
                if (readWriteDispatch.invokeExact(native, dispatchTimeout.inWholeMilliseconds.toInt()) as? Boolean == false) {
                    break
                }

                yield()
            }
        }
    }

    /**
     * @param rule the match rule that should be added
     *
     * @author Cedric Hammes
     * @since  03/04/2026
     */
    fun addMatchRule(rule: MatchRule) = Arena.ofConfined().use { arena ->
        busAddMatch.invokeExact(native, arena.allocateFrom(rule.toString()), MemorySegment.NULL) as Unit
    }

    override fun close() {
        eventLoopJob?.cancel()
        activeFilters.clear()
        unref.invokeExact(native) as Unit
    }

    companion object {
        // @formatter:off
        private val SEND_WITH_REPLY_AND_BLOCK = FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
        private val MESSAGE_NEW_METHOD_CALL = FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        private val READ_WRITE_DISPATCH = FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        private val BUS_ADD_MATCH = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        private val UNREF = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)

        private val ADD_FILTER = FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        private val REMOVE_FILTER = FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        private val HANDLER_DESC = FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        // @formatter:on
    }

    fun interface FilterHandler {
        operator fun invoke(connection: DBusConnection, message: DBusMessage): Boolean
    }

    private class FilterHandlerBridge(private val library: DBusNativeLibrary, private val closure: FilterHandler) {
        @Suppress("Unused")
        fun stub(connection: MemorySegment, message: MemorySegment, userData: MemorySegment): Boolean {
            return closure(DBusConnection(library, connection), DBusMessage.of(library, message))
        }
    }
}
