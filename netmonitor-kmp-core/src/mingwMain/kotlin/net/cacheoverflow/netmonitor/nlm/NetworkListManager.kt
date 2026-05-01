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

import dev.karmakrafts.cominterop.ComClass
import dev.karmakrafts.cominterop.ComRuntime
import kotlinx.cinterop.CPointer
import net.cacheoverflow.netmonitor.nlm.INetworkListManager.Companion
import platform.posix.CLSID

/**
 * [INetworkListManager on MSDN](https://learn.microsoft.com/en-us/windows/win32/api/netlistmgr/nn-netlistmgr-inetworklistmanager)
 *
 * @author Alexander Hinze
 * @since 01/05/2026
 */
internal object NetworkListManager : ComClass<Companion> {
    override fun getCLSID(clsid: CPointer<CLSID>) {
        ComRuntime.iidFromString("{DCB00000-570F-4A9B-8D69-199FDBA5723B}", clsid)
    }

    override val defaultInterface: Companion = INetworkListManager
}