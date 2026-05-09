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
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.data.ApiClient
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.net.URL
import javax.imageio.ImageIO

@Composable
fun ScanDetailScreen(scanId: Int, onBack: () -> Unit) {
    var resultImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var tumorVolume by remember { mutableStateOf<String?>(null) }
    var conclusion by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var currentSlice by remember { mutableStateOf(0) }
    var totalSlices by remember { mutableStateOf(1) }
    val coroutineScope = rememberCoroutineScope()

    // Load initial preview + metadata
    LaunchedEffect(scanId) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // First, get total slices from backend
                val infoJson = ApiClient.getScanSliceInfo(scanId)
                val totalMatch = "\"total_slices\":\\s*(\\d+)".toRegex().find(infoJson)
                val centerMatch = "\"center_slice\":\\s*(\\d+)".toRegex().find(infoJson)
                val total = totalMatch?.groupValues?.get(1)?.toInt() ?: 1
                val center = centerMatch?.groupValues?.get(1)?.toInt() ?: (total / 2)

                withContext(Dispatchers.Main) {
                    totalSlices = total
                    currentSlice = center
                }

                // Load preview at center slice
                loadSliceImage(scanId, center) { img ->
                    resultImage = img
                }

                // Load scan metadata for conclusion
                val scanJson = ApiClient.getScanStatus(scanId)
                val volMatch = "\"tumor_volume_cm3\":\\s*([0-9.]+)".toRegex().find(scanJson)
                val conclusionMatch = "\"conclusion\":\\s*\"([^\"]+)\"".toRegex().find(scanJson)
                val vol = volMatch?.groupValues?.get(1)
                val concl = conclusionMatch?.groupValues?.get(1)

                withContext(Dispatchers.Main) {
                    tumorVolume = vol
                    conclusion = concl ?: buildFallbackConclusion(vol)
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    conclusion = "Error loading scan data: ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    // Reload image when slice changes
    LaunchedEffect(currentSlice) {
        if (!isLoading) {
            coroutineScope.launch(Dispatchers.IO) {
                loadSliceImage(scanId, currentSlice) { img ->
                    resultImage = img
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(bottom = 4.dp)) {
            Text("← Back to Patient Archive")
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading scan data...")
                }
            }
        } else {
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
                                    bitmap = resultImage!!,
                                    contentDescription = "MRI Slice $currentSlice",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                CircularProgressIndicator(color = Color.White)
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
                                    valueRange = 0f..(totalSlices - 1).coerceAtLeast(1).toFloat(),
                                    steps = (totalSlices - 2).coerceAtLeast(0),
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
                            MetricLine("Tumor Volume", "${tumorVolume ?: "-"} cm³", Color(0xFFEF4444))
                        }
                    }

                    // Conclusion
                    CompactCard("Conclusion", modifier = Modifier.weight(1f)) {
                        Text(
                            text = conclusion ?: "No data available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Export Report
                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val pdfBytes = ApiClient.exportReport(scanId)
                                    val desktop = File(System.getProperty("user.home"), "Desktop")
                                    val reportsDir = File(desktop, "NeuroSegment_Reports")
                                    if (!reportsDir.exists()) {
                                        reportsDir.mkdirs()
                                    }
                                    val file = File(reportsDir, "scan_${scanId}_report.pdf")
                                    file.writeBytes(pdfBytes)
                                    Desktop.getDesktop().open(file)
                                } catch (e: Exception) {
                                    // Show error via conclusion field
                                    withContext(Dispatchers.Main) {
                                        conclusion = "Export failed: ${e.message}"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Export Report (PDF)")
                    }
                }
            }
        }
    }
}

// ─── Helper: load a specific slice image from backend ───
private suspend fun loadSliceImage(scanId: Int, slice: Int, onLoaded: suspend (ImageBitmap) -> Unit) {
    try {
        val url = URI("http://localhost:8000/scans/$scanId/preview?slice_idx=$slice").toURL()
        val image = ImageIO.read(url)
        if (image != null) {
            withContext(Dispatchers.Main) {
                onLoaded(image.toComposeImageBitmap())
            }
        }
    } catch (_: Exception) {}
}

private fun buildFallbackConclusion(volumeStr: String?): String {
    val vol = volumeStr?.toDoubleOrNull()
    return if (vol == null) {
        "Analysis completed. Volumetric data is not available for this scan."
    } else if (vol < 0.1) {
        "Conclusion: No significant lesion detected above detection threshold.\n\nRecommendation: Routine follow-up if symptoms persist."
    } else if (vol < 5.0) {
        "Conclusion: Detected a small lesion with volume ${String.format("%.2f", vol)} cm³.\n\nRecommendation: Follow-up MRI in 3-6 months to monitor growth."
    } else if (vol < 25.0) {
        "Conclusion: Detected a moderate-sized lesion with volume ${String.format("%.2f", vol)} cm³.\n\nRecommendation: Consultation with a neurosurgeon."
    } else {
        "Conclusion: Detected a large lesion with volume ${String.format("%.2f", vol)} cm³.\n\nRecommendation: Urgent consultation with a neurosurgeon."
    }
}

// ─── Reusable compact card ───
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
