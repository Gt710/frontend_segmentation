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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import java.awt.FileDialog

import java.awt.Frame
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.data.ApiClient
import org.example.project.data.Patient

enum class PatientMode { NEW, EXISTING }

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

    // Fetch patients for the "Existing Patient" mode (Mocked)
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
            // ─── Patient Information ───
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Інформація про пацієнта",
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
                            Text("Новий пацієнт", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { mode = PatientMode.EXISTING }) {
                            RadioButton(selected = mode == PatientMode.EXISTING, onClick = { mode = PatientMode.EXISTING })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Існуючий пацієнт", style = MaterialTheme.typography.bodyMedium)

                        }
                    }

                    if (mode == PatientMode.NEW) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = lastName, onValueChange = { lastName = it },
                                label = { Text("Прізвище *") },
                                modifier = Modifier.weight(1f), singleLine = true
                            )
                            OutlinedTextField(
                                value = firstName, onValueChange = { firstName = it },
                                label = { Text("Ім'я *") },
                                modifier = Modifier.weight(1f), singleLine = true
                            )

                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = dob, onValueChange = { dob = it },
                                label = { Text("РРРР-ММ-ДД*") },
                                modifier = Modifier.weight(1f), singleLine = true
                            )
                            OutlinedTextField(
                                value = phone, onValueChange = { phone = it },
                                label = { Text("Телефон") },
                                modifier = Modifier.weight(1f), singleLine = true
                            )

                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = notes, onValueChange = { notes = it },
                            label = { Text("Клінічні нотатки") },

                            modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3
                        )
                    } else {
                        // Existing Patient Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            var textFieldWidth by remember { mutableStateOf(0) }
                            val density = LocalDensity.current
                            
                            OutlinedTextField(
                                value = selectedPatient?.let { "${it.last_name}, ${it.first_name} (ID: ${it.id})" } ?: "Виберіть пацієнта",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Виберіть пацієнта *") },

                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { coordinates ->
                                        textFieldWidth = coordinates.size.width
                                    },
                                trailingIcon = {
                                    IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                        Text(if (dropdownExpanded) "▲" else "▼")
                                    }
                                }
                            )
                            // Overlay to make the entire field clickable
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { dropdownExpanded = true }
                            )

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.width(with(density) { textFieldWidth.toDp() })
                            ) {

                                if (patientsList.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Пацієнтів не знайдено") },

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
                        errorMessage = "Будь ласка, заповніть обов'язкові поля пацієнта"
                        return@Button
                    }
                    if (mode == PatientMode.EXISTING && selectedPatient == null) {
                        errorMessage = "Будь ласка, виберіть пацієнта"
                        return@Button
                    }
                    if (!allFilesSelected) {
                        errorMessage = "Будь ласка, виберіть усі 4 файли модальностей МРТ"
                        return@Button
                    }
                    errorMessage = null
                    isProcessing = true
                    statusMessage = "Завантаження знімків МРТ..."


                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val patientId = if (mode == PatientMode.NEW) {
                                withContext(Dispatchers.Main) { statusMessage = "Створення пацієнта..." }
                                val id = ApiClient.createPatient(firstName, lastName, dob, phone, notes)

                                if (id == 0) throw Exception("Не вдалося створити пацієнта")
                                id
                            } else {
                                selectedPatient!!.id
                            }

                            withContext(Dispatchers.Main) { statusMessage = "Завантаження та аналіз знімків МРТ..." }
                            
                            val filePaths = listOfNotNull(
                                t1File?.absolutePath,
                                t1cFile?.absolutePath,
                                t2File?.absolutePath,
                                flairFile?.absolutePath
                            )
                            
                            val responseJson = ApiClient.uploadPatientScans(patientId, filePaths)
                            
                            if (responseJson.contains("\"status\": \"error\"") || responseJson.contains("\"detail\"")) {
                                throw Exception("Аналіз не вдався: $responseJson")
                            }
                            
                            // Parse scan ID from response
                            val scanIdMatch = """"id"\s*:\s*(\d+)""".toRegex().find(responseJson.substringAfter("\"scans\""))
                            val scanId = scanIdMatch?.groupValues?.get(1)?.toInt() ?: throw Exception("Не вдалося отримати ID сканування з відповіді")



                            withContext(Dispatchers.Main) {
                                isProcessing = false
                                statusMessage = ""
                                onComplete(scanId)
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
                    Text("Обробка...")
                } else {
                    Text("Запустити ШІ аналіз", style = MaterialTheme.typography.titleSmall)
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
                Text("Вибрати файл", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

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
