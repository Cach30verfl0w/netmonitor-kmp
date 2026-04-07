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

package net.cacheoverflow.netmonitor.foreign

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.invoke.MethodHandle
import java.nio.file.Path

/**
 * @author Cedric Hammes
 * @since  03/04/2026
 */
internal interface NativeLibrary : AutoCloseable {
    val linker: Linker
    val handle: SymbolLookup

    fun find(name: String, descriptor: FunctionDescriptor): MethodHandle =
        handle.find(name).map { linker.downcallHandle(it, descriptor) }.orElseThrow()

    fun findSymbol(name: String): MemorySegment = handle.find(name).orElseThrow()

    companion object {

        /**
         * @param arena  the memory section for the structs allocated in memory
         * @param names  the name and its fallbacks of the library that is being looked up
         *
         * @author Cedric Hammes
         * @since  03/04/2026
         */
        @JvmStatic
        fun load(arena: Arena, names: Array<String>, linker: Linker = Linker.nativeLinker()): Result<NativeLibrary> {
            for (i in names.indices) {
                val result = load(arena, names[i], linker)
                if (result.isFailure && i != (names.size - 1)) {
                    continue
                }

                return result
            }

            return Result.failure(RuntimeException("Names list is empty"))
        }

        /**
         * @param arena  the memory section for the library lookup
         * @param name   the name of the library that is being looked up
         *
         * @author Cedric Hammes
         * @since  03/04/2026
         */
        fun load(arena: Arena, name: String, linker: Linker = Linker.nativeLinker()): Result<NativeLibrary> =
            runCatching { PanamaNativeLibrary(linker, SymbolLookup.libraryLookup(name, arena)) }


        /**
         * @param arena  the memory section for the library lookup
         * @param path   the path to the library that is being looked up
         *
         * @author Cedric Hammes
         * @since  03/04/2026
         */
        fun load(arena: Arena, path: Path, linker: Linker = Linker.nativeLinker()): Result<NativeLibrary> =
            runCatching { PanamaNativeLibrary(linker, SymbolLookup.libraryLookup(path, arena)) }

        /**
         * @param arena  the memory section for the library lookup
         *
         * @author Cedric Hammes
         * @since  03/04/2026
         */
        fun load(arena: Arena, linker: Linker = Linker.nativeLinker()): Result<NativeLibrary> =
            runCatching { PanamaNativeLibrary(linker, SymbolLookup.loaderLookup()) }
    }
}

