package net.cacheoverflow.netmonitor.example

import androidx.compose.foundation.background
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
import net.cacheoverflow.netmonitor.NetworkType

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

            Spacer(modifier = Modifier.height(16.dp))
            when (state) {
                is NetworkState.Online -> {
                    OnlineDetails(state)
                }
                is NetworkState.Offline -> {
                    Text("Please check your cables or airplane mode.", style = MaterialTheme.typography.bodyMedium)
                }
                is NetworkState.CaptivePortal -> {
                    Text("Sign in to Network")
                }
                else -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
fun OnlineDetails(state: NetworkState.Online) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when (val type = state.type) {
            is NetworkType.Cellular -> {
                Text(
                    text = type.carrier?.ifBlank { "Unknown Carrier" } ?: "Unknown Carrier",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = type.generation.name,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            is NetworkType.WiFi -> Text("WiFi")
            is NetworkType.Ethernet -> Text("Wired Connection")
            else -> {
                Text(text = type.toString(), style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BadgeInfo(
                text = if (state.isMetered) "Metered" else "Unlimited",
                color = if (state.isMetered) Color(0xFFFF9800) else Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun BadgeInfo(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .padding(end = 4.dp)
                .background(color, shape = androidx.compose.foundation.shape.CircleShape)
        )
        Text(text = text, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
    }
}