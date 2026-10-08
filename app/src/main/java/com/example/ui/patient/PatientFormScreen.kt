package com.example.ui.patient

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HospitalSettingEntity
import com.example.data.model.PatientEntity
import com.example.data.model.UserEntity
import com.example.ui.theme.ClinicalBackground
import com.example.ui.theme.HospitalEmergencyTertiary
import com.example.ui.theme.HospitalNavySecondary
import com.example.ui.theme.HospitalTealPrimary
import com.example.ui.theme.StatusWarning
import com.example.viewmodel.PatientFormMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientFormScreen(
    currentUser: UserEntity,
    formMode: PatientFormMode,
    initialPatient: PatientEntity?,
    settings: List<HospitalSettingEntity>,
    onSavePatient: (PatientEntity, Boolean, (Boolean, String) -> Unit) -> Unit,
    onNavigateBack: () -> Unit
) {
    val currentDateTimeFormatted = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    }
    val currentTimeFormatted = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    // Form fields
    var ipNo by remember { mutableStateOf(initialPatient?.ip_no ?: "") }
    var name by remember { mutableStateOf(initialPatient?.name ?: "") }
    var ageString by remember { mutableStateOf(initialPatient?.age?.toString() ?: "") }
    var sex by remember { mutableStateOf(initialPatient?.sex ?: "Male") }

    var admissionDateTime by remember {
        mutableStateOf(initialPatient?.admission_datetime ?: currentDateTimeFormatted)
    }
    var patientReceivedTime by remember {
        mutableStateOf(initialPatient?.patient_received_time ?: currentTimeFormatted)
    }

    var broadSpecialty by remember {
        mutableStateOf(initialPatient?.broad_speciality_category ?: "Emergency Medicine")
    }
    var diagnosis by remember {
        mutableStateOf(initialPatient?.diagnosis ?: "Acute Chest Pain / Evaluation")
    }
    var ageInterval by remember {
        mutableStateOf(initialPatient?.age_interval ?: "12-60")
    }
    var taeiCategory by remember {
        mutableStateOf(initialPatient?.taei_category ?: "TAEI Pillar")
    }
    var medicolegalCategory by remember {
        mutableStateOf(initialPatient?.medicolegal_category ?: "Non-MLC")
    }

    var transferredOut by remember {
        mutableStateOf(initialPatient?.transferred_out ?: "No")
    }
    var transferredOutTime by remember {
        mutableStateOf(initialPatient?.transferred_out_time ?: "")
    }
    var emergencyResponseTime by remember {
        mutableStateOf(initialPatient?.emergency_response_time ?: "Immediate (<1 min)")
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isSaveSuccess by remember { mutableStateOf(false) }
    var successDetails by remember { mutableStateOf("") }

    // Warning confirmation dialog for COPY duplicate IP
    var showCopyDuplicateIpDialog by remember { mutableStateOf(false) }
    var pendingPatientToSave by remember { mutableStateOf<PatientEntity?>(null) }

    fun autoSuggestAgeInterval(newAgeStr: String) {
        val parsedAge = newAgeStr.toIntOrNull()
        if (parsedAge != null) {
            ageInterval = when {
                parsedAge < 12 -> "Below 12"
                parsedAge in 12..60 -> "12-60"
                else -> "Above 60"
            }
        }
    }

    fun validateAndSubmit(forceConfirmDuplicateIp: Boolean = false) {
        if (ipNo.isBlank()) {
            errorMessage = "In-Patient Number (IP No) is mandatory."
            return
        }
        if (name.isBlank()) {
            errorMessage = "Patient Name is mandatory."
            return
        }
        val age = ageString.toIntOrNull()
        if (age == null || age < 0 || age > 130) {
            errorMessage = "Please enter a valid patient age (0-130)."
            return
        }
        if (admissionDateTime.isBlank()) {
            errorMessage = "Admission Date & Time is required."
            return
        }
        if (patientReceivedTime.isBlank()) {
            errorMessage = "Patient Received Time is required."
            return
        }
        if (broadSpecialty.isBlank()) {
            errorMessage = "Broad Specialty Category is required."
            return
        }
        if (diagnosis.isBlank()) {
            errorMessage = "Diagnosis is required."
            return
        }
        if (transferredOut.startsWith("Yes") && transferredOutTime.isBlank()) {
            errorMessage = "Transfer Out Time is required when patient is transferred."
            return
        }

        errorMessage = null
        isSubmitting = true

        val patientRecord = PatientEntity(
            id = if (formMode == PatientFormMode.EDIT) (initialPatient?.id ?: 0L) else 0L,
            serial_no = if (formMode == PatientFormMode.EDIT) (initialPatient?.serial_no ?: 0L) else 0L,
            ip_no = ipNo.trim(),
            name = name.trim(),
            age = age,
            sex = sex,
            admission_datetime = admissionDateTime.trim(),
            patient_received_time = patientReceivedTime.trim(),
            broad_speciality_category = broadSpecialty.trim(),
            diagnosis = diagnosis.trim(),
            age_interval = ageInterval,
            taei_category = taeiCategory,
            medicolegal_category = medicolegalCategory,
            transferred_out = transferredOut,
            transferred_out_time = transferredOutTime.trim(),
            emergency_response_time = emergencyResponseTime,
            entered_by = initialPatient?.entered_by ?: currentUser.id,
            entered_by_name = initialPatient?.entered_by_name ?: currentUser.full_name
        )

        onSavePatient(patientRecord, forceConfirmDuplicateIp) { success, message ->
            isSubmitting = false
            if (success) {
                isSaveSuccess = true
                successDetails = message
            } else {
                if (message.startsWith("WARNING_DUPLICATE_IP")) {
                    pendingPatientToSave = patientRecord
                    showCopyDuplicateIpDialog = true
                } else {
                    errorMessage = message
                }
            }
        }
    }

    // Duplicate IP Dialog for Copy feature
    if (showCopyDuplicateIpDialog && pendingPatientToSave != null) {
        AlertDialog(
            onDismissRequest = { showCopyDuplicateIpDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning) },
            title = { Text("Duplicate IP Number Warning") },
            text = {
                Column {
                    Text(
                        "You are copying a record with IP No '${pendingPatientToSave?.ip_no}', which already exists in the system."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Are you sure you want to retain this exact IP Number, or would you like to update the IP Number before saving?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCopyDuplicateIpDialog = false
                        validateAndSubmit(forceConfirmDuplicateIp = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HospitalNavySecondary),
                    modifier = Modifier.testTag("confirm_duplicate_ip_button")
                ) {
                    Text("Confirm Retain IP")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCopyDuplicateIpDialog = false },
                    modifier = Modifier.testTag("dismiss_duplicate_ip_button")
                ) {
                    Text("Edit IP No")
                }
            }
        )
    }

    val pageTitle = when (formMode) {
        PatientFormMode.CREATE -> "New Emergency Patient Entry"
        PatientFormMode.EDIT -> "Edit Patient Record"
        PatientFormMode.COPY -> "Copy & Review Patient Record"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pageTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("patient_form_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        if (isSaveSuccess) {
            // Success Screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ClinicalBackground)
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(HospitalTealPrimary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = HospitalTealPrimary,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Patient record saved successfully.",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Record registered in hospital database and entered in clinical audit log.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                // Reset form for next patient
                                ipNo = ""
                                name = ""
                                ageString = ""
                                isSaveSuccess = false
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_another_patient_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HospitalTealPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Another Patient", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("return_dashboard_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Dashboard, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Return to Dashboard")
                        }
                    }
                }
            }
        } else {
            // Full Form Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ClinicalBackground)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 1. Patient Information Section
                FormSectionCard(title = "1. Patient Information") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = if (initialPatient != null && initialPatient.serial_no > 0) "#${initialPatient.serial_no}" else "Auto (Next S.No)",
                            onValueChange = {},
                            label = { Text("S.No") },
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = ipNo,
                            onValueChange = { ipNo = it; errorMessage = null },
                            label = { Text("IP No * (e.g. IP-8921)") },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f).testTag("form_ip_no_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMessage = null },
                        label = { Text("Patient Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("form_patient_name_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ageString,
                            onValueChange = {
                                ageString = it
                                autoSuggestAgeInterval(it)
                                errorMessage = null
                            },
                            label = { Text("Age (Years) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("form_age_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        DropdownSelector(
                            label = "Sex *",
                            options = listOf("Male", "Female", "Other"),
                            selected = sex,
                            onSelect = { sex = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 2. Admission Information Section
                FormSectionCard(title = "2. Admission Information") {
                    OutlinedTextField(
                        value = admissionDateTime,
                        onValueChange = { admissionDateTime = it },
                        label = { Text("Admission Date & Time (YYYY-MM-DD HH:mm) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("form_admission_datetime_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = patientReceivedTime,
                        onValueChange = { patientReceivedTime = it },
                        label = { Text("Patient Received Time (HH:mm) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("form_received_time_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // 3. Clinical Information Section
                FormSectionCard(title = "3. Clinical Information") {
                    DropdownSelector(
                        label = "Broad Specialty Category *",
                        options = listOf(
                            "Trauma / Surgery",
                            "Emergency Medicine",
                            "Cardiology / CCU",
                            "Neurology / Stroke",
                            "Pediatric Emergency",
                            "Orthopedics",
                            "Toxicology & Poison",
                            "General Medicine"
                        ),
                        selected = broadSpecialty,
                        onSelect = { broadSpecialty = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = diagnosis,
                        onValueChange = { diagnosis = it; errorMessage = null },
                        label = { Text("Clinical Diagnosis *") },
                        modifier = Modifier.fillMaxWidth().testTag("form_diagnosis_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DropdownSelector(
                            label = "Age Interval *",
                            options = listOf("Below 12", "12-60", "Above 60"),
                            selected = ageInterval,
                            onSelect = { ageInterval = it },
                            modifier = Modifier.weight(1f)
                        )

                        DropdownSelector(
                            label = "TAEI Pillar *",
                            options = listOf("TAEI Pillar", "TAEI Non Pillar"),
                            selected = taeiCategory,
                            onSelect = { taeiCategory = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    DropdownSelector(
                        label = "Medicolegal Category *",
                        options = listOf("MLC", "Non-MLC"),
                        selected = medicolegalCategory,
                        onSelect = { medicolegalCategory = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 4. Transfer Information Section
                FormSectionCard(title = "4. Transfer Information") {
                    DropdownSelector(
                        label = "Transferred Out",
                        options = listOf(
                            "No",
                            "Yes - Emergency OT",
                            "Yes - Intensive Care Unit (ICU)",
                            "Yes - Coronary Care Unit (CCU)",
                            "Yes - Tertiary Trauma Center",
                            "Yes - Inpatient Ward"
                        ),
                        selected = transferredOut,
                        onSelect = { transferredOut = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (transferredOut.startsWith("Yes")) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = transferredOutTime,
                            onValueChange = { transferredOutTime = it },
                            label = { Text("Transferred Out Time (HH:mm) *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("form_transfer_time_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // 5. Emergency Information Section
                FormSectionCard(title = "5. Emergency Response Information") {
                    DropdownSelector(
                        label = "Emergency Response Time",
                        options = listOf(
                            "Immediate (<1 min)",
                            "2 mins",
                            "3 mins",
                            "5 mins",
                            "10 mins",
                            "15 mins"
                        ),
                        selected = emergencyResponseTime,
                        onSelect = { emergencyResponseTime = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Metadata note
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "System Metadata: Logged-in Staff: ${currentUser.full_name} (${currentUser.role}) • S.No & Timestamps automatically recorded upon submission.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // Submit Button
                Button(
                    onClick = { validateAndSubmit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_patient_form_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HospitalTealPrimary),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (formMode == PatientFormMode.EDIT) "Save Changes" else "Save Patient Record",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun FormSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = HospitalTealPrimary
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
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
            shape = RoundedCornerShape(10.dp)
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
