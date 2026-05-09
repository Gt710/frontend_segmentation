package org.example.project.data

import androidx.compose.runtime.mutableStateListOf

object ApiClient {
    val mockPatients = mutableStateListOf(
        Patient(
            id = 1,
            first_name = "John",
            last_name = "Doe",
            dob = "1980-01-01T00:00:00Z",
            phone = "+123456789",
            notes = "No special notes",
            scans = listOf(
                Scan(1, 1, "completed", "2026-05-01T10:00:00Z", 12.5, "Conclusion text"),
                Scan(2, 1, "failed", "2026-05-02T11:00:00Z", null, "Failed scan")
            )
        ),
        Patient(
            id = 2,
            first_name = "Jane",
            last_name = "Smith",
            dob = "1990-05-05T00:00:00Z",
            phone = "+987654321",
            notes = "Allergic to something",
            scans = listOf(
                Scan(3, 2, "completed", "2026-05-03T09:00:00Z", 5.2, "Looks better")
            )
        )
    )

    fun getPatients(): List<Patient> = mockPatients

    fun deletePatient(id: Int) {
        mockPatients.removeIf { it.id == id }
    }

    fun deleteScan(id: Int) {
        val patient = mockPatients.find { p -> p.scans.any { it.id == id } }
        if (patient != null) {
            val updatedScans = patient.scans.filter { it.id != id }
            val index = mockPatients.indexOf(patient)
            mockPatients[index] = patient.copy(scans = updatedScans)
        }
    }

    fun createPatient(firstName: String, lastName: String, dob: String, notes: String?): Int {
        val id = (mockPatients.maxOfOrNull { it.id } ?: 0) + 1
        mockPatients.add(Patient(id, firstName, lastName, dob, null, notes, emptyList()))
        return id
    }

    fun uploadPatientScans(patientId: Int, filePaths: List<String>): String {
        return "{\"id\": 10}" // Mock scan ID
    }

    fun getScanStatus(scanId: Int): String {
        return "{\"status\": \"completed\", \"tumor_volume_cm3\": 15.0, \"conclusion\": \"Mock conclusion\"}"
    }

    fun getScanSliceInfo(scanId: Int): String {
        return "{\"total_slices\": 100, \"center_slice\": 50}"
    }

    fun exportReport(scanId: Int): ByteArray {
        return ByteArray(0) // Empty PDF
    }
}
