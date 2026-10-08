package com.example.ui.doctor

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PatientEntity
import com.example.excel.ExcelExporter
import com.example.ui.theme.ClinicalBackground
import com.example.ui.theme.HospitalNavySecondary
import com.example.ui.theme.HospitalTealPrimary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelExportScreen(
    filteredPatients: List<PatientEntity>,
    allPatients: List<PatientEntity>,
    generatedFile: File?,
    onExportToExcel: (String, String, (File) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val todayFormatted = remember {
        SimpleDateFormat("MMM_dd_yyyy", Locale.getDefault()).format(Date())
    }

    var fileName by remember { mutableStateOf("Hospital_Patient_Report_$todayFormatted.xlsx") }
    var exportOption by remember { mutableStateOf("FILTERED") } // "ALL", "FILTERED", "MULTI_DATE"
    var isGenerating by remember { mutableStateOf(false) }
    var latestFile by remember { mutableStateOf<File?>(generatedFile) }
    var showPreviewGrid by remember { mutableStateOf(false) }

    val currentRecordsToExport = when (exportOption) {
        "FILTERED" -> filteredPatients
        else -> allPatients
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Patient Excel Spreadsheet", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("export_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ClinicalBackground)
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = HospitalTealPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Standard Hospital Excel Configuration",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // File Name Input
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("File Name (.xlsx automatically appended)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_filename_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Export Scope & Sheet Structure:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 1: Filtered records
                    ExportRadioOption(
                        title = "Currently Filtered Records (${filteredPatients.size} records)",
                        subtitle = "Exports only records matching active search and filter criteria.",
                        selected = exportOption == "FILTERED",
                        onSelect = { exportOption = "FILTERED" }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Option 2: All Active records
                    ExportRadioOption(
                        title = "All Hospital Records (${allPatients.size} records)",
                        subtitle = "Exports the entire active hospital emergency database in a single worksheet.",
                        selected = exportOption == "ALL",
                        onSelect = { exportOption = "ALL" }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Option 3: Date-wise Multi-sheet Workbook
                    ExportRadioOption(
                        title = "Date-Wise Multi-Sheet Workbook (AUG 11, AUG 12...)",
                        subtitle = "Replicates hospital date-wise workflow: creates a separate worksheet tab for each admission date.",
                        selected = exportOption == "MULTI_DATE",
                        onSelect = { exportOption = "MULTI_DATE" }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Export Button
                    Button(
                        onClick = {
                            isGenerating = true
                            onExportToExcel(fileName, exportOption) { file ->
                                isGenerating = false
                                latestFile = file
                                Toast.makeText(context, "Generated ${file.name}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_excel_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HospitalTealPrimary),
                        enabled = !isGenerating
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Generate .xlsx Workbook",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Export Success & Share Section
            if (latestFile != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HospitalTealPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Spreadsheet Ready: ${latestFile!!.name}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Size: ${latestFile!!.length() / 1024} KB • OpenXML Compliant (.xlsx)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val intent = ExcelExporter.createShareIntent(context, latestFile!!)
                                    context.startActivity(android.content.Intent.createChooser(intent, "Open or Share Excel Report"))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_excel_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HospitalNavySecondary)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share / Open with App")
                            }

                            OutlinedButton(
                                onClick = { showPreviewGrid = !showPreviewGrid },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("toggle_excel_preview_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (showPreviewGrid) "Hide Preview" else "Preview Grid")
                            }
                        }
                    }
                }
            }

            // Preview Table of required 15 columns
            if (showPreviewGrid) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Spreadsheet Column Preview (15 Exact Columns)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            Column {
                                // Header row
                                Surface(
                                    color = HospitalTealPrimary,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                        ExcelExporter.REQUIRED_HEADERS.forEach { header ->
                                            Text(
                                                text = header,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier
                                                    .width(140.dp)
                                                    .padding(horizontal = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Sample preview rows
                                currentRecordsToExport.take(10).forEachIndexed { index, p ->
                                    val rowBg = if (index % 2 == 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                                    Surface(color = rowBg) {
                                        Row(modifier = Modifier.padding(vertical = 6.dp)) {
                                            val cols = listOf(
                                                p.serial_no.toString(),
                                                p.ip_no,
                                                p.name,
                                                p.age.toString(),
                                                p.sex,
                                                p.admission_datetime,
                                                p.patient_received_time,
                                                p.broad_speciality_category,
                                                p.diagnosis,
                                                p.age_interval,
                                                p.taei_category,
                                                p.medicolegal_category,
                                                p.transferred_out,
                                                p.transferred_out_time,
                                                p.emergency_response_time
                                            )
                                            cols.forEach { value ->
                                                Text(
                                                    text = value,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier
                                                        .width(140.dp)
                                                        .padding(horizontal = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ExportRadioOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) HospitalTealPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = HospitalTealPrimary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
