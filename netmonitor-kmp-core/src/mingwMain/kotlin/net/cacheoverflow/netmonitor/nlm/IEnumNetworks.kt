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

package net.cacheoverflow.netmonitor.nlm

import dev.karmakrafts.cominterop.ComInterface
import dev.karmakrafts.cominterop.ComInterfaceType
import dev.karmakrafts.cominterop.ComRuntime
import dev.karmakrafts.cominterop.asCom
import dev.karmakrafts.cominterop.vtable.VTableFunctionList
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.posix.IID
import platform.windows.HRESULT
import platform.windows.S_OK
import platform.windows.ULONG
import platform.windows.ULONGVar

/**
 * [IEnumNetworks on MSDN](https://learn.microsoft.com/en-us/windows/win32/api/netlistmgr/nn-netlistmgr-ienumnetworks)
 *
 * @author Alexander Hinze
 * @since 01/05/2026
 */
internal class IEnumNetworks : ComInterface<IEnumNetworks.Companion>(Companion) {
    private typealias _Next = (self: COpaquePointer, celt: ULONG, rgelt: CPointer<COpaquePointerVar>, pceltFetched: CPointer<ULONGVar>) -> HRESULT

    companion object : ComInterfaceType {
        override val functions: List<String> = VTableFunctionList.build {
            addStubs(2)
            add("Next")
            addStubs(2)
        }

        override fun create(): ComInterface<*> = IEnumNetworks()

        override fun getIID(iid: CPointer<IID>, iface: ComInterface<*>) {
            ComRuntime.iidFromString("{DCB00001-570F-4A9B-8D69-199FDBA5723B}", iid)
        }
    }

    private val Next: CPointer<CFunction<_Next>> by vTable

    fun next(): INetwork? = memScoped {
        val result = alloc<COpaquePointerVar>()
        val count = alloc<ULONGVar>()
        if (Next(address, 1U, result.ptr, count.ptr) != S_OK || count.value == 0U) return@memScoped null
        result.value?.asCom(INetwork)
    }

    fun toList(): List<INetwork> {
        var network: INetwork? = next() ?: return emptyList()
        val networks = ArrayList<INetwork>()
        while (network != null) {
            networks += network
            network = next()
        }
        return networks
    }
}