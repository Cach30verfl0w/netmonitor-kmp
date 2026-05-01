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

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal class JvmNetworkMonitor(private val lookup: SymbolLookup) : NetworkMonitor {
    private val linker = Linker.nativeLinker()
    private val arena: Arena = Arena.ofAuto()
    private val handle: MemorySegment = call("netmonitor_network_monitor_create", CREATE)

    private val callbackHandles = mutableMapOf<NetworkMonitor.Callback, MemorySegment>()

    override fun registerCallback(callback: NetworkMonitor.Callback) {
        val lookup = MethodHandles.lookup()
        val bridge = object {
            @Suppress("UNUSED")
            fun onInvoke(packed: Int) {
                callback.networkStateChanged(NetworkState.fromPacked(packed))
            }
        }

        val finalHandle = lookup.findVirtual(bridge::class.java, "onInvoke", MethodType.methodType(Void.TYPE, Int::class.java))
            .bindTo(bridge)
        val upcallStub = linker.upcallStub(finalHandle, CALLBACK_DESCRIPTOR, arena)
        val nativeHandle: MemorySegment = call("netmonitor_network_monitor_register_callback", REGISTER, handle, upcallStub)
        callbackHandles[callback] = nativeHandle
    }

    override fun unregisterCallback(callback: NetworkMonitor.Callback) {
        val nativeHandle = callbackHandles.remove(callback) ?: return
        call("netmonitor_network_monitor_unregister_callback", UNREGISTER_CALLBACK, handle, nativeHandle) as Unit
    }

    override fun close(): Unit = call("netmonitor_network_monitor_dispose", DISPOSE, handle)

    @Suppress("UNCHECKED_CAST")
    private fun <T> call(name: String, descriptor: FunctionDescriptor, vararg args: Any): T =
        linker.downcallHandle(lookup.findOrThrow(name), descriptor).invokeWithArguments(*args) as T

    companion object {
        private val CALLBACK_DESCRIPTOR = FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT)

        private val CREATE = FunctionDescriptor.of(ValueLayout.ADDRESS)
        private val DISPOSE = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        private val REGISTER = FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        private val UNREGISTER_CALLBACK = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)

        fun tryInit(): JvmNetworkMonitor? {
            val target = NativeTarget.current() ?: return null
            val temporaryFolder = Files.createTempDirectory("de.cacheoverflow.netmonitor")
            val nativePath = temporaryFolder.resolve(target.binaryName)
            val resource = this::class.java.getResourceAsStream("/netmonitor-binaries/${target.binaryName}")
                ?: return null

            Files.copy(resource, nativePath, StandardCopyOption.REPLACE_EXISTING)
            return JvmNetworkMonitor(SymbolLookup.libraryLookup(nativePath, Arena.global()))
        }
    }

    private enum class NativeTarget(val os: String, val arch: String, val fileExtension: String) {
        WINDOWS_X64("windows", "x64", "dll"),
        LINUX_X64("linux", "x64", "so"),
        LINUX_ARM64("linux", "arm64", "so"),
        MACOS_ARM64("macos", "arm64", "dylib");

        val binaryName: String
            get() = "${os}_$arch.$fileExtension"

        companion object {
            fun current(): NativeTarget? {
                val osProp = System.getProperty("os.name").lowercase()
                val archProp = System.getProperty("os.arch").lowercase()

                val os = when {
                    osProp.contains("win") -> "windows"
                    osProp.contains("mac") -> "macos"
                    else -> "linux"
                }

                val arch = when {
                    archProp.contains("aarch64") || archProp.contains("arm64") -> "arm64"
                    else -> "x64"
                }

                return entries.find { it.os == os && it.arch == arch }
            }
        }
    }
}
