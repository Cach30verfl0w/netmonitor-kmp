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

package dev.cacheoverflow.netmonitor.foreign

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.util.Optional

internal class PosixNativeLibrary(internal: NativeLibrary, private val path: String) : NativeLibrary, SymbolLookup {
    private val dlopen: MethodHandle = internal.find("dlopen", DL_OPEN)
    private val dlclose: MethodHandle = internal.find("dlclose", DL_CLOSE)
    private val dlsym: MethodHandle = internal.find("dlsym", DL_SYM)
    private val library: MemorySegment = Arena.ofConfined().use {
        val pathPtr = it.allocateFrom(path)
        dlopen.invoke(pathPtr, 0x0) as MemorySegment // TODO: Other mode value?
    }

    override val linker: Linker = internal.linker
    override val handle: SymbolLookup = this

    override fun find(name: String?): Optional<MemorySegment> =
        when (val address = Arena.ofConfined().use { dlsym.invoke(library, it.allocateFrom(name)) as MemorySegment }) {
            MemorySegment.NULL -> return Optional.empty()
            else -> return Optional.of(address)
        }

    override fun close() = dlclose.invokeExact(library) as Unit

    companion object {
        private val DL_OPEN = FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        private val DL_SYM = FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        private val DL_CLOSE = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)

        // TODO: This should not be used when on Linux
        fun open(path: String, arena: Arena, linker: Linker = Linker.nativeLinker()): Result<PosixNativeLibrary> =
            NativeLibrary.load(arena, "libSystem.dylib", linker).map { defaultLibrary -> PosixNativeLibrary(defaultLibrary, path) }
    }
}
