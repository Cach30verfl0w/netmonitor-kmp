package net.cacheoverflow.netmonitor.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.cacheoverflow.netmonitor.NetworkMonitor
import net.cacheoverflow.netmonitor.NetworkState
import net.cacheoverflow.netmonitor.newCallbackFlow

@Composable
fun RootView(networkMonitor: NetworkMonitor) {
    val state = remember { networkMonitor.newCallbackFlow() }
    val networkState by state.collectAsState(NetworkState.Unknown)

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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val (title, color) = when (state) {
                is NetworkState.Online -> "Connected" to Color(0xFF4CAF50)
                is NetworkState.Offline -> "Disconnected" to Color(0xFFF44336)
                is NetworkState.CaptivePortal -> "Action Required" to Color(0xFFFF9800)
                is NetworkState.Unknown -> "Searching..." to Color.Gray
            }

            Text(
                text = title,
                color = color,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
