package com.example.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "monthly_absentee_reports",
    indices = [Index(value = ["month", "year"], unique = true)]
)
data class MonthlyAbsenteeReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val month: Int, // 1 to 12
    val year: Int,
    val totalWorkingDays: Int = 24,
    val schoolName: String = "",
    val udiseCode: String = "",
    val district: String = "",
    val block: String = "",
    val totalStaffCount: Int = 0,
    val remarks: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isFinalized: Boolean = false
)

@Entity(
    tableName = "monthly_absentee_details",
    foreignKeys = [
        ForeignKey(
            entity = MonthlyAbsenteeReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["reportId"]), Index(value = ["staffId"])]
)
data class MonthlyAbsenteeDetailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reportId: Long,
    val staffId: Long = 0,
    val teacherName: String,
    val designation: String,
    val employeeId: String = "",
    val basicPay: Double = 35400.0,
    val daPercent: Double = 53.0,
    val hraCategory: String = "8%",
    val joiningDate: String = "",
    val workingDays: Int = 24,
    val presentDays: Int = 24,
    val clDays: Int = 0,
    val elDays: Int = 0,
    val medicalDays: Int = 0,
    val maternityDays: Int = 0,
    val paternityDays: Int = 0,
    val trainingDays: Int = 0,
    val officialDutyDays: Int = 0,
    val electionDutyDays: Int = 0,
    val censusDutyDays: Int = 0,
    val otherLeaveDays: Int = 0,
    val absentDays: Int = 0,
    val remarks: String = ""
) {
    val totalAccountedDays: Int
        get() = presentDays + clDays + elDays + medicalDays + maternityDays +
                paternityDays + trainingDays + officialDutyDays + electionDutyDays +
                censusDutyDays + otherLeaveDays + absentDays

    val isValidAttendance: Boolean
        get() = totalAccountedDays <= workingDays &&
                presentDays >= 0 && clDays >= 0 && elDays >= 0 && medicalDays >= 0 &&
                absentDays >= 0
}
