package com.example.core

import java.math.BigDecimal

object NumberToWords {

    private val hindiUnits = arrayOf(
        "", "एक", "दो", "तीन", "चार", "पाँच", "छह", "सात", "आठ", "नौ", "दस",
        "ग्यारह", "बारह", "तेरह", "चौदह", "पंद्रह", "सोलह", "सत्रह", "अठारह", "उन्नीस", "बीस",
        "इक्कीस", "बाईस", "तेईस", "चौबीस", "पच्चीस", "छब्बीस", "सत्ताईस", "अट्ठाइस", "उनतीस", "तीस",
        "इकतीस", "बत्तीस", "तैंतीस", "चौंतीस", "पैंतीस", "छत्तीस", "सैंतीस", "अड़तीस", "उनतालीस", "चालीस",
        "इकतालीस", "बयालीस", "तैंतालीस", "चौवालीस", "पैंतालीस", "छियालीस", "सैंतालीस", "अड़तालीस", "उनचास", "पचास",
        "इक्यावन", "बावन", "तिरेपन", "चौवन", "पचपन", "छप्पन", "सत्तावन", "अट्ठावन", "उनसठ", "साठ",
        "इकसठ", "बासठ", "तिरेसठ", "चौंसठ", "पैंसठ", "छियासठ", "सरसठ", "अड़सठ", "उनहत्तर", "सत्तर",
        "इकहत्तर", "बहत्तर", "तिहत्तर", "चौहत्तर", "पचहत्तर", "छिहत्तर", "सतहत्तर", "अठहत्तर", "उन्नासी", "अस्सी",
        "इक्यासी", "बयासी", "तिरासी", "चौरासी", "पचासी", "छियासी", "सत्तासी", "अट्ठासी", "नवासी", "नब्बे",
        "इक्यानवे", "बानवे", "तिरानवे", "चौरानवे", "पंचानवे", "छियानवे", "सत्तानवे", "अट्ठानवे", "निन्यानवे"
    )

    private val englishUnits = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    )

    private val englishTens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    fun toHindiWords(amount: Long): String {
        if (amount == 0L) return "शून्य रुपये मात्र"
        if (amount < 0L) return "ऋणात्मक " + toHindiWords(-amount)

        var n = amount
        val sb = StringBuilder()

        val crore = n / 10000000L
        if (crore > 0) {
            sb.append(getTwoDigitHindi(crore.toInt())).append(" करोड़ ")
            n %= 10000000L
        }

        val lakh = n / 100000L
        if (lakh > 0) {
            sb.append(getTwoDigitHindi(lakh.toInt())).append(" लाख ")
            n %= 100000L
        }

        val thousand = n / 1000L
        if (thousand > 0) {
            sb.append(getTwoDigitHindi(thousand.toInt())).append(" हजार ")
            n %= 1000L
        }

        val hundred = n / 100L
        if (hundred > 0) {
            sb.append(hindiUnits[hundred.toInt()]).append(" सौ ")
            n %= 100L
        }

        if (n > 0) {
            sb.append(getTwoDigitHindi(n.toInt())).append(" ")
        }

        sb.append("रुपये मात्र")
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun getTwoDigitHindi(num: Int): String {
        return if (num in 1..99) hindiUnits[num] else ""
    }

    fun toEnglishWords(amount: Long): String {
        if (amount == 0L) return "Zero Rupees Only"
        if (amount < 0L) return "Minus " + toEnglishWords(-amount)

        var n = amount
        val sb = StringBuilder()

        val crore = n / 10000000L
        if (crore > 0) {
            sb.append(convertUnderThousandEnglish(crore.toInt())).append(" Crore ")
            n %= 10000000L
        }

        val lakh = n / 100000L
        if (lakh > 0) {
            sb.append(convertUnderThousandEnglish(lakh.toInt())).append(" Lakh ")
            n %= 100000L
        }

        val thousand = n / 1000L
        if (thousand > 0) {
            sb.append(convertUnderThousandEnglish(thousand.toInt())).append(" Thousand ")
            n %= 1000L
        }

        val hundred = n / 100L
        if (hundred > 0) {
            sb.append(englishUnits[hundred.toInt()]).append(" Hundred ")
            n %= 100L
        }

        if (n > 0) {
            if (n < 20) {
                sb.append(englishUnits[n.toInt()]).append(" ")
            } else {
                sb.append(englishTens[(n / 10).toInt()])
                if (n % 10 > 0) {
                    sb.append(" ").append(englishUnits[(n % 10).toInt()])
                }
                sb.append(" ")
            }
        }

        sb.append("Rupees Only")
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun convertUnderThousandEnglish(num: Int): String {
        var n = num
        val sb = StringBuilder()
        if (n >= 100) {
            sb.append(englishUnits[n / 100]).append(" Hundred ")
            n %= 100
        }
        if (n in 1..19) {
            sb.append(englishUnits[n])
        } else if (n >= 20) {
            sb.append(englishTens[n / 10])
            if (n % 10 > 0) sb.append(" ").append(englishUnits[n % 10])
        }
        return sb.toString().trim()
    }
}
