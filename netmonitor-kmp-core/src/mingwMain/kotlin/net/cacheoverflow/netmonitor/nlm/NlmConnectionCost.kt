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
 * [NLM_CONNECTION_COST on MSDN](https://learn.microsoft.com/en-us/windows/win32/api/netlistmgr/ne-netlistmgr-nlm_connection_cost)
 *
 * @author Alexander Hinze
 * @since 01/05/2026
 */
internal object NlmConnectionCost {
    // @formatter:off
    const val UNKNOWN: Int              = 0x00000
    const val UNRESTRICTED: Int         = 0x00001
    const val FIXED: Int                = 0x00002
    const val VARIABLE: Int             = 0x00004
    const val OVERDATALIMIT: Int        = 0x10000
    const val CONGESTED: Int            = 0x20000
    const val ROAMING: Int              = 0x40000
    const val APPROACHINGDATALIMIT: Int = 0x80000
    // @formatter:on
}