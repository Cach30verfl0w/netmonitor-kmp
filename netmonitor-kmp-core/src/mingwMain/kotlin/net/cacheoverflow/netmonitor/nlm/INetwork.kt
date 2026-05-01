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
import dev.karmakrafts.cominterop.vtable.VTableFunctionList
import dev.karmakrafts.cominterop.win32.IDispatch
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.posix.IID
import platform.windows.HRESULT
import platform.windows.S_OK

/**
 * [INetwork on MSDN](https://learn.microsoft.com/en-us/windows/win32/api/netlistmgr/nn-netlistmgr-inetwork)
 *
 * @author Alexander Hinze
 * @since 01/05/2026
 */
internal class INetwork : ComInterface<INetwork.Companion>(Companion) {
    private typealias _GetCategory = (self: COpaquePointer, category: CPointer<IntVar>) -> HRESULT
    private typealias _GetConnectivity = (self: COpaquePointer, connectivity: CPointer<IntVar>) -> HRESULT

    companion object : ComInterfaceType {
        override val superInterfaces: Array<ComInterfaceType> = arrayOf(IDispatch)

        override val functions: List<String> = VTableFunctionList.build {
            addStubs(10)
            add("GetConnectivity")
            add("GetCategory")
            addStubs(1)
        }

        override fun create(): ComInterface<*> = INetwork()

        override fun getIID(iid: CPointer<IID>, iface: ComInterface<*>) {
            ComRuntime.iidFromString("{DCB00002-570F-4A9B-8D69-199FDBA5723B}", iid)
        }
    }

    private val GetConnectivity: CPointer<CFunction<_GetConnectivity>> by vTable
    private val GetCategory: CPointer<CFunction<_GetCategory>> by vTable

    val category: Int
        get() = memScoped {
            val value = alloc<IntVar>()
            check(GetCategory(address, value.ptr) == S_OK) { "Could not get network category" }
            value.value
        }

    val connectivity: Int
        get() = memScoped {
            val value = alloc<IntVar>()
            check(GetConnectivity(address, value.ptr) == S_OK) { "Could not get network connectivity" }
            value.value
        }
}