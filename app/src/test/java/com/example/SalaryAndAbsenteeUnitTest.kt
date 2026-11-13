package com.example

import com.example.core.DateUtils
import com.example.core.NumberToWords
import com.example.database.entities.MonthlyAbsenteeDetailEntity
import com.example.database.entities.SalaryCalculatorHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class SalaryAndAbsenteeUnitTest {

    @Test
    fun testSalaryCalculationAccuracy() {
        val basic = 35400.0
        val daPercent = 53.0
        val hraCategory = "8%"

        val result = SalaryCalculatorHelper.calculate(
            basic = basic,
            daPercent = daPercent,
            hraCategory = hraCategory,
            medical = 1000.0,
            isNps = true,
            gis = 30.0
        )

        // Basic: 35400
        // DA (53%): 18762.00
        // HRA (8%): 2832.00
        // Medical: 1000.00
        // Gross: 35400 + 18762 + 2832 + 1000 = 57994.00
        assertEquals(57994.0, result.grossSalary, 0.01)

        // NPS: 10% of (35400 + 18762) = 10% of 54162 = 5416.20
        assertEquals(5416.20, result.npsGpfDeduction, 0.01)

        // Total deductions: 5416.20 + 30 = 5446.20
        assertEquals(5446.20, result.totalDeductions, 0.01)

        // Net: 57994 - 5446.20 = 52547.80
        assertEquals(52547.80, result.netSalary, 0.01)
    }

    @Test
    fun testNumberToWordsConversion() {
        val wordsHi = NumberToWords.toHindiWords(45230)
        assertTrue(wordsHi.contains("पैंतालीस हजार") && wordsHi.contains("रुपये मात्र"))

        val wordsEn = NumberToWords.toEnglishWords(45230)
        assertTrue(wordsEn.contains("Forty Five Thousand") && wordsEn.contains("Rupees Only"))
    }

    @Test
    fun testAbsenteeValidation() {
        val validRecord = MonthlyAbsenteeDetailEntity(
            reportId = 1,
            teacherName = "सत्यम प्रकाश",
            designation = "प्रधानाध्यापक",
            workingDays = 24,
            presentDays = 22,
            clDays = 2,
            absentDays = 0
        )
        assertTrue(validRecord.isValidAttendance)
        assertEquals(24, validRecord.totalAccountedDays)

        val invalidRecord = MonthlyAbsenteeDetailEntity(
            reportId = 1,
            teacherName = "शिक्षक",
            designation = "सहायक शिक्षक",
            workingDays = 24,
            presentDays = 22,
            clDays = 5,
            absentDays = 1
        )
        assertFalse(invalidRecord.isValidAttendance)
        assertEquals(28, invalidRecord.totalAccountedDays)
    }

    @Test
    fun testDateUtilsRetirement() {
        val dob = DateUtils.parseDate("15/08/1990")
        assertTrue(dob != null)
        val retirement = DateUtils.calculateRetirementDate(dob!!)
        val retStr = DateUtils.formatDate(retirement)
        assertEquals("31/08/2050", retStr)
    }
}
