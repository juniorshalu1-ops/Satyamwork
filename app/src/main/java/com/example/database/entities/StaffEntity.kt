package com.example.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "staff_members",
    indices = [Index(value = ["employeeId"]), Index(value = ["name"])]
)
data class StaffEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val designation: String, // e.g. "प्रधानाध्यापक", "सहायक शिक्षक", "शारीरिक शिक्षक", "टोला सेवक"
    val employeeId: String = "",
    val joiningDate: String = "01/01/2020",
    val basicPay: Double = 35400.0,
    val daPercent: Double = 53.0,
    val hraCategory: String = "8%",
    val contactNumber: String = "",
    val email: String = "",
    val pranNumber: String = "",
    val isActive: Boolean = true,
    val remarks: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
