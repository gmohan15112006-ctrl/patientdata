package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.HospitalDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.HospitalSettingEntity
import com.example.data.model.PatientEntity
import com.example.data.model.UserEntity
import com.example.data.repository.HospitalRepository
import com.example.excel.ExcelExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    LOGIN,
    NURSE_REGISTER,
    NURSE_DASHBOARD,
    NURSE_MY_RECORDS,
    NURSE_PROFILE,
    DOCTOR_DASHBOARD,
    ALL_PATIENT_RECORDS,
    PATIENT_FORM,
    USER_MANAGEMENT,
    AUDIT_LOGS,
    ADMIN_SETTINGS,
    EXCEL_EXPORT_DIALOG,
    EXCEL_PREVIEW,
    UNAUTHORIZED,
    ERROR_PAGE
}

enum class PatientFormMode {
    CREATE,
    EDIT,
    COPY
}

data class PatientFilterState(
    val query: String = "",
    val dateFilter: String = "",
    val specialty: String = "All",
    val nurseId: Long? = null,
    val sex: String = "All",
    val ageInterval: String = "All",
    val taeiCategory: String = "All",
    val medicolegal: String = "All",
    val transferredOut: String = "All",
    val showDeletedOnly: Boolean = false
)

class HospitalViewModel(application: Application) : AndroidViewModel(application) {

    val repository: HospitalRepository

    init {
        val db = HospitalDatabase.getDatabase(application)
        repository = HospitalRepository(db)
        // Ensure default admin/nurses/patients exist
        viewModelScope.launch {
            try {
                if (db.userDao().getUserCount() == 0) {
                    HospitalDatabase.seedDatabase(db)
                }
            } catch (e: Exception) {
                // Ignore if already seeded
            }
        }
    }

    // --- Navigation & Role State ---
    private val _currentScreen = MutableStateFlow(AppScreen.LOGIN)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val screenBackStack = mutableListOf<AppScreen>()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Form state
    private val _patientFormMode = MutableStateFlow(PatientFormMode.CREATE)
    val patientFormMode: StateFlow<PatientFormMode> = _patientFormMode.asStateFlow()

    private val _editingPatient = MutableStateFlow<PatientEntity?>(null)
    val editingPatient: StateFlow<PatientEntity?> = _editingPatient.asStateFlow()

    private val _selectedPatientForDetails = MutableStateFlow<PatientEntity?>(null)
    val selectedPatientForDetails: StateFlow<PatientEntity?> = _selectedPatientForDetails.asStateFlow()

    // Filter state
    private val _filterState = MutableStateFlow(PatientFilterState())
    val filterState: StateFlow<PatientFilterState> = _filterState.asStateFlow()

    // Excel Export State
    private val _generatedExcelFile = MutableStateFlow<File?>(null)
    val generatedExcelFile: StateFlow<File?> = _generatedExcelFile.asStateFlow()

    private val _excelStatusMessage = MutableStateFlow<String?>(null)
    val excelStatusMessage: StateFlow<String?> = _excelStatusMessage.asStateFlow()

    // Global Message & Error State
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Data streams
    val allActivePatients: StateFlow<List<PatientEntity>> = repository.getAllActivePatients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPatientsWithDeleted: StateFlow<List<PatientEntity>> = repository.getAllPatientsIncludingDeleted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nurses: StateFlow<List<UserEntity>> = repository.getNurses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.getAllAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clinicalSettings: StateFlow<List<HospitalSettingEntity>> = repository.getAllSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered patient list for Doctor/Admin
    val filteredDoctorPatients: StateFlow<List<PatientEntity>> = combine(
        allPatientsWithDeleted,
        _filterState
    ) { patients, filter ->
        patients.filter { p ->
            val matchesStatus = if (filter.showDeletedOnly) {
                p.status == "DELETED"
            } else {
                p.status == "ACTIVE"
            }

            val matchesQuery = filter.query.isBlank() ||
                    p.name.contains(filter.query, ignoreCase = true) ||
                    p.ip_no.contains(filter.query, ignoreCase = true) ||
                    p.diagnosis.contains(filter.query, ignoreCase = true)

            val matchesDate = filter.dateFilter.isBlank() ||
                    p.admission_datetime.startsWith(filter.dateFilter)

            val matchesSpecialty = filter.specialty == "All" ||
                    p.broad_speciality_category.equals(filter.specialty, ignoreCase = true)

            val matchesNurse = filter.nurseId == null || p.entered_by == filter.nurseId

            val matchesSex = filter.sex == "All" || p.sex.equals(filter.sex, ignoreCase = true)

            val matchesAgeInterval = filter.ageInterval == "All" ||
                    p.age_interval.equals(filter.ageInterval, ignoreCase = true)

            val matchesTaei = filter.taeiCategory == "All" ||
                    p.taei_category.equals(filter.taeiCategory, ignoreCase = true)

            val matchesMlc = filter.medicolegal == "All" ||
                    p.medicolegal_category.equals(filter.medicolegal, ignoreCase = true)

            val matchesTransfer = filter.transferredOut == "All" ||
                    (if (filter.transferredOut == "Yes") p.transferred_out.startsWith("Yes")
                    else p.transferred_out.equals("No", ignoreCase = true))

            matchesStatus && matchesQuery && matchesDate && matchesSpecialty &&
                    matchesNurse && matchesSex && matchesAgeInterval && matchesTaei &&
                    matchesMlc && matchesTransfer
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Nurse's own filtered patient records
    val nurseFilteredPatients: StateFlow<List<PatientEntity>> = combine(
        allActivePatients,
        _currentUser,
        _filterState
    ) { patients, user, filter ->
        if (user == null || user.role != "NURSE") return@combine emptyList()
        patients.filter { p ->
            p.entered_by == user.id &&
                    (filter.query.isBlank() ||
                            p.name.contains(filter.query, ignoreCase = true) ||
                            p.ip_no.contains(filter.query, ignoreCase = true) ||
                            p.diagnosis.contains(filter.query, ignoreCase = true)) &&
                    (filter.dateFilter.isBlank() || p.admission_datetime.startsWith(filter.dateFilter)) &&
                    (filter.specialty == "All" || p.broad_speciality_category.equals(filter.specialty, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Navigation Functions ---

    fun navigateTo(screen: AppScreen) {
        val current = _currentScreen.value
        // Role based route protection
        val user = _currentUser.value
        if (isDoctorScreen(screen) && user?.role != "DOCTOR") {
            _currentScreen.value = AppScreen.UNAUTHORIZED
            return
        }
        if (isNurseScreen(screen) && user?.role != "NURSE") {
            _currentScreen.value = AppScreen.UNAUTHORIZED
            return
        }

        if (current != screen) {
            screenBackStack.add(current)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (screenBackStack.isNotEmpty()) {
            val prev = screenBackStack.removeAt(screenBackStack.size - 1)
            _currentScreen.value = prev
            return true
        }
        return false
    }

    private fun isDoctorScreen(screen: AppScreen): Boolean {
        return screen in listOf(
            AppScreen.DOCTOR_DASHBOARD,
            AppScreen.ALL_PATIENT_RECORDS,
            AppScreen.USER_MANAGEMENT,
            AppScreen.AUDIT_LOGS,
            AppScreen.ADMIN_SETTINGS,
            AppScreen.EXCEL_EXPORT_DIALOG
        )
    }

    private fun isNurseScreen(screen: AppScreen): Boolean {
        return screen in listOf(
            AppScreen.NURSE_DASHBOARD,
            AppScreen.NURSE_MY_RECORDS,
            AppScreen.NURSE_PROFILE
        )
    }

    // --- Authentication Actions ---

    fun login(email: String, passwordPlain: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            repository.authenticate(email, passwordPlain)
                .onSuccess { user ->
                    _currentUser.value = user
                    screenBackStack.clear()
                    if (user.role == "DOCTOR") {
                        _currentScreen.value = AppScreen.DOCTOR_DASHBOARD
                    } else {
                        _currentScreen.value = AppScreen.NURSE_DASHBOARD
                    }
                    onResult(true, "Welcome back, ${user.full_name}")
                }
                .onFailure { exception ->
                    onResult(false, exception.message ?: "Invalid email or password.")
                }
        }
    }

    fun registerNurse(
        fullName: String,
        employeeId: String,
        email: String,
        mobile: String,
        department: String,
        passwordPlain: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            repository.registerNurse(
                fullName = fullName,
                employeeId = employeeId,
                email = email,
                mobile = mobile,
                department = department,
                passwordPlain = passwordPlain
            )
                .onSuccess {
                    onResult(true, "Registration successful! Please login.")
                    _currentScreen.value = AppScreen.LOGIN
                }
                .onFailure { exception ->
                    onResult(false, exception.message ?: "Registration failed.")
                }
        }
    }

    fun logout() {
        val user = _currentUser.value
        _currentUser.value = null
        screenBackStack.clear()
        _currentScreen.value = AppScreen.LOGIN
        _toastMessage.value = "Logged out safely."
    }

    fun updateUserProfile(user: UserEntity, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            repository.updateUserProfile(user)
                .onSuccess {
                    _currentUser.value = user
                    onResult(true, "Profile updated successfully.")
                }
                .onFailure {
                    onResult(false, it.message ?: "Failed to update profile.")
                }
        }
    }

    // --- Patient Operations ---

    fun prepareNewPatientForm() {
        _patientFormMode.value = PatientFormMode.CREATE
        _editingPatient.value = null
        navigateTo(AppScreen.PATIENT_FORM)
    }

    fun prepareEditPatient(patient: PatientEntity) {
        val user = _currentUser.value
        if (user?.role != "DOCTOR" && patient.entered_by != user?.id) {
            _currentScreen.value = AppScreen.UNAUTHORIZED
            return
        }
        _patientFormMode.value = PatientFormMode.EDIT
        _editingPatient.value = patient
        navigateTo(AppScreen.PATIENT_FORM)
    }

    fun prepareCopyPatient(patient: PatientEntity) {
        val user = _currentUser.value
        if (user?.role != "DOCTOR") {
            _currentScreen.value = AppScreen.UNAUTHORIZED
            return
        }
        // Copy as a new unsaved record
        _patientFormMode.value = PatientFormMode.COPY
        _editingPatient.value = patient.copy(id = 0, serial_no = 0)
        navigateTo(AppScreen.PATIENT_FORM)
    }

    fun viewPatientDetails(patient: PatientEntity) {
        _selectedPatientForDetails.value = patient
    }

    fun closePatientDetails() {
        _selectedPatientForDetails.value = null
    }

    fun savePatient(
        patient: PatientEntity,
        isCopyWarningAcknowledged: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        val user = _currentUser.value ?: run {
            onResult(false, "User session expired.")
            return
        }

        viewModelScope.launch {
            when (_patientFormMode.value) {
                PatientFormMode.CREATE -> {
                    // Check duplicate IP
                    if (repository.checkIpExists(patient.ip_no)) {
                        onResult(false, "Patient with In-Patient Number '${patient.ip_no}' already exists in active records.")
                        return@launch
                    }
                    repository.saveNewPatient(patient, user)
                        .onSuccess {
                            _toastMessage.value = "Patient record saved successfully."
                            onResult(true, "Patient record saved successfully.")
                        }
                        .onFailure {
                            onResult(false, it.message ?: "Failed to save record.")
                        }
                }
                PatientFormMode.EDIT -> {
                    repository.updatePatient(patient, user)
                        .onSuccess {
                            _toastMessage.value = "Patient record updated successfully."
                            onResult(true, "Patient record updated successfully.")
                        }
                        .onFailure {
                            onResult(false, it.message ?: "Failed to update record.")
                        }
                }
                PatientFormMode.COPY -> {
                    if (!isCopyWarningAcknowledged && repository.checkIpExists(patient.ip_no)) {
                        onResult(false, "WARNING_DUPLICATE_IP: IP No '${patient.ip_no}' already exists. Please modify or confirm intentional duplicate.")
                        return@launch
                    }
                    repository.saveNewPatient(patient, user)
                        .onSuccess {
                            _toastMessage.value = "Copied patient record saved successfully."
                            onResult(true, "Copied patient record saved successfully.")
                        }
                        .onFailure {
                            onResult(false, it.message ?: "Failed to save copy.")
                        }
                }
            }
        }
    }

    fun deletePatient(patient: PatientEntity, onResult: (Boolean, String) -> Unit) {
        val user = _currentUser.value ?: run {
            onResult(false, "Session expired.")
            return
        }
        if (user.role != "DOCTOR") {
            onResult(false, "Unauthorized: Only Doctor/Admin can delete records.")
            return
        }

        viewModelScope.launch {
            repository.softDeletePatient(patient.id, user)
                .onSuccess {
                    _toastMessage.value = "Patient record soft-deleted."
                    onResult(true, "Patient record soft-deleted.")
                }
                .onFailure {
                    onResult(false, it.message ?: "Failed to delete record.")
                }
        }
    }

    // --- Filter Management ---

    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(query = query)
    }

    fun updateDateFilter(date: String) {
        _filterState.value = _filterState.value.copy(dateFilter = date)
    }

    fun updateSpecialtyFilter(specialty: String) {
        _filterState.value = _filterState.value.copy(specialty = specialty)
    }

    fun updateNurseFilter(nurseId: Long?) {
        _filterState.value = _filterState.value.copy(nurseId = nurseId)
    }

    fun updateSexFilter(sex: String) {
        _filterState.value = _filterState.value.copy(sex = sex)
    }

    fun updateAgeIntervalFilter(interval: String) {
        _filterState.value = _filterState.value.copy(ageInterval = interval)
    }

    fun updateTaeiFilter(taei: String) {
        _filterState.value = _filterState.value.copy(taeiCategory = taei)
    }

    fun updateMedicolegalFilter(mlc: String) {
        _filterState.value = _filterState.value.copy(medicolegal = mlc)
    }

    fun updateTransferredOutFilter(trans: String) {
        _filterState.value = _filterState.value.copy(transferredOut = trans)
    }

    fun toggleShowDeletedOnly() {
        _filterState.value = _filterState.value.copy(showDeletedOnly = !_filterState.value.showDeletedOnly)
    }

    fun clearFilters() {
        _filterState.value = PatientFilterState()
    }

    // --- Excel Export ---

    fun exportToExcel(
        fileName: String,
        exportOption: String, // "ALL", "FILTERED", "MULTI_DATE"
        onComplete: (File) -> Unit
    ) {
        val user = _currentUser.value ?: return
        if (user.role != "DOCTOR") {
            _currentScreen.value = AppScreen.UNAUTHORIZED
            return
        }

        val context = getApplication<Application>()
        viewModelScope.launch {
            val patientsToExport = when (exportOption) {
                "FILTERED" -> filteredDoctorPatients.value
                else -> allActivePatients.value
            }

            val sheets = if (exportOption == "MULTI_DATE") {
                // Group patients by admission date (e.g., 2026-08-11 -> "AUG 11")
                val grouped = patientsToExport.groupBy { p ->
                    try {
                        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(p.admission_datetime.take(10))
                        SimpleDateFormat("MMM dd", Locale.getDefault()).format(parsed ?: Date()).uppercase()
                    } catch (e: Exception) {
                        p.admission_datetime.take(10)
                    }
                }
                if (grouped.isEmpty()) {
                    listOf(ExcelExporter.ExportSheet("All Patients", patientsToExport))
                } else {
                    grouped.map { (dateLabel, list) ->
                        ExcelExporter.ExportSheet(dateLabel, list)
                    }
                }
            } else {
                listOf(ExcelExporter.ExportSheet("Patient Records", patientsToExport))
            }

            val finalFile = ExcelExporter.generateExcelFile(context, fileName, sheets)
            _generatedExcelFile.value = finalFile

            repository.recordExcelExportAudit(user, finalFile.name, patientsToExport.size)
            _toastMessage.value = "Excel report generated: ${finalFile.name}"
            onComplete(finalFile)
        }
    }

    // --- User Management ---

    fun toggleUserStatus(targetUser: UserEntity) {
        val admin = _currentUser.value ?: return
        val newStatus = if (targetUser.status == "ACTIVE") "INACTIVE" else "ACTIVE"
        viewModelScope.launch {
            repository.updateUserStatus(admin, targetUser.id, newStatus)
                .onSuccess {
                    _toastMessage.value = "User status updated to $newStatus."
                }
                .onFailure {
                    _errorMessage.value = it.message ?: "Failed to update user status."
                }
        }
    }

    // --- Admin Clinical Settings ---

    fun addClinicalSetting(category: String, value: String) {
        val admin = _currentUser.value ?: return
        val key = "${category.lowercase()}_${System.currentTimeMillis()}"
        viewModelScope.launch {
            repository.saveSetting(admin, HospitalSettingEntity(key, category, value.trim()))
                .onSuccess {
                    _toastMessage.value = "Added $value to $category."
                }
                .onFailure {
                    _errorMessage.value = it.message ?: "Failed to save category."
                }
        }
    }

    fun deleteClinicalSetting(key: String) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.deleteSetting(admin, key)
                .onSuccess {
                    _toastMessage.value = "Option removed."
                }
                .onFailure {
                    _errorMessage.value = it.message ?: "Failed to delete option."
                }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Utility: Auto calculate age interval
    fun suggestAgeInterval(age: Int): String {
        return when {
            age < 12 -> "Below 12"
            age in 12..60 -> "12-60"
            else -> "Above 60"
        }
    }
}
