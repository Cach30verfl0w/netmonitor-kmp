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
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.posix.IID
import platform.windows.DWORD
import platform.windows.DWORDVar
import platform.windows.HRESULT
import platform.windows.S_OK

/**
 * [INetworkCostManager on MSDN](https://learn.microsoft.com/en-us/windows/win32/api/netlistmgr/nn-netlistmgr-inetworkcostmanager)
 *
 * @author Alexander Hinze
 * @since 01/05/2026
 */
internal class INetworkCostManager : ComInterface<INetworkCostManager.Companion>(Companion) {
    private typealias _GetCost = (self: COpaquePointer, pCost: CPointer<DWORDVar>, pDestIpAddr: CPointer<ByteVar>?) -> HRESULT

    companion object : ComInterfaceType {
        override val functions: List<String> = VTableFunctionList.build {
            add("GetCost")
            addStubs(2)
        }

        override fun create(): ComInterface<*> = INetworkCostManager()

        override fun getIID(iid: CPointer<IID>, iface: ComInterface<*>) {
            ComRuntime.iidFromString("{DCB00008-570F-4A9B-8D69-199FDBA5723B}", iid)
        }
    }

    private val GetCost: CPointer<CFunction<_GetCost>> by vTable

    val cost: DWORD
        get() = memScoped {
            val cost = alloc<DWORDVar>()
            check(GetCost(address, cost.ptr, null) == S_OK) { "Could not retrieve connection costs" }
            cost.value
        }
}