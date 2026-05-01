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
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.CStructVar
import kotlinx.cinterop.NativePtr
import kotlinx.cinterop.alignOf
import kotlinx.cinterop.memberAt

/**
 * @author Cedric Hammes
 * @since  28/04/2026
 */
internal class DBusError(rawPointer: NativePtr) : CStructVar(rawPointer) {
    val name: CPointerVar<ByteVar> get() = memberAt(0L)
    val message: CPointerVar<ByteVar> get() = memberAt(8L)

    @Suppress("DEPRECATION")
    companion object : Type(
        size = (Int.SIZE_BYTES * 4 + Long.SIZE_BYTES * 3).toLong(),
        align = alignOf<COpaquePointerVar>()
    )
}
