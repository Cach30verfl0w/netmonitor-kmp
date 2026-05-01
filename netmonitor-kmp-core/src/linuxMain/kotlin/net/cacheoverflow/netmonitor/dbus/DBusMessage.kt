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
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.CVariable
import kotlinx.cinterop.LongVar
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.NativeFreeablePlacement
import kotlinx.cinterop.NativePlacement
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.cstr
import kotlinx.cinterop.free
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKStringFromUtf8
import kotlinx.cinterop.value

/**
 * @author Cedric Hammes
 * @since  01/05/2026
 */
internal class DBusMessage(
    private val library: DBusSharedLibrary,
    internal val handle: COpaquePointer,
    private val owned: Boolean
) : AutoCloseable {
    val memberType: String
        get() = requireNotNull(library.findFunctionOrThrow<GetMember>("dbus_message_get_member").invoke(handle)?.toKStringFromUtf8())

    context(scope: MemScope)
    fun allocateIterator(read: Boolean, placement: NativePlacement? = null): Iterator =
        allocateIteratorWithPlacement(read, placement ?: scope)

    fun allocateIteratorWithPlacement(read: Boolean, placement: NativePlacement): Iterator = requireNotNull(
        when (read) {
            true -> Iterator.readFromMessage(placement, library, handle)
            false -> Iterator.appendToMessage(placement, library, handle)
        }
    )

    override fun close() {
        if (owned) {
            library.findFunctionOrThrow<Unref>("dbus_message_unref").invoke(handle)
        }
    }

    companion object {
        typealias NewMethodCall = (CPointer<ByteVar>, CPointer<ByteVar>, CPointer<ByteVar>, CPointer<ByteVar>) -> COpaquePointer
        typealias GetMember = (COpaquePointer) -> CPointer<ByteVar>?
        typealias Unref = (COpaquePointer) -> Unit

        internal fun borrowed(library: DBusSharedLibrary, handle: COpaquePointer): DBusMessage = DBusMessage(library, handle, false)
        internal fun owned(library: DBusSharedLibrary, handle: COpaquePointer): DBusMessage = DBusMessage(library, handle, true)
    }

    /**
     * @param memScope the memory scope to allocate data structures in
     * @param library  the reference to the D-Bus library handle
     * @param handle   the handle of the D-Bus message iterator
     *
     * @author Cedric Hammes
     * @since  01/05/2026
     */
    internal class Iterator(
        private val memScope: NativePlacement,
        private val library: DBusSharedLibrary,
        private val handle: COpaquePointer,
        private val parent: COpaquePointer? = null
    ) : AutoCloseable {
        fun appendString(value: String) = appendType('s'.code) { value.cstr.ptr }
        fun readString(): String? = readType<CPointerVar<ByteVar>, String>('s') { it.value?.toKStringFromUtf8() }
        fun readUInt(): UInt? = readType<UIntVar, UInt>('u') { it.value }

        /**
         * @author Cedric Hammes
         * @since  01/05/2026
         *
         * @see [dbus_message_iter_get_arg_type, D-Bus: DBusMessage](https://dbus.freedesktop.org/doc/api/html/group__DBusMessage.html#ga5aae3c882a75aed953d8b3d489e9b271)
         */
        fun currentArgType(): Char = library.findFunctionOrThrow<GetArgType>("dbus_message_iter_get_arg_type").invoke(handle).toChar()

        /**
         * @author Cedric Hammes
         * @since  01/05/2026
         *
         * @see [dbus_message_iter_next, D-Bus: DBusMessage](https://dbus.freedesktop.org/doc/api/html/group__DBusMessage.html#ga554e9fafd4dcc84cebe9da9344846a82)
         */
        fun next(): Boolean = library.findFunctionOrThrow<Next>("dbus_message_iter_next").invoke(handle)

        /**
         * @return the new iterator or null if the current value is not an iterator
         *
         * @author Cedric Hammes
         * @since  01/05/2026
         *
         * @see [dbus_message_iter_recurse, D-Bus: DBusMessage](https://dbus.freedesktop.org/doc/api/html/group__DBusMessage.html#ga7652e1208743da5dd4ecc5aef07bf207)
         */
        fun recurse(): Iterator? {
            if (currentArgType() != 'v')
                return null

            val iterator = memScope.allocArray<ByteVar>(64)
            library.findFunctionOrThrow<Recurse>("dbus_message_iter_recurse").invoke(handle, iterator)
            next()

            return Iterator(memScope, library, iterator.reinterpret(), handle)
        }

        override fun close() {
            if (memScope is NativeFreeablePlacement)
                memScope.free(handle)
        }

        /**
         * @author Cedric Hammes
         * @since  01/05/2026
         *
         * @see [dbus_message_iter_append_basic, D-Bus: DBusMessage](https://dbus.freedesktop.org/doc/api/html/group__DBusMessage.html#ga17491f3b75b3203f6fc47dcc2e3b221b)
         */
        private inline fun <reified T : CPointed> appendType(code: Int, closure: MemScope.() -> CPointer<T>): Boolean = memScoped {
            val ptr = allocPointerTo<T>()
            ptr.value = closure()
            library.findFunctionOrThrow<AppendBasic>("dbus_message_iter_append_basic").invoke(handle, code, ptr.reinterpret())
        }

        /**
         * @author Cedric Hammes
         * @since  01/05/2026
         *
         * @see [dbus_message_iter_get_basic, D-Bus: DBusMessage](https://dbus.freedesktop.org/doc/api/html/group__DBusMessage.html#ga41c23a05e552d0574d0444d4693d18ab)
         */
        private inline fun <reified T : CVariable, O> readType(code: Char, closure: (T) -> O?): O? {
            if (currentArgType() != code) {
                return null
            }

            return memScoped {
                val value = alloc<T>()
                library.findFunctionOrThrow<GetBasic>("dbus_message_iter_get_basic").invoke(handle, value.ptr)
                next()
                closure(value)
            }
        }

        companion object {
            typealias Init = (COpaquePointer, COpaquePointer) -> Boolean
            typealias InitAppend = (COpaquePointer, COpaquePointer) -> Unit
            typealias AppendBasic = (COpaquePointer, Int, CPointerVarOf<COpaquePointer>) -> Boolean
            typealias Next = (COpaquePointer) -> Boolean
            typealias Recurse = (COpaquePointer, COpaquePointer) -> Unit
            typealias GetBasic = (COpaquePointer, COpaquePointer) -> Unit
            typealias GetArgType = (COpaquePointer) -> Int

            internal fun readFromMessage(memScope: NativePlacement, library: DBusSharedLibrary, message: COpaquePointer): Iterator? {
                val iterator = memScope.allocArray<LongVar>(8)
                if (!library.findFunctionOrThrow<Init>("dbus_message_iter_init").invoke(message, iterator)) {
                    return null
                }

                return Iterator(memScope, library, iterator)
            }

            internal fun appendToMessage(memScope: NativePlacement, library: DBusSharedLibrary, message: COpaquePointer): Iterator {
                val iterator = memScope.allocArray<LongVar>(8)
                library.findFunctionOrThrow<InitAppend>("dbus_message_iter_init_append").invoke(message, iterator)
                return Iterator(memScope, library, iterator)
            }
        }
    }
}
