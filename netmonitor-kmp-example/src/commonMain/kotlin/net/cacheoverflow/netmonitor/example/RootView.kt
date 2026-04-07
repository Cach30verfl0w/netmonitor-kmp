package net.cacheoverflow.netmonitor.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.cacheoverflow.netmonitor.NetworkMonitor
import net.cacheoverflow.netmonitor.NetworkState

@Composable
fun RootView(networkMonitor: NetworkMonitor) {
    val networkState by networkMonitor.state.collectAsState(NetworkState.Unknown)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Network Status",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            NetworkStatusCard(state = networkState)
        }
    }
}

@Composable
fun NetworkStatusCard(state: NetworkState) {
    val (statusText, color, icon) = when (state) {
        is NetworkState.Online -> Triple(
            "Online (${state.type.name})",
            Color(0xFF4CAF50),
            if (state.isMetered) "⚠️ Metered Connection" else "✅ Unmetered"
        )
        is NetworkState.Offline -> Triple("Offline", Color(0xFFF44336), "❌ No Internet access")
        is NetworkState.CaptivePortal -> Triple("Action Required", Color(0xFFFF9800), "🌐 Please log in to the network")
        is NetworkState.Unknown -> Triple("Unknown", Color.Gray, "🔍 Determining state...")
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = statusText,
                color = color,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (state is NetworkState.Online) {
                Text(text = icon, style = MaterialTheme.typography.bodyMedium)
            } else if (state !is NetworkState.Unknown) {
                Text(text = icon, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
