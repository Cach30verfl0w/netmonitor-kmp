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
import dev.karmakrafts.cominterop.win32.IDispatch
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

/**
 * [INetworkListManager on MSDN](https://learn.microsoft.com/en-us/windows/win32/api/netlistmgr/nn-netlistmgr-inetworklistmanager)
 *
 * @author Alexander Hinze
 * @since 01/05/2026
 */
internal class INetworkListManager : ComInterface<INetworkListManager.Companion>(Companion) {
    private typealias _GetNetworks = (self: COpaquePointer, flags: Int, ppEnumNetwork: CPointer<COpaquePointerVar>) -> HRESULT

    companion object : ComInterfaceType {
        override val superInterfaces: Array<ComInterfaceType> = arrayOf(IDispatch)

        override val functions: List<String> = VTableFunctionList.build {
            addStubs(7)
            add("GetNetworks")
            addStubs(1)
        }

        override fun create(): ComInterface<*> = INetworkListManager()

        override fun getIID(iid: CPointer<IID>, iface: ComInterface<*>) {
            ComRuntime.iidFromString("{DCB00001-570F-4A9B-8D69-199FDBA5723B}", iid)
        }
    }

    private val GetNetworks: CPointer<CFunction<_GetNetworks>> by vTable

    fun getNetworks(flags: Int): IEnumNetworks = memScoped {
        val enumAddress = alloc<COpaquePointerVar>()
        check(GetNetworks(address, flags, enumAddress.ptr) == S_OK) { "Could not retrieve list of networks" }
        checkNotNull(enumAddress.value) { "Could not retrieve network enumerator address" }.asCom(IEnumNetworks)
    }
}