package com.example.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teacher_profile")
data class TeacherProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val name: String = "शिक्षक का नाम",
    val designation: String = "सहायक शिक्षक (वर्ग 1-5)",
    val hrmsId: String = "HRMS-849201",
    val eshikshakoshId: String = "ESK-521948",
    val pranNumber: String = "110058291432",
    val schoolName: String = "राजकीय प्राथमिक विद्यालय",
    val udiseCode: String = "10010100101",
    val district: String = "पटना",
    val block: String = "सदर",
    val panchayat: String = "ग्राम पंचायत",
    val mobileNumber: String = "",
    val email: String = "",
    val basicPay: Double = 35400.0,
    val daPercent: Double = 53.0,
    val hraCategory: String = "7.5%", // 5%, 7.5%, 10%, 20%
    val medicalAllowance: Double = 1000.0,
    val otherAllowance: Double = 0.0,
    val pensionType: String = "NPS", // NPS or GPF
    val npsDeduction: Double = 0.0,
    val gisDeduction: Double = 30.0,
    val incomeTaxMonthly: Double = 0.0,
    val otherDeduction: Double = 0.0,
    val dateOfBirth: String = "15/08/1990",
    val dateOfJoining: String = "01/01/2020",
    val dateOfRetirement: String = "31/08/2050",
    val bankAccountNumber: String = "",
    val ifscCode: String = "",
    val employeeId: String = "HRMS-849201", // Backwards compatibility
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "school_profile")
data class SchoolProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val schoolName: String = "राजकीय उत्क्रमित मध्य विद्यालय",
    val udiseCode: String = "10010100101",
    val district: String = "पटना",
    val block: String = "पटना सदर",
    val panchayat: String = "ग्राम पंचायत",
    val village: String = "गाँव",
    val address: String = "पो० - सदर, जिला - पटना, बिहार - 800001",
    val headmasterName: String = "प्रधानाध्यापक का नाम",
    val headmasterContact: String = "",
    val schoolCategory: String = "मध्य विद्यालय (1-8)",
    val totalStudents: Int = 250,
    val totalSanctionedPosts: Int = 8,
    val totalWorkingTeachers: Int = 6,
    val establishmentYear: String = "1985",
    val lastUpdated: Long = System.currentTimeMillis()
)
