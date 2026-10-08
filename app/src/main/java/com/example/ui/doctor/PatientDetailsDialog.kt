package com.example.ui.doctor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TransferWithinAStation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PatientEntity
import com.example.ui.components.StatusBadge
import com.example.ui.theme.HospitalNavySecondary
import com.example.ui.theme.HospitalTealPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PatientDetailsDialog(
    patient: PatientEntity,
    onDismiss: () -> Unit
) {
    val createdFormatted = remember(patient.created_at) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(patient.created_at))
    }
    val updatedFormatted = remember(patient.updated_at) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(patient.updated_at))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Patient Clinical File",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "S.No #${patient.serial_no} • IP: ${patient.ip_no}",
                        style = MaterialTheme.typography.bodySmall,
                        color = HospitalTealPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_patient_details")) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusBadge(status = patient.status)
                    StatusBadge(status = patient.medicolegal_category)
                    StatusBadge(status = patient.taei_category)
                }

                // 1. Patient Information
                DetailSectionCard(title = "Patient Information", icon = Icons.Default.Person) {
                    DetailRow("Full Name", patient.name)
                    DetailRow("IP Number", patient.ip_no)
                    DetailRow("Age", "${patient.age} years")
                    DetailRow("Sex", patient.sex)
                    DetailRow("Age Interval", patient.age_interval)
                }

                // 2. Admission Information
                DetailSectionCard(title = "Admission Information", icon = Icons.Default.Schedule) {
                    DetailRow("Admission Date & Time", patient.admission_datetime)
                    DetailRow("Patient Received Time", patient.patient_received_time)
                }

                // 3. Clinical Information
                DetailSectionCard(title = "Clinical Information", icon = Icons.Default.MedicalServices) {
                    DetailRow("Broad Specialty", patient.broad_speciality_category)
                    DetailRow("Clinical Diagnosis", patient.diagnosis)
                    DetailRow("TAEI Classification", patient.taei_category)
                    DetailRow("Medicolegal Category", patient.medicolegal_category)
                }

                // 4. Transfer Information
                DetailSectionCard(title = "Transfer Information", icon = Icons.Default.TransferWithinAStation) {
                    DetailRow("Transferred Out", patient.transferred_out)
                    if (patient.transferred_out_time.isNotBlank()) {
                        DetailRow("Transfer Time", patient.transferred_out_time)
                    }
                }

                // 5. Emergency Information
                DetailSectionCard(title = "Emergency Information", icon = Icons.Default.Schedule) {
                    DetailRow("Emergency Response Time", patient.emergency_response_time)
                }

                // 6. Record Metadata
                DetailSectionCard(title = "Audit Metadata", icon = Icons.Default.Person) {
                    DetailRow("Entered By Nurse", "${patient.entered_by_name} (ID: #${patient.entered_by})")
                    DetailRow("Created Timestamp", createdFormatted)
                    DetailRow("Last Updated", updatedFormatted)
                    if (patient.updated_by_name != null) {
                        DetailRow("Updated By", patient.updated_by_name)
                    }
                    if (patient.deleted_at != null) {
                        val deletedFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(patient.deleted_at))
                        DetailRow("Deleted At", deletedFormatted)
                        DetailRow("Deleted By", patient.deleted_by_name ?: "Doctor")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun DetailSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = HospitalTealPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = HospitalNavySecondary
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
