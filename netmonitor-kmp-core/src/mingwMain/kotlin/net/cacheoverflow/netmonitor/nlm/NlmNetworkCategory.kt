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
 * [NLM_NETWORK_CATEGORY on MSDN](https://learn.microsoft.com/en-us/windows/win32/api/netlistmgr/ne-netlistmgr-nlm_network_category)
 *
 * @author Alexander Hinze
 * @since 01/05/2026
 */
internal object NlmNetworkCategory {
    // @formatter:off
    const val PUBLIC: Int               = 0x0
    const val PRIVATE: Int              = 0x1
    const val DOMAIN_AUTHENTICATED: Int = 0x2
    // @formatter:on
}