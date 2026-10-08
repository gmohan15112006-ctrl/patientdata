package com.example.ui.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.ClinicalBackground
import com.example.ui.theme.HospitalEmergencyTertiary
import com.example.ui.theme.HospitalNavySecondary
import com.example.ui.theme.HospitalTealPrimary
import com.example.viewmodel.PatientFilterState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPatientsScreen(
    patients: List<PatientEntity>,
    nurses: List<UserEntity>,
    filterState: PatientFilterState,
    onUpdateSearchQuery: (String) -> Unit,
    onUpdateDateFilter: (String) -> Unit,
    onUpdateSpecialtyFilter: (String) -> Unit,
    onUpdateNurseFilter: (Long?) -> Unit,
    onUpdateSexFilter: (String) -> Unit,
    onUpdateAgeIntervalFilter: (String) -> Unit,
    onUpdateTaeiFilter: (String) -> Unit,
    onUpdateMedicolegalFilter: (String) -> Unit,
    onUpdateTransferredOutFilter: (String) -> Unit,
    onToggleShowDeleted: () -> Unit,
    onClearFilters: () -> Unit,
    onAddNewPatient: () -> Unit,
    onViewPatient: (PatientEntity) -> Unit,
    onEditPatient: (PatientEntity) -> Unit,
    onCopyPatient: (PatientEntity) -> Unit,
    onDeletePatient: (PatientEntity) -> Unit,
    onOpenExportDialog: () -> Unit,
    onBack: () -> Unit
) {
    var showFilterPanel by remember { mutableStateOf(false) }
    var patientToDelete by remember { mutableStateOf<PatientEntity?>(null) }

    if (patientToDelete != null) {
        ConfirmDeleteDialog(
            patientName = patientToDelete!!.name,
            ipNo = patientToDelete!!.ip_no,
            onConfirm = {
                onDeletePatient(patientToDelete!!)
                patientToDelete = null
            },
            onDismiss = { patientToDelete = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (filterState.showDeletedOnly) "Deleted Records Archive" else "All Inpatient Records",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${patients.size} records matched",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("all_patients_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showFilterPanel = !showFilterPanel },
                        modifier = Modifier.testTag("all_patients_toggle_filter")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Toggle Filter Panel",
                            tint = if (showFilterPanel) HospitalTealPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onOpenExportDialog,
                        modifier = Modifier.testTag("all_patients_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Export Excel",
                            tint = HospitalTealPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewPatient,
                containerColor = HospitalTealPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("all_patients_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Patient")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ClinicalBackground)
                .padding(paddingValues)
        ) {
            // Search Bar & Filter Toggle
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = filterState.query,
                        onValueChange = onUpdateSearchQuery,
                        placeholder = { Text("Search Patient Name, IP Number, or Diagnosis...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = HospitalTealPrimary) },
                        trailingIcon = {
                            if (filterState.query.isNotEmpty()) {
                                IconButton(onClick = { onUpdateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("all_patients_search_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Expandable Multi-faceted Filter Panel
                    if (showFilterPanel) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FilterPanel(
                            filterState = filterState,
                            nurses = nurses,
                            onUpdateDateFilter = onUpdateDateFilter,
                            onUpdateSpecialtyFilter = onUpdateSpecialtyFilter,
                            onUpdateNurseFilter = onUpdateNurseFilter,
                            onUpdateSexFilter = onUpdateSexFilter,
                            onUpdateAgeIntervalFilter = onUpdateAgeIntervalFilter,
                            onUpdateTaeiFilter = onUpdateTaeiFilter,
                            onUpdateMedicolegalFilter = onUpdateMedicolegalFilter,
                            onUpdateTransferredOutFilter = onUpdateTransferredOutFilter,
                            onToggleShowDeleted = onToggleShowDeleted,
                            onClearFilters = onClearFilters
                        )
                    }
                }
            }

            // Results summary bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Displaying ${patients.size} clinical entries",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Show Soft-Deleted",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = filterState.showDeletedOnly,
                            onCheckedChange = { onToggleShowDeleted() },
                            modifier = Modifier.testTag("toggle_deleted_switch")
                        )
                    }
                }
            }

            // Patient List
            if (patients.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No patient records match the filters.",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try clearing filters to see all available hospital records.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onClearFilters,
                            modifier = Modifier.testTag("clear_filters_button")
                        ) {
                            Text("Reset All Filters")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    items(patients, key = { it.id }) { patient ->
                        DoctorPatientDetailedCard(
                            patient = patient,
                            onView = { onViewPatient(patient) },
                            onEdit = { onEditPatient(patient) },
                            onCopy = { onCopyPatient(patient) },
                            onDelete = { patientToDelete = patient }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterPanel(
    filterState: PatientFilterState,
    nurses: List<UserEntity>,
    onUpdateDateFilter: (String) -> Unit,
    onUpdateSpecialtyFilter: (String) -> Unit,
    onUpdateNurseFilter: (Long?) -> Unit,
    onUpdateSexFilter: (String) -> Unit,
    onUpdateAgeIntervalFilter: (String) -> Unit,
    onUpdateTaeiFilter: (String) -> Unit,
    onUpdateMedicolegalFilter: (String) -> Unit,
    onUpdateTransferredOutFilter: (String) -> Unit,
    onToggleShowDeleted: () -> Unit,
    onClearFilters: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Advanced Search & Filters (Combined)",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = HospitalTealPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Date filter row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = filterState.dateFilter,
                    onValueChange = onUpdateDateFilter,
                    label = { Text("Admission Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("filter_date_input"),
                    shape = RoundedCornerShape(8.dp)
                )

                // Specialty filter
                FilterDropdown(
                    label = "Specialty",
                    options = listOf("All", "Trauma / Surgery", "Emergency Medicine", "Cardiology / CCU", "Neurology / Stroke", "Pediatric Emergency", "General Medicine"),
                    selected = filterState.specialty,
                    onSelect = onUpdateSpecialtyFilter,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Nurse filter & Sex filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val nurseOptions = listOf("All Nurses") + nurses.map { it.full_name }
                val currentNurseName = nurses.find { it.id == filterState.nurseId }?.full_name ?: "All Nurses"
                FilterDropdown(
                    label = "Nurse",
                    options = nurseOptions,
                    selected = currentNurseName,
                    onSelect = { selectedName ->
                        if (selectedName == "All Nurses") {
                            onUpdateNurseFilter(null)
                        } else {
                            val found = nurses.find { it.full_name == selectedName }
                            onUpdateNurseFilter(found?.id)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                    label = "Sex",
                    options = listOf("All", "Male", "Female", "Other"),
                    selected = filterState.sex,
                    onSelect = onUpdateSexFilter,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Age Interval & TAEI Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterDropdown(
                    label = "Age Interval",
                    options = listOf("All", "Below 12", "12-60", "Above 60"),
                    selected = filterState.ageInterval,
                    onSelect = onUpdateAgeIntervalFilter,
                    modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                    label = "TAEI Category",
                    options = listOf("All", "TAEI Pillar", "TAEI Non Pillar"),
                    selected = filterState.taeiCategory,
                    onSelect = onUpdateTaeiFilter,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Medicolegal & Transfer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterDropdown(
                    label = "Medicolegal",
                    options = listOf("All", "MLC", "Non-MLC"),
                    selected = filterState.medicolegal,
                    onSelect = onUpdateMedicolegalFilter,
                    modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                    label = "Transferred",
                    options = listOf("All", "Yes", "No"),
                    selected = filterState.transferredOut,
                    onSelect = onUpdateTransferredOutFilter,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onClearFilters,
                    modifier = Modifier.testTag("filter_reset_button")
                ) {
                    Text("Clear All Filters", color = HospitalEmergencyTertiary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(8.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DoctorPatientDetailedCard(
    patient: PatientEntity,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("doctor_patient_row_${patient.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (patient.status == "DELETED") MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: S.No, IP No, Badges
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
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (patient.status == "DELETED") {
                        StatusBadge(status = "DELETED")
                    }
                    StatusBadge(status = patient.medicolegal_category)
                    StatusBadge(status = patient.taei_category)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Patient Name, Age, Sex, Age Interval
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${patient.name} (${patient.age}y, ${patient.sex})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Age Group: ${patient.age_interval}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Clinical Info
            Text(
                text = "Specialty: ${patient.broad_speciality_category}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = HospitalTealPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = "Diagnosis: ${patient.diagnosis}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            // Transfer & Emergency Info
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Transfer: ${patient.transferred_out} ${if (patient.transferred_out_time.isNotBlank()) "(${patient.transferred_out_time})" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Response: ${patient.emergency_response_time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = HospitalNavySecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Metadata: Nurse, Admission Datetime
            Text(
                text = "Entered by: ${patient.entered_by_name} • Admitted: ${patient.admission_datetime} (Rec: ${patient.patient_received_time})",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Actions Row: View, Edit, Copy, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onView,
                    modifier = Modifier.height(34.dp).testTag("action_view_${patient.id}"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.height(34.dp).testTag("action_edit_${patient.id}"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = HospitalTealPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 11.sp, color = HospitalTealPrimary)
                }

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.height(34.dp).testTag("action_copy_${patient.id}"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = HospitalNavySecondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", fontSize = 11.sp, color = HospitalNavySecondary)
                }

                if (patient.status != "DELETED") {
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.height(34.dp).testTag("action_delete_${patient.id}"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = HospitalEmergencyTertiary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 11.sp, color = HospitalEmergencyTertiary)
                    }
                }
            }
        }
    }
}
