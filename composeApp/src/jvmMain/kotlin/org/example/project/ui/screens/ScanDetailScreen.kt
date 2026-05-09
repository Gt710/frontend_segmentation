package org.example.project.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ScanDetailScreen(scanId: Int, onBack: () -> Unit) {
    val resultImage: ImageBitmap? = null // Mock: no image
    val tumorVolume = "15.42"
    val conclusion = "Conclusion: Detected a moderate-sized lesion with volume 15.42 cm³.\n\nRecommendation: Consultation with a neurosurgeon."
    val isLoading = false
    var currentSlice by remember { mutableStateOf(50) }
    val totalSlices = 100

    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(bottom = 4.dp)) {
            Text("← Back to Patient Archive")
        }

        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            // ─── Left: MRI Viewer with Slider ───
            Surface(
                modifier = Modifier.fillMaxHeight().weight(7f),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Axial View", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Text("Scan #$scanId • Slice ${currentSlice + 1} / $totalSlices", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Image
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (resultImage != null) {
                            Image(
                                bitmap = resultImage,
                                contentDescription = "MRI Slice $currentSlice",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text("MRI Slice Preview (Mock)", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    // Slice Slider
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Slice: ${currentSlice + 1}", style = MaterialTheme.typography.labelSmall)
                                Text("Total: $totalSlices", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Slider(
                                value = currentSlice.toFloat(),
                                onValueChange = { currentSlice = it.toInt() },
                                valueRange = 0f..(totalSlices - 1).toFloat(),
                                steps = totalSlices - 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // ─── Right: Analysis Panel ───
            Column(
                modifier = Modifier.fillMaxHeight().weight(3f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status
                CompactCard("Analysis Status") {
                    Text("Model: U-Net 3D\nStatus: Completed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Volumetric Metrics
                CompactCard("Volumetric Metrics") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        MetricLine("Tumor Volume", "$tumorVolume cm³", Color(0xFFEF4444))
                    }
                }

                // Conclusion
                CompactCard("Conclusion", modifier = Modifier.weight(1f)) {
                    Text(
                        text = conclusion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Export Report
                Button(
                    onClick = { /* Do nothing */ },
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("Export Report (PDF)")
                }
            }
        }
    }
}

@Composable
fun CompactCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(modifier = Modifier.padding(12.dp)) { content() }
        }
    }
}

@Composable
fun MetricLine(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.width(4.dp).height(20.dp).background(color, RoundedCornerShape(2.dp)))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}
