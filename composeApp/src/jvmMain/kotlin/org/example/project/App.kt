package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.ui.theme.AppTheme
import org.example.project.ui.screens.PatientListScreen
import org.example.project.ui.screens.SegmentationScreen
import org.example.project.ui.screens.ScanDetailScreen

enum class AppScreen {
    PatientList,
    Segmentation,
    ScanDetail
}

@Composable
fun App() {
    AppTheme {
        var currentScreen by remember { mutableStateOf(AppScreen.PatientList) }
        var selectedPatientId by remember { mutableStateOf<Int?>(null) }
        var selectedScanId by remember { mutableStateOf<Int?>(null) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Navigation Bar
            TopNavBar(
                currentScreen = currentScreen,
                onNavigate = { currentScreen = it }
            )

            // Main Content
            Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                when (currentScreen) {
                    AppScreen.PatientList -> PatientListScreen(
                        onPatientSelected = { selectedPatientId = it.id },
                        onScanClicked = { scanId ->
                            selectedScanId = scanId
                            currentScreen = AppScreen.ScanDetail
                        },
                        onNewAnalysis = { currentScreen = AppScreen.Segmentation }
                    )
                    AppScreen.Segmentation -> SegmentationScreen(
                        onComplete = { scanId ->
                            selectedScanId = scanId
                            currentScreen = AppScreen.ScanDetail
                        }
                    )
                    AppScreen.ScanDetail -> ScanDetailScreen(
                        scanId = selectedScanId ?: 0,
                        onBack = { currentScreen = AppScreen.PatientList }
                    )
                }
            }
        }
    }
}

@Composable
fun TopNavBar(currentScreen: AppScreen, onNavigate: (AppScreen) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "NeuroSegment AI",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 24.dp)
            )

            NavTab("Patient Archive", currentScreen == AppScreen.PatientList || currentScreen == AppScreen.ScanDetail) { onNavigate(AppScreen.PatientList) }
            NavTab("Segmentation", currentScreen == AppScreen.Segmentation) { onNavigate(AppScreen.Segmentation) }

            Spacer(modifier = Modifier.weight(1f))

            if (currentScreen == AppScreen.PatientList) {
                Button(onClick = { onNavigate(AppScreen.Segmentation) }) {
                    Text("+ New Analysis")
                }
            }
        }
    }
}

@Composable
fun NavTab(title: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val textColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = textColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}