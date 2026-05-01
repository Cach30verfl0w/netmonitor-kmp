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

package net.cacheoverflow.netmonitor

import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.reinterpret
import platform.posix.RTLD_LAZY
import platform.posix.RTLD_NOW
import platform.posix.dlclose
import platform.posix.dlopen
import platform.posix.dlsym

/**
 * @author Cedric Hammes
 * @since  28/04/2026
 */
@InternalNetMonitorAPI
@OptIn(ExperimentalForeignApi::class)
open class SharedLibrary(internal val handle: CValuesRef<*>) : AutoCloseable {
    fun findFunctionOrNull(name: String): COpaquePointer? = dlsym(handle, name)

    inline fun <reified T : Function<*>> findFunctionOrNull(name: String): CPointer<CFunction<T>>? =
        findFunctionOrNull(name)?.reinterpret()

    inline fun <reified T : Function<*>> findFunctionOrThrow(name: String): CPointer<CFunction<T>> =
        requireNotNull(findFunctionOrNull<T>(name)) { "Could not find function '$name'" }

    override fun close() {
        dlclose(handle)
    }

    companion object {
        fun open(names: Array<String>, mode: Mode = Mode.LAZY): Result<SharedLibrary> {
            if (names.isEmpty()) {
                return Result.failure(IllegalArgumentException("No names for the library specified"))
            }

            var libraryHandle: COpaquePointer? = null
            for (name in names) {
                libraryHandle = dlopen(name, mode.inner)
                if (libraryHandle != null) break
            }

            if (libraryHandle == null) {
                return Result.failure(RuntimeException("Failed to load library '${names.first()}'"))
            }

            return Result.success(SharedLibrary(libraryHandle))
        }
    }

    @InternalNetMonitorAPI
    enum class Mode(internal val inner: Int) {
        IMMEDIATE(RTLD_NOW), LAZY(RTLD_LAZY)
    }
}