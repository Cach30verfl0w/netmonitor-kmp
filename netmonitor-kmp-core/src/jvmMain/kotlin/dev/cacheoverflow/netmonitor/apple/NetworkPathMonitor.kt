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

package dev.cacheoverflow.netmonitor.apple

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.foreign.*

internal class NetworkPathMonitor(
    private val library: AppleNetworkLibrary,
    private val native: MemorySegment
) : AutoCloseable {
    private val setHandler: MethodHandle = library.find("nw_path_monitor_set_update_handler", SET_HANDLER)
    private val setQueue: MethodHandle = library.find("nw_path_monitor_set_queue", SET_QUEUE)
    private val start: MethodHandle = library.find("nw_path_monitor_start", START)
    private val cancel: MethodHandle = library.find("nw_path_monitor_cancel", CANCEL)
    private val release: MethodHandle = library.find("nw_release", RELEASE)

    private val arena: Arena = Arena.ofAuto()
    private val globalDescriptor: MemorySegment = arena.allocate(DESCRIPTOR_LAYOUT).apply {
        set(ValueLayout.JAVA_LONG, 0, 0)
        set(ValueLayout.JAVA_LONG, 8, 32)
    }

    fun start() {
        start.invokeExact(native)
    }

    fun setQueue(queue: AppleDispatchQueue) {
        setQueue.invokeExact(native, queue.native)
    }

    fun setHandler(handler: Handler) {
        val bridge = HandlerBridge(library, handler)
        val methodType = MethodType.methodType(Void.TYPE, MemorySegment::class.java, MemorySegment::class.java)
        val methodHandle = MethodHandles.lookup().findVirtual(HandlerBridge::class.java, "stub", methodType).bindTo(bridge)
        val stub = library.linker.upcallStub(methodHandle, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS), arena)

        // TODO: Create abstraction for this
        val block: MemorySegment = arena.allocate(BLOCK_LAYOUT)
        val globalBlock = library.findSymbol("_NSConcreteGlobalBlock")
        block.set(ValueLayout.ADDRESS, 0, globalBlock)
        block.set(ValueLayout.JAVA_INT, 8, 1 shl 28)
        block.set(ValueLayout.JAVA_INT, 12, 0)
        block.set(ValueLayout.ADDRESS, 16, stub)
        block.set(ValueLayout.ADDRESS, 24, globalDescriptor)
        setHandler.invokeExact(native, block)
    }

    override fun close() {
        arena.close()
        cancel.invokeExact(native)
        release.invokeExact(native)
    }

    fun interface Handler {
        fun invoke(path: NetworkPath)
    }

    @Suppress("Unused")
    class HandlerBridge(private val library: AppleNetworkLibrary, private val handler: Handler) {
        fun stub(block: MemorySegment, path: MemorySegment) = handler.invoke(NetworkPath(library, path))
    }

    companion object {
        private val SET_QUEUE = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        private val SET_HANDLER = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        private val START = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        private val CANCEL = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        private val RELEASE = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)

        private val BLOCK_LAYOUT: StructLayout = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("isa"),
            ValueLayout.JAVA_INT.withName("flags"),
            ValueLayout.JAVA_INT.withName("reserved"),
            ValueLayout.ADDRESS.withName("invoke"),
            ValueLayout.ADDRESS.withName("descriptor")
        )

        private val DESCRIPTOR_LAYOUT: StructLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("reserved"),
            ValueLayout.JAVA_LONG.withName("size")
        )
    }
}