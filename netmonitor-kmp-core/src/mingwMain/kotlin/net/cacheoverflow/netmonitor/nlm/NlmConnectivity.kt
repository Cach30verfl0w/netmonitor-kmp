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

/**
 * [NLM_CONNECTIVITY on MSDN](https://learn.microsoft.com/en-us/windows/win32/api/netlistmgr/ne-netlistmgr-nlm_connectivity)
 *
 * @author Alexander Hinze
 * @since 01/05/2026
 */
internal object NlmConnectivity {
    // @formatter:off
    const val DISCONNECTED: Int      = 0x0000
    const val IPV4_NOTRAFFIC: Int    = 0x0001
    const val IPV6_NOTRAFFIC: Int    = 0x0002
    const val IPV4_SUBNET: Int       = 0x0010
    const val IPV4_LOCALNETWORK: Int = 0x0020
    const val IPV4_INTERNET: Int     = 0x0040
    const val IPV6_SUBNET: Int       = 0x0100
    const val IPV6_LOCALNETWORK: Int = 0x0200
    const val IPV6_INTERNET: Int     = 0x0400
    // @formatter:on
}