package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.data.ApiClient
import org.example.project.data.Patient

enum class PatientMode { NEW, EXISTING }

/**
 * Single-page: Patient registration (or selection) + File upload + Run Inference.
 * After inference completes, navigates to ScanDetail.
 */
@Composable
fun SegmentationScreen(onComplete: (Int) -> Unit) {
    var mode by remember { mutableStateOf(PatientMode.NEW) }
    var patientsList by remember { mutableStateOf<List<Patient>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<Patient?>(null) }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var t1File by remember { mutableStateOf<File?>(null) }
    var t1cFile by remember { mutableStateOf<File?>(null) }
    var t2File by remember { mutableStateOf<File?>(null) }
    var flairFile by remember { mutableStateOf<File?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val allFilesSelected = t1File != null && t1cFile != null && t2File != null && flairFile != null
    val formFilled = if (mode == PatientMode.NEW) {
        firstName.isNotBlank() && lastName.isNotBlank() && dob.isNotBlank()
    } else {
        selectedPatient != null
    }
    val canSubmit = allFilesSelected && formFilled && !isProcessing

    var dropdownExpanded by remember { mutableStateOf(false) }

    // Fetch patients for the "Existing Patient" mode
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                patientsList = ApiClient.getPatients().sortedByDescending { it.id }
            } catch (_: Exception) {}
        }
    }

    fun selectFile(onFileSelected: (File) -> Unit) {
        val dialog = FileDialog(Frame(), "Select NIfTI file", FileDialog.LOAD)
        dialog.isVisible = true
        if (dialog.directory != null && dialog.file != null) {
            onFileSelected(File(dialog.directory, dialog.file))
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── Patient Mode Selection & Info ───
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Patient Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { mode = PatientMode.NEW }) {
                            RadioButton(selected = mode == PatientMode.NEW, onClick = { mode = PatientMode.NEW })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Patient", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { mode = PatientMode.EXISTING }) {
                            RadioButton(selected = mode == PatientMode.EXISTING, onClick = { mode = PatientMode.EXISTING })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Existing Patient", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (mode == PatientMode.NEW) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = lastName, onValueChange = { lastName = it },
                                label = { Text("Last Name *") },
                                modifier = Modifier.weight(1f), singleLine = true
                            )
                            OutlinedTextField(
                                value = firstName, onValueChange = { firstName = it },
                                label = { Text("First Name *") },
                                modifier = Modifier.weight(1f), singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = dob, onValueChange = { dob = it },
                                label = { Text("Date of Birth * (YYYY-MM-DD)") },
                                modifier = Modifier.weight(1f), singleLine = true
                            )
                            OutlinedTextField(
                                value = phone, onValueChange = { phone = it },
                                label = { Text("Phone") },
                                modifier = Modifier.weight(1f), singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = notes, onValueChange = { notes = it },
                            label = { Text("Clinical Notes") },
                            modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3
                        )
                    } else {
                        // Existing Patient Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedPatient?.let { "${it.last_name}, ${it.first_name} (ID: ${it.id})" } ?: "Select Patient",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Patient *") },
                                modifier = Modifier.fillMaxWidth().clickable { dropdownExpanded = true },
                                trailingIcon = {
                                    IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                        Text(if (dropdownExpanded) "▲" else "▼")
                                    }
                                }
                            )
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                if (patientsList.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No patients found") },
                                        onClick = { dropdownExpanded = false }
                                    )
                                } else {
                                    patientsList.forEach { p ->
                                        DropdownMenuItem(
                                            text = { Text("${p.last_name}, ${p.first_name} (ID: ${p.id})") },
                                            onClick = {
                                                selectedPatient = p
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ─── MRI File Upload ───
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "MRI Modalities",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModalityZone("T1-weighted (.t1n)", t1File) { selectFile { t1File = it } }
                        ModalityZone("T1Gd — Contrast (.t1c)", t1cFile) { selectFile { t1cFile = it } }
                        ModalityZone("T2-weighted (.t2w)", t2File) { selectFile { t2File = it } }
                        ModalityZone("FLAIR (.t2f)", flairFile) { selectFile { flairFile = it } }
                    }
                }
            }

            // ─── Error / Status ───
            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (statusMessage.isNotEmpty()) {
                Text(statusMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }

            // ─── Run AI Inference Button ───
            Button(
                onClick = {
                    if (mode == PatientMode.NEW && !formFilled) {
                        errorMessage = "Please fill required patient fields"
                        return@Button
                    }
                    if (mode == PatientMode.EXISTING && selectedPatient == null) {
                        errorMessage = "Please select a patient"
                        return@Button
                    }
                    if (!allFilesSelected) {
                        errorMessage = "Please select all 4 MRI modality files"
                        return@Button
                    }
                    errorMessage = null
                    isProcessing = true
                    statusMessage = if (mode == PatientMode.NEW) "Creating patient record..." else "Preparing upload..."

                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val pId = if (mode == PatientMode.NEW) {
                                ApiClient.createPatient(
                                    firstName = firstName,
                                    lastName = lastName,
                                    dob = "${dob}T00:00:00Z",
                                    notes = notes.ifBlank { null }
                                )
                            } else {
                                selectedPatient!!.id
                            }
                            
                            withContext(Dispatchers.Main) { statusMessage = "Uploading MRI scans..." }

                            val paths = listOf(t1File!!.absolutePath, t1cFile!!.absolutePath, t2File!!.absolutePath, flairFile!!.absolutePath)
                            val responseJson = ApiClient.uploadPatientScans(patientId = pId, filePaths = paths)
                            val idMatch = "\"id\":\\s*(\\d+)".toRegex().find(responseJson)
                            val scanId = idMatch?.groupValues?.get(1)?.toInt()

                            withContext(Dispatchers.Main) { statusMessage = "Running AI inference... This may take a few minutes." }

                            if (scanId != null) {
                                var isDone = false
                                while (!isDone) {
                                    kotlinx.coroutines.delay(3000)
                                    try {
                                        val scanJson = ApiClient.getScanStatus(scanId)
                                        if (scanJson.contains("\"completed\"")) {
                                            isDone = true
                                            withContext(Dispatchers.Main) {
                                                onComplete(scanId)
                                            }
                                        } else if (scanJson.contains("\"failed\"")) {
                                            withContext(Dispatchers.Main) {
                                                errorMessage = "Analysis failed. Please check MRI files."
                                                isProcessing = false
                                                statusMessage = ""
                                            }
                                            isDone = true
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                errorMessage = "Error: ${e.message}"
                                isProcessing = false
                                statusMessage = ""
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = canSubmit
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Processing...")
                } else {
                    Text("Run AI Inference", style = MaterialTheme.typography.titleSmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ModalityZone(title: String, file: File?, onClick: () -> Unit) {
    if (file == null) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onClick),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text("Select file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onClick),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text(file.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
