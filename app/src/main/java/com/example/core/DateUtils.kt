package com.example.core

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val indianDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
    private val displayDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("hi", "IN"))
    private val displayDateEnglish = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("hi", "IN"))

    fun getCurrentDateFormatted(isHindi: Boolean = true): String {
        return if (isHindi) {
            displayDateFormat.format(Date())
        } else {
            displayDateEnglish.format(Date())
        }
    }

    fun parseDate(dateStr: String): Date? {
        return try {
            indianDateFormat.parse(dateStr.trim())
        } catch (e: Exception) {
            null
        }
    }

    fun formatDate(date: Date): String {
        return indianDateFormat.format(date)
    }

    data class DateDiffResult(
        val years: Int,
        val months: Int,
        val days: Int,
        val totalDays: Long
    )

    fun calculateDifference(startDate: Date, endDate: Date): DateDiffResult {
        val startCal = Calendar.getInstance().apply { time = startDate }
        val endCal = Calendar.getInstance().apply { time = endDate }

        if (endCal.before(startCal)) {
            return DateDiffResult(0, 0, 0, 0)
        }

        val totalDays = (endDate.time - startDate.time) / (1000 * 60 * 60 * 24)

        var years = endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)
        var months = endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH)
        var days = endCal.get(Calendar.DAY_OF_MONTH) - startCal.get(Calendar.DAY_OF_MONTH)

        if (days < 0) {
            months -= 1
            val tempCal = Calendar.getInstance().apply {
                time = endCal.time
                add(Calendar.MONTH, -1)
            }
            days += tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }

        if (months < 0) {
            years -= 1
            months += 12
        }

        return DateDiffResult(
            years = maxOf(0, years),
            months = maxOf(0, months),
            days = maxOf(0, days),
            totalDays = maxOf(0L, totalDays)
        )
    }

    /**
     * Bihar Service Rules: Retirement is at age 60, effective on the last day of the month
     * in which the employee attains age 60. (If born on 1st of month, on the previous month's last day).
     */
    fun calculateRetirementDate(dob: Date): Date {
        val cal = Calendar.getInstance().apply {
            time = dob
            add(Calendar.YEAR, 60)
        }
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        if (dayOfMonth == 1) {
            cal.add(Calendar.MONTH, -1)
        }
        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, lastDay)
        return cal.time
    }

    /**
     * Bihar Government Annual Increment is generally on 1st January or 1st July
     * depending on joining / completion of 6 months.
     */
    fun getNextIncrementDate(currentDate: Date = Date()): String {
        val cal = Calendar.getInstance().apply { time = currentDate }
        val month = cal.get(Calendar.MONTH) // 0-based
        val year = cal.get(Calendar.YEAR)

        return if (month < Calendar.JULY) {
            "01/07/$year"
        } else {
            "01/01/${year + 1}"
        }
    }

    fun getMonthName(monthNumber: Int, isHindi: Boolean = true): String {
        val hindiMonths = arrayOf(
            "जनवरी", "फरवरी", "मार्च", "अप्रैल", "मई", "जून",
            "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर"
        )
        val englishMonths = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val index = (monthNumber - 1).coerceIn(0, 11)
        return if (isHindi) hindiMonths[index] else englishMonths[index]
    }
}
