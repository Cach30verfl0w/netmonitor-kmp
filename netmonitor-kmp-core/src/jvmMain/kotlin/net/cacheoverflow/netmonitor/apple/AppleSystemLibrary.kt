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

package net.cacheoverflow.netmonitor.apple

import net.cacheoverflow.netmonitor.foreign.NativeLibrary
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle

internal class AppleSystemLibrary(private val library: NativeLibrary) : NativeLibrary by library {
    private val dispatchQueueCreate: MethodHandle = find("dispatch_queue_create", DISPATCH_QUEUE_CREATE)

    fun createDispatchQueue(label: String): AppleDispatchQueue = Arena.ofConfined().use { arena ->
        val namePtr = arena.allocateFrom(label)
        AppleDispatchQueue(this, dispatchQueueCreate.invoke(namePtr, MemorySegment.NULL) as MemorySegment)
    }

    companion object {
        private val DISPATCH_QUEUE_CREATE = FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)

        @JvmStatic
        fun from(library: NativeLibrary): AppleSystemLibrary = AppleSystemLibrary(library)

        @JvmStatic
        fun load(arena: Arena = Arena.global(), linker: Linker = Linker.nativeLinker()): Result<AppleSystemLibrary> =
            NativeLibrary.load(arena, "libSystem.dylib", linker).map(AppleSystemLibrary::from)
    }
}
