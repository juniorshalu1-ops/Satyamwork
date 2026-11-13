package com.example.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.math.RoundingMode

@Entity(tableName = "salary_records")
data class SalaryRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val month: Int,
    val year: Int,
    val basicPay: Double,
    val daPercent: Double,
    val daAmount: Double,
    val hraCategory: String,
    val hraAmount: Double,
    val medicalAllowance: Double = 1000.0,
    val otherAllowance: Double = 0.0,
    val grossSalary: Double,
    val pensionType: String = "NPS", // NPS or GPF
    val npsGpfDeduction: Double,
    val gisDeduction: Double = 30.0,
    val incomeTaxDeduction: Double = 0.0,
    val otherDeduction: Double = 0.0,
    val totalDeductions: Double,
    val netSalary: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

object SalaryCalculatorHelper {

    const val SALARY_DISCLAIMER_HI =
        "यह स्व-निर्मित वेतन गणना विवरण है, आधिकारिक Treasury-issued Salary Slip नहीं।"
    const val SALARY_DISCLAIMER_EN =
        "This is a self-generated salary calculation statement, not an official Treasury-issued Salary Slip."

    fun calculate(
        basic: Double,
        daPercent: Double,
        hraCategory: String, // 5%, 7.5%, 10%, 20% (and legacy 4%, 6%, 8%, 16%)
        medical: Double = 1000.0,
        otherAllowance: Double = 0.0,
        isNps: Boolean = true,
        gis: Double = 30.0,
        incomeTax: Double = 0.0,
        otherDeductions: Double = 0.0
    ): SalaryCalculationResult {
        val basicBd = BigDecimal(basic.toString()).setScale(2, RoundingMode.HALF_UP)
        val daRateBd = BigDecimal(daPercent.toString()).divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        val daAmountBd = basicBd.multiply(daRateBd).setScale(2, RoundingMode.HALF_UP)

        val hraPercentValue = when {
            hraCategory.contains("20") -> 0.20
            hraCategory.contains("10") -> 0.10
            hraCategory.contains("7.5") -> 0.075
            hraCategory.contains("5") -> 0.05
            hraCategory.contains("16") -> 0.16
            hraCategory.contains("8") -> 0.08
            hraCategory.contains("6") -> 0.06
            hraCategory.contains("4") -> 0.04
            else -> hraCategory.replace("%", "").trim().toDoubleOrNull()?.let { it / 100.0 } ?: 0.075
        }
        val hraAmountBd = basicBd.multiply(BigDecimal(hraPercentValue.toString())).setScale(2, RoundingMode.HALF_UP)

        val medicalBd = BigDecimal(medical.toString()).setScale(2, RoundingMode.HALF_UP)
        val otherAllowBd = BigDecimal(otherAllowance.toString()).setScale(2, RoundingMode.HALF_UP)

        val grossBd = basicBd.add(daAmountBd).add(hraAmountBd).add(medicalBd).add(otherAllowBd)

        // NPS is 10% of (Basic + DA)
        val npsDeductionBd = if (isNps) {
            basicBd.add(daAmountBd).multiply(BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP)
        } else {
            // GPF minimum typically ~6% of basic or user input
            basicBd.multiply(BigDecimal("0.06")).setScale(2, RoundingMode.HALF_UP)
        }

        val gisBd = BigDecimal(gis.toString()).setScale(2, RoundingMode.HALF_UP)
        val taxBd = BigDecimal(incomeTax.toString()).setScale(2, RoundingMode.HALF_UP)
        val otherDedBd = BigDecimal(otherDeductions.toString()).setScale(2, RoundingMode.HALF_UP)

        val totalDeductionsBd = npsDeductionBd.add(gisBd).add(taxBd).add(otherDedBd)
        val netBd = grossBd.subtract(totalDeductionsBd)

        return SalaryCalculationResult(
            basicPay = basicBd.toDouble(),
            daPercent = daPercent,
            daAmount = daAmountBd.toDouble(),
            hraPercent = hraPercentValue * 100,
            hraAmount = hraAmountBd.toDouble(),
            medicalAllowance = medicalBd.toDouble(),
            otherAllowance = otherAllowBd.toDouble(),
            grossSalary = grossBd.toDouble(),
            isNps = isNps,
            npsGpfDeduction = npsDeductionBd.toDouble(),
            gisDeduction = gisBd.toDouble(),
            incomeTaxDeduction = taxBd.toDouble(),
            otherDeduction = otherDedBd.toDouble(),
            totalDeductions = totalDeductionsBd.toDouble(),
            netSalary = netBd.toDouble()
        )
    }
}

data class SalaryCalculationResult(
    val basicPay: Double,
    val daPercent: Double,
    val daAmount: Double,
    val hraPercent: Double,
    val hraAmount: Double,
    val medicalAllowance: Double,
    val otherAllowance: Double,
    val grossSalary: Double,
    val isNps: Boolean,
    val npsGpfDeduction: Double,
    val gisDeduction: Double,
    val incomeTaxDeduction: Double,
    val otherDeduction: Double,
    val totalDeductions: Double,
    val netSalary: Double
)
