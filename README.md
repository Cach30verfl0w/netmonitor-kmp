<div align="center">

# 🌐 Network Monitor KMP

**A lifecycle-aware, reactive network monitoring library for Kotlin Multiplatform.**

![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)
![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg?logo=android)
![iOS](https://img.shields.io/badge/Platform-iOS-lightgrey.svg?logo=apple)
![Desktop](https://img.shields.io/badge/Platform-Desktop-blue.svg?logo=kotlin)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

</div>

---

## 🚀 Key Features

* ⚡ **Reactive:** Powered by Kotlin Coroutines `Flow`.
* 🛠 **Detailed State:** Distinguishes between `Online`, `Offline`, `CaptivePortal`, and `Unknown`.
* 📊 **Rich Metadata:** Access network types (WiFi, Ethernet, Cellular, etc.) and metered connection detection.

| Capability          | Linux | Windows | macOS  | iOS    | Android   |
|---------------------|-------|---------|--------|--------|-----------|
| Bluetooth (Type)    | 🏗️   | 🏗️     | ❌️[^3] | ❌️[^3] | ✅         |
| Ethernet (Type)     | ✅     | 🏗️     | ✅      | ✅      | ✅         |
| Cellular (Type)     | ❌     | 🏗️     | ✅      | ✅      | ✅         |
| WIFI (Type)         | ✅     | ❌       | ✅      | ✅      | ✅         |
| Cellular Generation | ❌     | ❌       | ❌️     | ✅      | ✅[^1][^2] |
| Cellular Carrier    | ❌     | ❌       | ❌️     | ✅      | ✅[^1]     |

[^1]: Due to limitations on Android, this data requires a permission request. Further information in the documentation about `NetworkType.Cellular`.
[^2]: Information about the Bluetooth generation can be inaccurate
[^3]: Apple systems does not seem to allow the detection of Bluetooth-powered connections

## 🛠 Usage

The `NetworkMonitor` provides a reactive way to handle connectivity. Since it implements `AutoCloseable`, it fits perfectly into lifecycle-managed components like ViewModels.

### 1. Simple Flow Collection
If you just want to listen to changes in a simple coroutine scope:

```kotlin
val monitor = NetworkMonitor { applicationContext } // Or NetworkMonitor() for non-Android
val scope = CoroutineScope(Dispatchers.Main)

scope.launch {
    monitor.state.collect { state ->
        when (state) {
            is NetworkState.Online -> {
                val connectionType = state.type // e.g., WIFI, CELLULAR
                if (state.isMetered) {
                    println("Online on a metered connection ($connectionType)")
                } else {
                    println("Online on a free connection ($connectionType)")
                }
            }
            NetworkState.Offline -> println("You are currently offline")
            NetworkState.CaptivePortal -> println("Login to the WiFi network required")
            NetworkState.Unknown -> println("Connection status is being determined...")
        }
    }
}
```

## 📦 Installation

Add the dependency to your `commonMain` source set:

```gradle
sourceSets {
    commonMain.dependencies {
        implementation("net.cacheoverflow.netmonitor:netmonitor-kmp-core:1.0.0-SNAPSHOT") 
    }
}
