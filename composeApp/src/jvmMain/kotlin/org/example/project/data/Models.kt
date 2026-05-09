package org.example.project.data

data class Patient(
    val id: Int,
    val first_name: String,
    val last_name: String,
    val dob: String,
    val phone: String?,
    val notes: String?,
    val scans: List<Scan>
)

data class Scan(
    val id: Int,
    val patient_id: Int,
    val status: String,
    val upload_date: String,
    val tumor_volume_cm3: Double?,
    val conclusion: String?
)
