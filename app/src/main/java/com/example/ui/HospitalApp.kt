package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.NurseRegisterScreen
import com.example.ui.common.GeneralErrorScreen
import com.example.ui.common.NotFoundScreen
import com.example.ui.common.UnauthorizedScreen
import com.example.ui.doctor.AdminSettingsScreen
import com.example.ui.doctor.AllPatientsScreen
import com.example.ui.doctor.AuditLogsScreen
import com.example.ui.doctor.DoctorDashboardScreen
import com.example.ui.doctor.ExcelExportScreen
import com.example.ui.doctor.PatientDetailsDialog
import com.example.ui.doctor.UserManagementScreen
import com.example.ui.nurse.NurseDashboardScreen
import com.example.ui.nurse.NurseProfileScreen
import com.example.ui.nurse.NurseRecordsScreen
import com.example.ui.patient.PatientFormScreen
import com.example.viewmodel.AppScreen
import com.example.viewmodel.HospitalViewModel

@Composable
fun HospitalApp(viewModel: HospitalViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedPatientForDetails by viewModel.selectedPatientForDetails.collectAsState()
    val generatedExcelFile by viewModel.generatedExcelFile.collectAsState()

    // Data streams
    val allActivePatients by viewModel.allActivePatients.collectAsState()
    val filteredDoctorPatients by viewModel.filteredDoctorPatients.collectAsState()
    val nurseFilteredPatients by viewModel.nurseFilteredPatients.collectAsState()
    val nurses by viewModel.nurses.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val clinicalSettings by viewModel.clinicalSettings.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val patientFormMode by viewModel.patientFormMode.collectAsState()
    val editingPatient by viewModel.editingPatient.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // BackHandler: pop screen or exit
    BackHandler(enabled = currentScreen != AppScreen.LOGIN) {
        val handled = viewModel.navigateBack()
        if (!handled) {
            // If on main dashboards, return to login or stay
            if (currentUser != null) {
                // Logout prompt or stay on dashboard
            }
        }
    }

    // Modal Details Dialog
    selectedPatientForDetails?.let { patient ->
        PatientDetailsDialog(
            patient = patient,
            onDismiss = { viewModel.closePatientDetails() }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            AppScreen.LOGIN -> {
                LoginScreen(
                    onLoginClick = { email, password, onResult ->
                        viewModel.login(email, password, onResult)
                    },
                    onNavigateToRegister = {
                        viewModel.navigateTo(AppScreen.NURSE_REGISTER)
                    }
                )
            }

            AppScreen.NURSE_REGISTER -> {
                NurseRegisterScreen(
                    onRegisterClick = { name, empId, email, mobile, dept, pwd, onResult ->
                        viewModel.registerNurse(name, empId, email, mobile, dept, pwd, onResult)
                    },
                    onBackToLogin = {
                        viewModel.navigateTo(AppScreen.LOGIN)
                    }
                )
            }

            AppScreen.NURSE_DASHBOARD -> {
                val user = currentUser
                if (user != null && user.role == "NURSE") {
                    NurseDashboardScreen(
                        currentUser = user,
                        myPatients = nurseFilteredPatients,
                        onAddNewPatient = { viewModel.prepareNewPatientForm() },
                        onViewMyRecords = { viewModel.navigateTo(AppScreen.NURSE_MY_RECORDS) },
                        onSearchMyRecords = { viewModel.navigateTo(AppScreen.NURSE_MY_RECORDS) },
                        onViewProfile = { viewModel.navigateTo(AppScreen.NURSE_PROFILE) },
                        onViewPatientDetails = { viewModel.viewPatientDetails(it) },
                        onLogout = { viewModel.logout() }
                    )
                } else {
                    viewModel.navigateTo(AppScreen.LOGIN)
                }
            }

            AppScreen.NURSE_MY_RECORDS -> {
                val user = currentUser
                if (user != null && user.role == "NURSE") {
                    NurseRecordsScreen(
                        currentUser = user,
                        patients = nurseFilteredPatients,
                        searchQuery = filterState.query,
                        onSearchChange = { viewModel.updateSearchQuery(it) },
                        dateFilter = filterState.dateFilter,
                        onDateFilterChange = { viewModel.updateDateFilter(it) },
                        onAddNewPatient = { viewModel.prepareNewPatientForm() },
                        onViewPatient = { viewModel.viewPatientDetails(it) },
                        onEditPatient = { viewModel.prepareEditPatient(it) },
                        onBack = { viewModel.navigateBack() }
                    )
                } else {
                    viewModel.navigateTo(AppScreen.LOGIN)
                }
            }

            AppScreen.NURSE_PROFILE -> {
                val user = currentUser
                if (user != null) {
                    NurseProfileScreen(
                        currentUser = user,
                        onSaveProfile = { updated, onResult ->
                            viewModel.updateUserProfile(updated, onResult)
                        },
                        onBack = { viewModel.navigateBack() }
                    )
                } else {
                    viewModel.navigateTo(AppScreen.LOGIN)
                }
            }

            AppScreen.DOCTOR_DASHBOARD -> {
                val user = currentUser
                if (user != null && user.role == "DOCTOR") {
                    DoctorDashboardScreen(
                        currentUser = user,
                        allPatients = allActivePatients,
                        nurses = nurses,
                        onViewAllPatients = { viewModel.navigateTo(AppScreen.ALL_PATIENT_RECORDS) },
                        onAddNewPatient = { viewModel.prepareNewPatientForm() },
                        onOpenExportDialog = { viewModel.navigateTo(AppScreen.EXCEL_EXPORT_DIALOG) },
                        onViewUserManagement = { viewModel.navigateTo(AppScreen.USER_MANAGEMENT) },
                        onViewAuditLogs = { viewModel.navigateTo(AppScreen.AUDIT_LOGS) },
                        onViewSettings = { viewModel.navigateTo(AppScreen.ADMIN_SETTINGS) },
                        onViewPatientDetails = { viewModel.viewPatientDetails(it) },
                        onSearchSubmit = { viewModel.updateSearchQuery(it) },
                        onLogout = { viewModel.logout() }
                    )
                } else {
                    viewModel.navigateTo(AppScreen.UNAUTHORIZED)
                }
            }

            AppScreen.ALL_PATIENT_RECORDS -> {
                val user = currentUser
                if (user != null && user.role == "DOCTOR") {
                    AllPatientsScreen(
                        patients = filteredDoctorPatients,
                        nurses = nurses,
                        filterState = filterState,
                        onUpdateSearchQuery = { viewModel.updateSearchQuery(it) },
                        onUpdateDateFilter = { viewModel.updateDateFilter(it) },
                        onUpdateSpecialtyFilter = { viewModel.updateSpecialtyFilter(it) },
                        onUpdateNurseFilter = { viewModel.updateNurseFilter(it) },
                        onUpdateSexFilter = { viewModel.updateSexFilter(it) },
                        onUpdateAgeIntervalFilter = { viewModel.updateAgeIntervalFilter(it) },
                        onUpdateTaeiFilter = { viewModel.updateTaeiFilter(it) },
                        onUpdateMedicolegalFilter = { viewModel.updateMedicolegalFilter(it) },
                        onUpdateTransferredOutFilter = { viewModel.updateTransferredOutFilter(it) },
                        onToggleShowDeleted = { viewModel.toggleShowDeletedOnly() },
                        onClearFilters = { viewModel.clearFilters() },
                        onAddNewPatient = { viewModel.prepareNewPatientForm() },
                        onViewPatient = { viewModel.viewPatientDetails(it) },
                        onEditPatient = { viewModel.prepareEditPatient(it) },
                        onCopyPatient = { viewModel.prepareCopyPatient(it) },
                        onDeletePatient = { patient ->
                            viewModel.deletePatient(patient) { _, _ -> }
                        },
                        onOpenExportDialog = { viewModel.navigateTo(AppScreen.EXCEL_EXPORT_DIALOG) },
                        onBack = { viewModel.navigateBack() }
                    )
                } else {
                    viewModel.navigateTo(AppScreen.UNAUTHORIZED)
                }
            }

            AppScreen.PATIENT_FORM -> {
                val user = currentUser
                if (user != null) {
                    PatientFormScreen(
                        currentUser = user,
                        formMode = patientFormMode,
                        initialPatient = editingPatient,
                        settings = clinicalSettings,
                        onSavePatient = { patient, forceDuplicate, onResult ->
                            viewModel.savePatient(patient, forceDuplicate, onResult)
                        },
                        onNavigateBack = { viewModel.navigateBack() }
                    )
                } else {
                    viewModel.navigateTo(AppScreen.LOGIN)
                }
            }

            AppScreen.USER_MANAGEMENT -> {
                val user = currentUser
                if (user != null && user.role == "DOCTOR") {
                    UserManagementScreen(
                        nurses = nurses,
                        allPatients = allActivePatients,
                        onToggleStatus = { viewModel.toggleUserStatus(it) },
                        onBack = { viewModel.navigateBack() }
                    )
                } else {
                    viewModel.navigateTo(AppScreen.UNAUTHORIZED)
                }
            }

            AppScreen.AUDIT_LOGS -> {
                val user = currentUser
                if (user != null && user.role == "DOCTOR") {
                    AuditLogsScreen(
                        auditLogs = auditLogs,
                        onBack = { viewModel.navigateBack() }
                    )
                } else {
                    viewModel.navigateTo(AppScreen.UNAUTHORIZED)
                }
            }

            AppScreen.ADMIN_SETTINGS -> {
                val user = currentUser
                if (user != null && user.role == "DOCTOR") {
                    AdminSettingsScreen(
                        settings = clinicalSettings,
                        onAddSetting = { cat, value -> viewModel.addClinicalSetting(cat, value) },
                        onDeleteSetting = { key -> viewModel.deleteClinicalSetting(key) },
                        onBack = { viewModel.navigateBack() }
                    )
                } else {
                    viewModel.navigateTo(AppScreen.UNAUTHORIZED)
                }
            }

            AppScreen.EXCEL_EXPORT_DIALOG, AppScreen.EXCEL_PREVIEW -> {
                val user = currentUser
                if (user != null && user.role == "DOCTOR") {
                    ExcelExportScreen(
                        filteredPatients = filteredDoctorPatients,
                        allPatients = allActivePatients,
                        generatedFile = generatedExcelFile,
                        onExportToExcel = { name, option, onComplete ->
                            viewModel.exportToExcel(name, option, onComplete)
                        },
                        onBack = { viewModel.navigateBack() }
                    )
                } else {
                    viewModel.navigateTo(AppScreen.UNAUTHORIZED)
                }
            }

            AppScreen.UNAUTHORIZED -> {
                UnauthorizedScreen(
                    onReturnHome = {
                        val user = currentUser
                        if (user?.role == "DOCTOR") {
                            viewModel.navigateTo(AppScreen.DOCTOR_DASHBOARD)
                        } else if (user?.role == "NURSE") {
                            viewModel.navigateTo(AppScreen.NURSE_DASHBOARD)
                        } else {
                            viewModel.navigateTo(AppScreen.LOGIN)
                        }
                    }
                )
            }

            AppScreen.ERROR_PAGE -> {
                GeneralErrorScreen(
                    errorMessage = errorMessage ?: "An unexpected error occurred in the hospital subsystem.",
                    onRetry = { viewModel.navigateTo(AppScreen.LOGIN) }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
