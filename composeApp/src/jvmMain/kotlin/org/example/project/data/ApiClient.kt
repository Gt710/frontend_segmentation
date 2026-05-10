package org.example.project.data

import androidx.compose.runtime.mutableStateListOf
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ApiClient {
    private val client = HttpClient.newBuilder().build()
    private const val BASE_URL = "http://localhost:8001"
    private const val USERNAME = "doctor" // Required by backend role checks

    fun getPatients(): List<Patient> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$BASE_URL/patients"))
            .header("username", USERNAME)
            .GET()
            .build()

        return try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                parsePatients(response.body())
            } else {
                println("Error getting patients: ${response.statusCode()} - ${response.body()}")
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }


    fun createPatient(firstName: String, lastName: String, dob: String, notes: String?): Int {
        val encodedFirstName = URLEncoder.encode(firstName, StandardCharsets.UTF_8.toString())
        val encodedLastName = URLEncoder.encode(lastName, StandardCharsets.UTF_8.toString())
        val encodedDob = URLEncoder.encode(dob, StandardCharsets.UTF_8.toString())
        val encodedPhone = URLEncoder.encode("+123456789", StandardCharsets.UTF_8.toString()) // Hardcoded phone
        val encodedNotes = notes?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) } ?: ""

        val url = "$BASE_URL/patients?first_name=$encodedFirstName&last_name=$encodedLastName&dob=$encodedDob&phone=$encodedPhone&notes=$encodedNotes"
        
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("username", USERNAME)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build()

        return try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val body = response.body()
                // Parse ID from response: {"id":X,...}
                val idMatch = """\{"id":(\d+)""".toRegex().find(body)
                idMatch?.groupValues?.get(1)?.toInt() ?: 0
            } else {
                println("Error creating patient: ${response.statusCode()} - ${response.body()}")
                0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    fun uploadPatientScans(patientId: Int, filePaths: List<String>): String {
        // Extract scan number from file path. Expecting something like "BraTS-GLI-00005"
        val firstPath = filePaths.firstOrNull() ?: return "{\"status\": \"error\", \"message\": \"No files selected\"}"
        val scanNumberMatch = """BraTS-GLI-(\d+)""".toRegex().find(firstPath)
        val scanNumber = scanNumberMatch?.groupValues?.get(1) ?: "00005" // Default fallback

        val url = "$BASE_URL/analyze?patient_id=$patientId&scan_number=$scanNumber"
        
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("username", USERNAME)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build()

        return try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                response.body()
            } else {
                "{\"status\": \"error\", \"message\": \"${response.statusCode()} - ${response.body()}\"}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "{\"status\": \"error\", \"message\": \"${e.message}\"}"
        }
    }

    fun getScanSlice(scanId: Int, sliceIdx: Int): ByteArray? {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$BASE_URL/scans/$scanId/slice/$sliceIdx"))
            .header("username", USERNAME)
            .GET()
            .build()

        return try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
            if (response.statusCode() == 200) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getScanReport(scanId: Int): ByteArray? {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$BASE_URL/scans/$scanId/report"))
            .header("username", USERNAME)
            .GET()
            .build()

        return try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
            if (response.statusCode() == 200) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getScanDetails(scanId: Int): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$BASE_URL/scans/$scanId"))
            .header("username", USERNAME)
            .GET()
            .build()

        return try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                response.body()
            } else {
                "{\"status\": \"error\", \"message\": \"${response.statusCode()}\"}"
            }
        } catch (e: Exception) {
            "{\"status\": \"error\", \"message\": \"${e.message}\"}"
        }
    }

    fun deletePatient(patientId: Int): Boolean {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$BASE_URL/patients/$patientId"))
            .header("username", USERNAME)
            .DELETE()
            .build()

        return try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            response.statusCode() == 200
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteScan(scanId: Int): Boolean {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$BASE_URL/scans/$scanId"))
            .header("username", USERNAME)
            .DELETE()
            .build()

        return try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            response.statusCode() == 200
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun parsePatients(json: String): List<Patient> {

        val list = mutableListOf<Patient>()
        
        // Split by patient objects. Each patient starts with "id":
        val patientParts = json.split("\"id\":").drop(1)
        
        for (part in patientParts) {
            if (!part.contains("\"first_name\"")) continue
            val idMatch = """^(\d+)""".toRegex().find(part)

            val id = idMatch?.groupValues?.get(1)?.toInt() ?: continue
            
            val firstName = """"first_name"\s*:\s*"([^"]*)"""".toRegex().find(part)?.groupValues?.get(1) ?: ""
            val lastName = """"last_name"\s*:\s*"([^"]*)"""".toRegex().find(part)?.groupValues?.get(1) ?: ""
            val dob = """"dob"\s*:\s*"([^"]*)"""".toRegex().find(part)?.groupValues?.get(1) ?: ""
            val phone = """"phone"\s*:\s*"([^"]*)"""".toRegex().find(part)?.groupValues?.get(1) ?: ""
            val notes = """"notes"\s*:\s*"([^"]*)"""".toRegex().find(part)?.groupValues?.get(1)
            
            val scans = mutableListOf<Scan>()
            val scansPart = part.substringAfter("\"scans\":[", "")
            if (scansPart.isNotEmpty()) {
                val scansContent = scansPart.substringBefore("]")
                // Match scans non-greedily
                val scanMatches = """\{"id":(\d+)[^}]*"status":"([^"]*)"[^}]*"upload_date":"([^"]*)"[^}]*"tumor_volume_cm3":([^,}]*)[^}]*"conclusion":"([^"]*)"\}""".toRegex().findAll(scansContent)
                
                for (scanMatch in scanMatches) {
                    val scanId = scanMatch.groupValues[1].toInt()
                    val status = scanMatch.groupValues[2]
                    val uploadDate = scanMatch.groupValues[3]
                    val tumorVolume = scanMatch.groupValues[4].let { if (it == "null") null else it.toDouble() }
                    val conclusion = scanMatch.groupValues[5]
                    scans.add(Scan(scanId, id, status, uploadDate, tumorVolume, conclusion))
                }
            }
            
            list.add(Patient(id, firstName, lastName, dob, phone, notes, scans))
        }
        return list
    }

}
