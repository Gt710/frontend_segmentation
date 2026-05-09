package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import org.example.project.data.Patient
import org.example.project.data.Scan
import java.net.URL
import javax.imageio.ImageIO

@Composable
fun PatientListScreen(
    onPatientSelected: (Patient) -> Unit,
    onScanClicked: (Int) -> Unit,
    onNewAnalysis: () -> Unit
) {
    var patients by remember { mutableStateOf<List<Patient>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<Patient?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    fun fetchPatients() {
        isLoading = true
        patients = emptyList() // Clear current list (cache) before fetching new data
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val fetchedPatients = ApiClient.getPatients().sortedByDescending { it.id }
                withContext(Dispatchers.Main) {
                    patients = fetchedPatients
                    if (fetchedPatients.isNotEmpty()) {
                        selectedPatient = fetchedPatients.first()
                        onPatientSelected(fetchedPatients.first())
                    }
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchPatients()
    }

    val filteredPatients = patients.filter {
        if (searchQuery.isBlank()) true
        else {
            it.first_name.contains(searchQuery, ignoreCase = true) ||
            it.last_name.contains(searchQuery, ignoreCase = true) ||
            (it.notes?.contains(searchQuery, ignoreCase = true) ?: false)
        }
    }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        // Left Column: Patient List with Search
        Column(modifier = Modifier.width(280.dp).fillMaxHeight()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Patients",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = { fetchPatients() }) {
                    Text("Refresh", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name...") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                singleLine = true
            )

            if (isLoading) {
                CircularProgressIndicator()
            } else if (filteredPatients.isEmpty()) {
                Text("No patients found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    lazyItems(filteredPatients) { patient ->
                        val isSelected = selectedPatient?.id == patient.id
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedPatient = patient
                                onPatientSelected(patient)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "${patient.last_name}, ${patient.first_name}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "DOB: ${patient.dob.take(10)} • Scans: ${patient.scans.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right Column: Patient Details
        if (selectedPatient != null) {
            val patient = selectedPatient!!
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Patient Summary Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "${patient.last_name.uppercase()}, ${patient.first_name}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ID: MRN-${patient.id} • DOB: ${patient.dob.take(10)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (patient.notes != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = patient.notes,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        ApiClient.deletePatient(patient.id)
                                        val refreshed = ApiClient.getPatients()
                                        withContext(Dispatchers.Main) {
                                            patients = refreshed
                                            selectedPatient = refreshed.firstOrNull()
                                        }
                                    } catch (_: Exception) {}
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text("Delete Patient", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard("Total Scans", patient.scans.size.toString(), Modifier.weight(1f))
                    val firstScan = patient.scans.minByOrNull { it.upload_date }?.upload_date?.take(10) ?: "N/A"
                    val latestScan = patient.scans.maxByOrNull { it.upload_date }?.upload_date?.take(10) ?: "N/A"
                    StatCard("First Scan", firstScan, Modifier.weight(1f))
                    StatCard("Latest Scan", latestScan, Modifier.weight(1f))
                }

                // Segmentation Archive Title
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Segmentation Archive",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(bottom = 12.dp))

                if (patient.scans.isEmpty()) {
                    Text("No scans found. Click '+ New Analysis' to upload MRI data.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(patient.scans.sortedByDescending { it.id }) { scan ->
                            ScanCard(
                                scan = scan,
                                onClick = { onScanClicked(scan.id) },
                                onDelete = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            ApiClient.deleteScan(scan.id)
                                            // Refresh patient data
                                            val refreshed = ApiClient.getPatients().sortedByDescending { it.id }
                                            withContext(Dispatchers.Main) {
                                                patients = refreshed
                                                selectedPatient = refreshed.find { it.id == patient.id }
                                            }
                                        } catch (_: Exception) {}
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ScanCard(scan: Scan, onClick: () -> Unit, onDelete: () -> Unit) {
    var previewImage by remember(scan.id) { mutableStateOf<ImageBitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Load preview image for completed scans
    LaunchedEffect(scan.id, scan.status) {
        if (scan.status == "completed") {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val url = URL("http://localhost:8000/scans/${scan.id}/preview")
                    val image = ImageIO.read(url)
                    if (image != null) {
                        withContext(Dispatchers.Main) {
                            previewImage = image.toComposeImageBitmap()
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp).background(Color(0xFF1a1a2e))
            ) {
                if (previewImage != null) {
                    androidx.compose.foundation.Image(
                        bitmap = previewImage!!,
                        contentDescription = "Scan preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                // Status badge
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                ) {
                    Text(
                        scan.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (scan.status) {
                            "completed" -> Color(0xFF22C55E)
                            "failed" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = scan.upload_date.replace("T", " ").take(16),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (scan.tumor_volume_cm3 != null) {
                    Text(
                        "Tumor: ${String.format("%.2f", scan.tumor_volume_cm3)} cm³",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Delete button
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.padding(top = 4.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
