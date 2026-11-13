package com.example.salary

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.DateUtils
import com.example.core.SalaryPdfGenerator
import com.example.database.AppDatabase
import com.example.database.AppRepository
import com.example.database.entities.SalaryCalculationResult
import com.example.database.entities.SalaryCalculatorHelper
import com.example.database.entities.SalaryRecordEntity
import com.example.database.entities.TeacherProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class SalaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository
    val salaryHistory: StateFlow<List<SalaryRecordEntity>>
    val teacherProfile: StateFlow<TeacherProfileEntity?>

    // Salary Calculator Inputs
    val basicPayInput = MutableStateFlow("35400")
    val daPercentInput = MutableStateFlow("53") // Bihar current DA
    val hraCategoryInput = MutableStateFlow("8%") // 4%, 6%, 8%, 16%
    val medicalAllowanceInput = MutableStateFlow("1000")
    val otherAllowanceInput = MutableStateFlow("0")
    val isNpsInput = MutableStateFlow(true)
    val gisInput = MutableStateFlow("30")
    val incomeTaxInput = MutableStateFlow("0")
    val otherDeductionInput = MutableStateFlow("0")

    private val _calculationResult = MutableStateFlow(
        SalaryCalculatorHelper.calculate(
            basic = 35400.0,
            daPercent = 53.0,
            hraCategory = "8%"
        )
    )
    val calculationResult: StateFlow<SalaryCalculationResult> = _calculationResult.asStateFlow()

    // Additional Calculators
    // 1. DA Arrear
    val arrearBasicInput = MutableStateFlow("35400")
    val arrearOldDaInput = MutableStateFlow("50")
    val arrearNewDaInput = MutableStateFlow("53")
    val arrearMonthsInput = MutableStateFlow("3")
    val arrearTotalResult = MutableStateFlow(0.0)

    // 2. Increment Calculator
    val incCurrentBasicInput = MutableStateFlow("35400")
    val incResultNewBasic = MutableStateFlow(36500.0)

    // 3. Pay Revision (Fitment Factor 2.57)
    val preRevisionBasicInput = MutableStateFlow("12000")
    val revisedBasicResult = MutableStateFlow(30840.0)

    // 4. Future Salary Estimator
    val futureYearsInput = MutableStateFlow("3")
    val futureEstimatedNet = MutableStateFlow(0.0)

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = AppRepository(db)

        salaryHistory = repository.allSalaryRecords
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        teacherProfile = repository.teacherProfile
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        viewModelScope.launch {
            repository.teacherProfile.collect { teacher ->
                teacher?.let {
                    basicPayInput.value = it.basicPay.toInt().toString()
                    daPercentInput.value = it.daPercent.toInt().toString()
                    hraCategoryInput.value = it.hraCategory
                    medicalAllowanceInput.value = it.medicalAllowance.toInt().toString()
                    isNpsInput.value = it.pensionType.equals("NPS", ignoreCase = true)
                    recalculate()
                }
            }
        }
    }

    fun recalculate() {
        val basic = basicPayInput.value.toDoubleOrNull() ?: 0.0
        val da = daPercentInput.value.toDoubleOrNull() ?: 53.0
        val hraCat = hraCategoryInput.value
        val med = medicalAllowanceInput.value.toDoubleOrNull() ?: 1000.0
        val other = otherAllowanceInput.value.toDoubleOrNull() ?: 0.0
        val isNps = isNpsInput.value
        val gis = gisInput.value.toDoubleOrNull() ?: 30.0
        val tax = incomeTaxInput.value.toDoubleOrNull() ?: 0.0
        val otherDed = otherDeductionInput.value.toDoubleOrNull() ?: 0.0

        val result = SalaryCalculatorHelper.calculate(
            basic = basic,
            daPercent = da,
            hraCategory = hraCat,
            medical = med,
            otherAllowance = other,
            isNps = isNps,
            gis = gis,
            incomeTax = tax,
            otherDeductions = otherDed
        )
        _calculationResult.value = result
    }

    fun calculateDaArrear() {
        val basic = arrearBasicInput.value.toDoubleOrNull() ?: 0.0
        val oldDa = arrearOldDaInput.value.toDoubleOrNull() ?: 0.0
        val newDa = arrearNewDaInput.value.toDoubleOrNull() ?: 0.0
        val months = arrearMonthsInput.value.toIntOrNull() ?: 1

        val diffPercent = (newDa - oldDa).coerceAtLeast(0.0)
        val monthlyDiff = basic * (diffPercent / 100.0)
        arrearTotalResult.value = monthlyDiff * months
    }

    fun calculateIncrement() {
        val basic = incCurrentBasicInput.value.toDoubleOrNull() ?: 0.0
        // Standard 3% increment rounded up to next 100
        val incAmount = basic * 0.03
        val rawNext = basic + incAmount
        // Round to nearest 100 as per Pay Matrix convention
        val rounded = Math.round(rawNext / 100.0) * 100.0
        incResultNewBasic.value = rounded
    }

    fun calculatePayRevision() {
        val basic = preRevisionBasicInput.value.toDoubleOrNull() ?: 0.0
        val fitment = 2.57
        val revised = basic * fitment
        revisedBasicResult.value = Math.round(revised / 100.0) * 100.0
    }

    fun calculateFutureSalary() {
        val years = futureYearsInput.value.toIntOrNull() ?: 1
        var basic = basicPayInput.value.toDoubleOrNull() ?: 35400.0
        var da = daPercentInput.value.toDoubleOrNull() ?: 53.0

        for (i in 1..years) {
            basic = Math.round((basic * 1.03) / 100.0) * 100.0
            da += 6.0 // Approximate 3% twice a year = 6% per year
        }

        val estimated = SalaryCalculatorHelper.calculate(
            basic = basic,
            daPercent = da,
            hraCategory = hraCategoryInput.value,
            medical = medicalAllowanceInput.value.toDoubleOrNull() ?: 1000.0,
            isNps = isNpsInput.value
        )
        futureEstimatedNet.value = estimated.netSalary
    }

    fun saveCurrentCalculation(note: String = "") {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val month = cal.get(Calendar.MONTH) + 1
            val year = cal.get(Calendar.YEAR)
            val res = _calculationResult.value

            val record = SalaryRecordEntity(
                month = month,
                year = year,
                basicPay = res.basicPay,
                daPercent = res.daPercent,
                daAmount = res.daAmount,
                hraCategory = hraCategoryInput.value,
                hraAmount = res.hraAmount,
                medicalAllowance = res.medicalAllowance,
                otherAllowance = res.otherAllowance,
                grossSalary = res.grossSalary,
                pensionType = if (res.isNps) "NPS" else "GPF",
                npsGpfDeduction = res.npsGpfDeduction,
                gisDeduction = res.gisDeduction,
                incomeTaxDeduction = res.incomeTaxDeduction,
                otherDeduction = res.otherDeduction,
                totalDeductions = res.totalDeductions,
                netSalary = res.netSalary,
                note = note
            )
            repository.saveSalaryRecord(record)
        }
    }

    fun deleteRecord(record: SalaryRecordEntity) {
        viewModelScope.launch {
            repository.deleteSalaryRecord(record)
        }
    }

    fun exportSalaryPdf(context: Context): File {
        val teacher = teacherProfile.value ?: TeacherProfileEntity()
        val cal = Calendar.getInstance()
        val monthName = DateUtils.getMonthName(cal.get(Calendar.MONTH) + 1, isHindi = true)
        val year = cal.get(Calendar.YEAR)
        return SalaryPdfGenerator.generatePdf(
            context = context,
            teacher = teacher,
            salary = _calculationResult.value,
            monthName = monthName,
            year = year
        )
    }
}
