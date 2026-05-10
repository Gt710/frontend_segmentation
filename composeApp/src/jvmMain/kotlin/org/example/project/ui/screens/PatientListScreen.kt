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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.example.project.data.ApiClient
import org.example.project.data.Patient
import org.example.project.data.Scan

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
        patients = emptyList()
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
                e.printStackTrace()
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
                    "Пацієнти",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = { fetchPatients() }) {
                    Text("Оновити", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Пошук за ім'ям...") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                singleLine = true
            )


            if (isLoading) {
                CircularProgressIndicator()
            } else if (filteredPatients.isEmpty()) {
                Text("Пацієнтів не знайдено.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    "Дата нар.: ${patient.dob.take(10)} • Сканувань: ${patient.scans.size}",
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
                            text = "ID: MRN-${patient.id} • Дата нар.: ${patient.dob.take(10)} • Тел.: ${patient.phone ?: "Немає"}",
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
                            Text("Видалити пацієнта", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard("Всього сканувань", patient.scans.size.toString(), Modifier.weight(1f))
                    val firstScan = patient.scans.minByOrNull { it.upload_date }?.upload_date?.take(10) ?: "N/A"
                    val latestScan = patient.scans.maxByOrNull { it.upload_date }?.upload_date?.take(10) ?: "N/A"
                    StatCard("Перше сканування", firstScan, Modifier.weight(1f))
                    StatCard("Останнє сканування", latestScan, Modifier.weight(1f))

                }

                // Progression Block
                val sortedScans = patient.scans.sortedBy { it.upload_date }
                val progressionText = if (sortedScans.size >= 2) {
                    val latest = sortedScans.last()
                    val previous = sortedScans[sortedScans.size - 2]
                    val latestVol = latest.tumor_volume_cm3 ?: 0.0
                    val prevVol = previous.tumor_volume_cm3 ?: 0.0
                    val diff = latestVol - prevVol
                    val percent = if (prevVol > 0) (diff / prevVol) * 100 else 0.0
                    
                    val direction = if (diff > 0) "збільшився" else if (diff < 0) "зменшився" else "стабільний"
                    
                    val absDiff = if (diff < 0) -diff else diff
                    val absPercent = if (percent < 0) -percent else percent
                    
                    if (diff == 0.0) {
                        "Стан стабільний. Об'єм пухлини не змінився порівняно з попереднім обстеженням від ${previous.upload_date.take(10)}."
                    } else {
                        "Об'єм пухлини $direction на ${String.format("%.2f", absDiff)} cm³ (${String.format("%.1f", absPercent)}%) порівняно з попереднім обстеженням від ${previous.upload_date.take(10)}."
                    }
                } else {
                    "Це перше обстеження пацієнта. Динаміка буде доступна після наступних сканувань."
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = progressionText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
                    Text("Сканувань не знайдено. Натисніть '+ Новий аналіз', щоб завантажити дані МРТ.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    var previewImage by remember { mutableStateOf<ImageBitmap?>(null) }
    
    LaunchedEffect(scan.id) {
        withContext(Dispatchers.IO) {
            try {
                // Fetch middle slice (around 77 for BraTS)
                val bytes = ApiClient.getScanSlice(scan.id, 77)
                if (bytes != null) {
                    val bitmap = org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
                    withContext(Dispatchers.Main) {
                        previewImage = bitmap
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                        when (scan.status) {
                            "completed" -> "ГОТОВО"
                            "failed" -> "ПОМИЛКА"
                            else -> scan.status.uppercase()
                        },
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
                        "Пухлина: ${String.format("%.2f", scan.tumor_volume_cm3)} см³",
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
                    Text("Видалити", style = MaterialTheme.typography.labelSmall)
                }

            }
        }
    }
}
