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
import org.jetbrains.skia.Image as SkiaImage
import java.io.File

@Composable
fun ScanDetailScreen(scanId: Int, onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var resultImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var currentSlice by remember { mutableStateOf(75) } // Default to middle-ish
    val totalSlices = 155 // BraTS standard
    
    var tumorVolume by remember { mutableStateOf("...") }
    var conclusion by remember { mutableStateOf("Завантаження висновку...") }
    var tumorNature by remember { mutableStateOf("...") }

    // Fetch scan details on start
    LaunchedEffect(scanId) {
        coroutineScope.launch(Dispatchers.IO) {
            val json = ApiClient.getScanDetails(scanId)
            if (!json.contains("\"status\": \"error\"")) {
                val volMatch = """"tumor_volume_cm3"\s*:\s*([^,}]+)""".toRegex().find(json)
                val concMatch = """"conclusion"\s*:\s*"([^"]+)"""".toRegex().find(json)
                val natureMatch = """"tumor_nature"\s*:\s*"([^"]+)"""".toRegex().find(json)
                
                withContext(Dispatchers.Main) {
                    tumorVolume = volMatch?.groupValues?.get(1)?.let { if(it == "null") "N/A" else "%.2f".format(it.toDouble()) } ?: "N/A"
                    conclusion = concMatch?.groupValues?.get(1) ?: "Немає висновку"
                    tumorNature = natureMatch?.groupValues?.get(1) ?: "Невідомо"
                }
            } else {
                withContext(Dispatchers.Main) {
                    conclusion = "Не вдалося завантажити деталі сканування"
                }
            }
        }
    }
    
    // Fetch slice image when currentSlice or scanId changes
    LaunchedEffect(scanId, currentSlice) {
        isLoading = true
        coroutineScope.launch(Dispatchers.IO) {
            val bytes = ApiClient.getScanSlice(scanId, currentSlice)
            if (bytes != null) {
                try {
                    val skiaImage = SkiaImage.makeFromEncoded(bytes)
                    val bitmap = skiaImage.toComposeImageBitmap()
                    withContext(Dispatchers.Main) {
                        resultImage = bitmap
                        isLoading = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) { isLoading = false }
                }
            } else {
                withContext(Dispatchers.Main) {
                    resultImage = null
                    isLoading = false
                }
            }
        }
    }

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
                        if (isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        } else if (resultImage != null) {
                            Image(
                                bitmap = resultImage!!,
                                contentDescription = "MRI Slice $currentSlice",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text("No image available for this slice", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
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
                    Text("Model: U-Net 3D\nStatus: Completed\nNature: $tumorNature", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val bytes = ApiClient.getScanReport(scanId)
                            if (bytes != null) {
                                try {
                                    val file = File("report_scan_$scanId.pdf")
                                    file.writeBytes(bytes)
                                    // Open file with default system viewer
                                    if (java.awt.Desktop.isDesktopSupported()) {
                                        java.awt.Desktop.getDesktop().open(file)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
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
