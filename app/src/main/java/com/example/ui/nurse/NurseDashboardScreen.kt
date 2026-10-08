package com.example.ui.nurse

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
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NurseDashboardScreen(
    currentUser: UserEntity,
    myPatients: List<PatientEntity>,
    onAddNewPatient: () -> Unit,
    onViewMyRecords: () -> Unit,
    onSearchMyRecords: () -> Unit,
    onViewProfile: () -> Unit,
    onViewPatientDetails: (PatientEntity) -> Unit,
    onLogout: () -> Unit
) {
    val todayDatePrefix = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayEntriesCount = myPatients.count { it.admission_datetime.startsWith(todayDatePrefix) }
    val emergencyCount = myPatients.count {
        it.emergency_response_time.contains("Immediate", ignoreCase = true) ||
                it.broad_speciality_category.contains("Trauma", ignoreCase = true) ||
                it.broad_speciality_category.contains("Emergency", ignoreCase = true)
    }

    Scaffold(
        topBar = {
            HospitalTopBar(
                title = "Nurse Clinical Station",
                currentUser = currentUser,
                onProfileClick = onViewProfile,
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
            // Welcome Header
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
                                .background(HospitalTealPrimary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalHospital,
                                contentDescription = null,
                                tint = HospitalTealPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welcome, ${currentUser.full_name}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Role: NURSE • ${currentUser.employee_id} • ${currentUser.department}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // PRIMARY EMERGENCY ACTION BUTTON
            item {
                Button(
                    onClick = onAddNewPatient,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("nurse_add_patient_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HospitalTealPrimary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add New Patient",
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "NEW PATIENT ENTRY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }

            // Quick Nav Links
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewMyRecords,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nurse_my_records_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("My Records", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onSearchMyRecords,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nurse_search_records_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onViewProfile,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nurse_profile_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Profile", fontSize = 13.sp)
                    }
                }
            }

            // Summary Cards Grid (2x2)
            item {
                Text(
                    text = "Shift Summary",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Today's Entries",
                        value = todayEntriesCount.toString(),
                        icon = Icons.Default.Today,
                        iconColor = HospitalTealPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "My Total Entries",
                        value = myPatients.size.toString(),
                        icon = Icons.Default.ListAlt,
                        iconColor = HospitalNavySecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Emergency Cases",
                        value = emergencyCount.toString(),
                        icon = Icons.Default.Emergency,
                        iconColor = HospitalEmergencyTertiary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Active Status",
                        value = "Online",
                        icon = Icons.Default.LocalHospital,
                        iconColor = HospitalTealPrimary,
                        subtitle = "Database Connected",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Recent Entries Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Patient Admissions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "View All (${myPatients.size})",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = HospitalTealPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier
                            .clickable { onViewMyRecords() }
                            .padding(4.dp)
                    )
                }
            }

            // Recent Entries List
            if (myPatients.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No patient entries yet for your account.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onAddNewPatient,
                                colors = ButtonDefaults.buttonColors(containerColor = HospitalTealPrimary)
                            ) {
                                Text("Add First Patient")
                            }
                        }
                    }
                }
            } else {
                items(myPatients.take(5)) { patient ->
                    NursePatientItemCard(
                        patient = patient,
                        onClick = { onViewPatientDetails(patient) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun NursePatientItemCard(
    patient: PatientEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("nurse_patient_card_${patient.id}"),
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
                StatusBadge(status = patient.medicolegal_category)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${patient.name} (${patient.age}y, ${patient.sex})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Dx: ${patient.diagnosis}",
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
                    text = "Admitted: ${patient.admission_datetime}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "View Details",
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
