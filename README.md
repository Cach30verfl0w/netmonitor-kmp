

<div align="center">  

# `🚀 netmonitor-kmp`
**A library designed to monitor network connectivity states across different platforms**  

![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-purple.svg?style=flat&logo=kotlin)
![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)
![CI Status](https://github.com/cach30verfl0w/netmonitor-kmp/actions/workflows/publish.yml/badge.svg)

</div>

  
## 🛠 Example Usage  
  
To start monitoring the network state, obtain an instance of NetworkMonitor and register a NetworkStateCallback:  
  
```kotlin  
val networkMonitor: NetworkMonitor = NetworkMonitor()  
networkMonitor.registerCallback { state ->
    println("New network state: $state")
}
```  
  
## 📦 Installation  
  
Add the dependency to your `commonMain` source set in your `build.gradle.kts`:  
  
```kotlin  
kotlin {  
    sourceSets { 
        commonMain.dependencies { 
            implementation("net.cacheoverflow.netmonitor:netmonitor-kmp-core:1.0.0")
        }
    }
}
```

## 📄 License

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
