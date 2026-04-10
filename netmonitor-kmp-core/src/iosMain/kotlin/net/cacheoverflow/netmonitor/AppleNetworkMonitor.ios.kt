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

import platform.CoreTelephony.CTCarrier
import platform.CoreTelephony.CTRadioAccessTechnologyEdge
import platform.CoreTelephony.CTRadioAccessTechnologyGPRS
import platform.CoreTelephony.CTRadioAccessTechnologyHSDPA
import platform.CoreTelephony.CTRadioAccessTechnologyLTE
import platform.CoreTelephony.CTRadioAccessTechnologyNR
import platform.CoreTelephony.CTRadioAccessTechnologyNRNSA
import platform.CoreTelephony.CTRadioAccessTechnologyWCDMA
import platform.CoreTelephony.CTTelephonyNetworkInfo

import platform.CoreTelephony.*

/**
 * @author Cedric Hammes
 * @since  08/04/2026
 */
actual class CellularInformationFactory {
    private val telephonyInfo = CTTelephonyNetworkInfo()

    actual fun create(): NetworkType.Cellular {
        val dataServiceKey = telephonyInfo.dataServiceIdentifier
        val carrierName = if (dataServiceKey != null) {
            (telephonyInfo.serviceSubscriberCellularProviders?.get(dataServiceKey) as? CTCarrier)?.carrierName
        } else {
            telephonyInfo.serviceSubscriberCellularProviders?.values?.firstOrNull()
                ?.let { (it as? CTCarrier)?.carrierName }
        }

        val radioTech = if (dataServiceKey != null) {
            telephonyInfo.serviceCurrentRadioAccessTechnology?.get(dataServiceKey) as? String
        } else {
            telephonyInfo.serviceCurrentRadioAccessTechnology?.values?.firstOrNull() as? String
        }

        return NetworkType.Cellular(
            carrier = carrierName,
            generation = when (radioTech) {
                CTRadioAccessTechnologyNR, CTRadioAccessTechnologyNRNSA -> NetworkType.Cellular.Generation.G5
                CTRadioAccessTechnologyLTE -> NetworkType.Cellular.Generation.G4
                CTRadioAccessTechnologyWCDMA, CTRadioAccessTechnologyHSDPA, CTRadioAccessTechnologyHSUPA -> NetworkType.Cellular.Generation.G3
                CTRadioAccessTechnologyEdge, CTRadioAccessTechnologyGPRS -> NetworkType.Cellular.Generation.G2
                else -> NetworkType.Cellular.Generation.UNKNOWN
            }
        )
    }
}
