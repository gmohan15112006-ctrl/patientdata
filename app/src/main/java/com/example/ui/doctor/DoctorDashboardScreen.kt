package com.example.ui.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PatientEntity
import com.example.data.model.UserEntity
import com.example.ui.components.HospitalTopBar
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.ClinicalBackground
import com.example.ui.theme.HospitalEmergencyTertiary
import com.example.ui.theme.HospitalNavySecondary
import com.example.ui.theme.HospitalTealPrimary
import com.example.ui.theme.StatusWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DoctorDashboardScreen(
    currentUser: UserEntity,
    allPatients: List<PatientEntity>,
    nurses: List<UserEntity>,
    onViewAllPatients: () -> Unit,
    onAddNewPatient: () -> Unit,
    onOpenExportDialog: () -> Unit,
    onViewUserManagement: () -> Unit,
    onViewAuditLogs: () -> Unit,
    onViewSettings: () -> Unit,
    onViewPatientDetails: (PatientEntity) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onLogout: () -> Unit
) {
    val todayDatePrefix = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayPatientsCount = allPatients.count { it.admission_datetime.startsWith(todayDatePrefix) }
    val emergencyCount = allPatients.count {
        it.emergency_response_time.contains("Immediate", ignoreCase = true) ||
                it.broad_speciality_category.contains("Trauma", ignoreCase = true) ||
                it.broad_speciality_category.contains("Emergency", ignoreCase = true)
    }
    val surgeryCount = allPatients.count { it.broad_speciality_category.contains("Surgery", ignoreCase = true) }
    val medicineCount = allPatients.count { it.broad_speciality_category.contains("Medicine", ignoreCase = true) || it.broad_speciality_category.contains("Cardiology", ignoreCase = true) }

    var quickSearchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            HospitalTopBar(
                title = "Hospital Administrative Command",
                currentUser = currentUser,
                onLogoutClick = onLogout
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ClinicalBackground)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Banner
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(HospitalNavySecondary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupervisorAccount,
                                contentDescription = null,
                                tint = HospitalNavySecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser.full_name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Chief Medical Administrator • ${currentUser.department}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onOpenExportDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = HospitalTealPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("doctor_quick_export_button")
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Search
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = quickSearchQuery,
                            onValueChange = { quickSearchQuery = it },
                            placeholder = { Text("Quick search patient, IP No, or diagnosis...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = HospitalTealPrimary) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("doctor_quick_search_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSearchSubmit(quickSearchQuery)
                                onViewAllPatients()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HospitalTealPrimary),
                            modifier = Modifier.testTag("doctor_quick_search_submit")
                        ) {
                            Text("Find")
                        }
                    }
                }
            }

            // Administrative Overview Cards (6 metrics)
            item {
                Text(
                    text = "Hospital Inpatient & Emergency Metrics",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Total Patients",
                        value = allPatients.size.toString(),
                        icon = Icons.Default.People,
                        iconColor = HospitalTealPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = onViewAllPatients
                    )
                    StatCard(
                        title = "Today's Patients",
                        value = todayPatientsCount.toString(),
                        icon = Icons.Default.Today,
                        iconColor = HospitalNavySecondary,
                        modifier = Modifier.weight(1f),
                        onClick = onViewAllPatients
                    )
                    StatCard(
                        title = "Total Nurses",
                        value = nurses.size.toString(),
                        icon = Icons.Default.SupervisorAccount,
                        iconColor = StatusWarning,
                        modifier = Modifier.weight(1f),
                        onClick = onViewUserManagement
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Emergency Cases",
                        value = emergencyCount.toString(),
                        icon = Icons.Default.Emergency,
                        iconColor = HospitalEmergencyTertiary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Surgery Cases",
                        value = surgeryCount.toString(),
                        icon = Icons.Default.LocalHospital,
                        iconColor = HospitalTealPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Medicine Cases",
                        value = medicineCount.toString(),
                        icon = Icons.Default.Medication,
                        iconColor = HospitalNavySecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Admin Modules Grid Actions
            item {
                Text(
                    text = "Administrative Modules",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewAllPatients,
                        modifier = Modifier.weight(1f).testTag("doctor_nav_all_patients"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("All Patients", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onAddNewPatient,
                        modifier = Modifier.weight(1f).testTag("doctor_nav_new_patient"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Patient", fontSize = 12.sp)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewUserManagement,
                        modifier = Modifier.weight(1f).testTag("doctor_nav_user_management"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nurse Accounts", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onViewAuditLogs,
                        modifier = Modifier.weight(1f).testTag("doctor_nav_audit_logs"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Audit Trail", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onViewSettings,
                        modifier = Modifier.weight(1f).testTag("doctor_nav_settings"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Settings", fontSize = 12.sp)
                    }
                }
            }

            // Recent Patient Entries
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Inpatient Admissions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "View All (${allPatients.size})",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = HospitalTealPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .clickable { onViewAllPatients() }
                            .padding(4.dp)
                    )
                }
            }

            items(allPatients.take(5)) { patient ->
                DoctorPatientSummaryCard(
                    patient = patient,
                    onClick = { onViewPatientDetails(patient) }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun DoctorPatientSummaryCard(
    patient: PatientEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("doctor_recent_patient_${patient.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = HospitalNavySecondary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "S.No #${patient.serial_no}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = HospitalNavySecondary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = patient.ip_no,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusBadge(status = patient.medicolegal_category)
                    StatusBadge(status = patient.taei_category)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${patient.name} (${patient.age}y, ${patient.sex})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Dx: ${patient.diagnosis} • ${patient.broad_speciality_category}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Staff: ${patient.entered_by_name} • ${patient.admission_datetime}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Review",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = HospitalTealPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = HospitalTealPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
