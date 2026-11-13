package com.example.core

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.database.entities.SalaryCalculationResult
import com.example.database.entities.SalaryCalculatorHelper
import com.example.database.entities.TeacherProfileEntity
import java.io.File
import java.io.FileOutputStream

object SalaryPdfGenerator {

    /**
     * Generates an official A4 Portrait Salary Statement PDF
     * A4 Portrait: width 595 pt, height 842 pt.
     */
    fun generatePdf(
        context: Context,
        teacher: TeacherProfileEntity,
        salary: SalaryCalculationResult,
        monthName: String,
        year: Int
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 37, 55) // Navy Blue
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subTitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val sectionHeaderPaint = Paint().apply {
            color = Color.rgb(15, 37, 55)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val boldLabelPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valuePaint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val rightValuePaint = Paint().apply {
            color = Color.rgb(15, 37, 55)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val disclaimerPaint = Paint().apply {
            color = Color.rgb(180, 40, 40)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        val linePaint = Paint().apply {
            color = Color.rgb(200, 210, 220)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val boxPaint = Paint().apply {
            color = Color.rgb(245, 248, 252)
            style = Paint.Style.FILL
        }

        var y = 40f
        // Header
        canvas.drawText("शिक्षक मासिक वेतन संगणना विवरण (Salary Statement)", (pageWidth / 2).toFloat(), y, titlePaint)
        y += 16f
        canvas.drawText("${teacher.schoolName} | UDISE: ${teacher.udiseCode} | प्रखंड: ${teacher.block}, जिला: ${teacher.district}", (pageWidth / 2).toFloat(), y, subTitlePaint)
        y += 14f
        canvas.drawText("माह: $monthName $year | 7th Pay Commission Scale", (pageWidth / 2).toFloat(), y, subTitlePaint)
        y += 12f
        canvas.drawLine(35f, y, (pageWidth - 35).toFloat(), y, linePaint)
        y += 20f

        // Teacher Info Card
        canvas.drawRect(35f, y, (pageWidth - 35).toFloat(), y + 80f, boxPaint)
        canvas.drawRect(35f, y, (pageWidth - 35).toFloat(), y + 80f, linePaint)

        var cardY = y + 18f
        canvas.drawText("शिक्षक का नाम:", 45f, cardY, boldLabelPaint)
        canvas.drawText(teacher.name, 125f, cardY, valuePaint)
        canvas.drawText("पदनाम:", 310f, cardY, boldLabelPaint)
        canvas.drawText(teacher.designation, 360f, cardY, valuePaint)

        cardY += 20f
        canvas.drawText("मानव संपदा ID:", 45f, cardY, boldLabelPaint)
        canvas.drawText(if (teacher.employeeId.isBlank()) "-" else teacher.employeeId, 125f, cardY, valuePaint)
        canvas.drawText("PRAN/पेंशन:", 310f, cardY, boldLabelPaint)
        canvas.drawText(if (teacher.pranNumber.isBlank()) "NPS योजना" else teacher.pranNumber, 380f, cardY, valuePaint)

        cardY += 20f
        canvas.drawText("बैंक खाता संख्या:", 45f, cardY, boldLabelPaint)
        canvas.drawText(if (teacher.bankAccountNumber.isBlank()) "स्टेट बैंक ऑफ़ इंडिया" else teacher.bankAccountNumber, 135f, cardY, valuePaint)
        canvas.drawText("IFSC कोड:", 310f, cardY, boldLabelPaint)
        canvas.drawText(if (teacher.ifscCode.isBlank()) "-" else teacher.ifscCode, 375f, cardY, valuePaint)

        y += 105f

        // Allowances (उपार्जन) and Deductions (कटौती) Two-Column Layout
        val colWidth = (pageWidth - 80) / 2f
        val leftX = 35f
        val rightX = leftX + colWidth + 10f

        // Column Titles
        canvas.drawText("1. वेतन एवं भत्ते (Gross Allowances)", leftX, y, sectionHeaderPaint)
        canvas.drawText("2. कटौतियाँ (Deductions)", rightX, y, sectionHeaderPaint)
        y += 8f
        canvas.drawLine(leftX, y, leftX + colWidth, y, linePaint)
        canvas.drawLine(rightX, y, rightX + colWidth, y, linePaint)
        y += 16f

        fun drawLeftRow(label: String, amount: Double) {
            canvas.drawText(label, leftX, y, valuePaint)
            canvas.drawText("₹%,.2f".format(amount), leftX + colWidth, y, rightValuePaint)
        }
        fun drawRightRow(label: String, amount: Double) {
            canvas.drawText(label, rightX, y, valuePaint)
            canvas.drawText("₹%,.2f".format(amount), rightX + colWidth, y, rightValuePaint)
        }

        drawLeftRow("मूल वेतन (Basic Pay)", salary.basicPay)
        drawRightRow(if (salary.isNps) "NPS अंशदान (10% Basic+DA)" else "GPF सामान्य भविष्य निधि", salary.npsGpfDeduction)
        y += 22f

        drawLeftRow("महंगाई भत्ता (DA @ ${salary.daPercent.toInt()}%)", salary.daAmount)
        drawRightRow("सामूहिक बीमा (GIS)", salary.gisDeduction)
        y += 22f

        drawLeftRow("मकान किराया भत्ता (HRA @ ${salary.hraPercent.toInt()}%)", salary.hraAmount)
        drawRightRow("आयकर कटौती (TDS / IT)", salary.incomeTaxDeduction)
        y += 22f

        drawLeftRow("चिकित्सा भत्ता (Medical)", salary.medicalAllowance)
        drawRightRow("अन्य कटौतियाँ (Other)", salary.otherDeduction)
        y += 22f

        drawLeftRow("अन्य भत्ते (Other Allowance)", salary.otherAllowance)
        y += 12f

        canvas.drawLine(leftX, y, leftX + colWidth, y, linePaint)
        canvas.drawLine(rightX, y, rightX + colWidth, y, linePaint)
        y += 18f

        // Totals
        drawLeftRow("सकल वेतन (Gross Salary)", salary.grossSalary)
        drawRightRow("कुल कटौती (Total Deductions)", salary.totalDeductions)
        y += 25f

        // Net Salary Box (Highlight)
        val netBoxPaint = Paint().apply {
            color = Color.rgb(235, 245, 235)
            style = Paint.Style.FILL
        }
        canvas.drawRect(35f, y, (pageWidth - 35).toFloat(), y + 60f, netBoxPaint)
        canvas.drawRect(35f, y, (pageWidth - 35).toFloat(), y + 60f, linePaint)

        val netTitlePaint = Paint().apply {
            color = Color.rgb(20, 100, 30)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val netAmtPaint = Paint().apply {
            color = Color.rgb(20, 100, 30)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("शुद्ध देय वेतन (Net Payable Salary):", 50f, y + 26f, netTitlePaint)
        canvas.drawText("₹%,.2f".format(salary.netSalary), (pageWidth - 50).toFloat(), y + 26f, netAmtPaint)

        val wordsHindi = NumberToWords.toHindiWords(salary.netSalary.toLong())
        canvas.drawText("अक्षरे: $wordsHindi", 50f, y + 46f, valuePaint)

        y += 85f

        // Mandatory Disclaimer Box
        canvas.drawText("महत्वपूर्ण अस्वीकरण (Disclaimer):", (pageWidth / 2).toFloat(), y, disclaimerPaint)
        y += 15f
        canvas.drawText(SalaryCalculatorHelper.SALARY_DISCLAIMER_HI, (pageWidth / 2).toFloat(), y, disclaimerPaint)
        y += 13f
        canvas.drawText(SalaryCalculatorHelper.SALARY_DISCLAIMER_EN, (pageWidth / 2).toFloat(), y, subTitlePaint)

        // Signatures at bottom
        val sigY = pageHeight - 75f
        canvas.drawLine(50f, sigY, 210f, sigY, linePaint)
        canvas.drawText("हस्ताक्षर संबंधित शिक्षक", 130f, sigY + 16f, subTitlePaint)

        canvas.drawLine((pageWidth - 210).toFloat(), sigY, (pageWidth - 50).toFloat(), sigY, linePaint)
        canvas.drawText("हस्ताक्षर एवं मुहर प्रधानाध्यापक", (pageWidth - 130).toFloat(), sigY + 16f, subTitlePaint)

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "salary_statements").apply { mkdirs() }
        val outputFile = File(outputDir, "Salary_Statement_${monthName}_$year.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return outputFile
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "वेतन विवरणी साझा करें (Share Salary Statement)"))
    }
}
