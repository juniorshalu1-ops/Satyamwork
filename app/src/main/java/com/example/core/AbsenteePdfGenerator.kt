package com.example.core

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.database.entities.MonthlyAbsenteeDetailEntity
import com.example.database.entities.MonthlyAbsenteeReportEntity
import com.example.database.entities.SchoolProfileEntity
import java.io.File
import java.io.FileOutputStream

object AbsenteePdfGenerator {

    /**
     * Generates an official A4 Landscape Monthly Absentee Report PDF
     * A4 Landscape: width 842 pt, height 595 pt.
     */
    fun generatePdf(
        context: Context,
        report: MonthlyAbsenteeReportEntity,
        school: SchoolProfileEntity,
        details: List<MonthlyAbsenteeDetailEntity>
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 842
        val pageHeight = 595

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subTitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val cellPaint = Paint().apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }
        val cellCenterPaint = Paint().apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }
        val bgHeaderPaint = Paint().apply {
            color = Color.rgb(235, 240, 248)
            style = Paint.Style.FILL
        }

        val rowsPerPage = 12
        val totalPages = maxOf(1, (details.size + rowsPerPage - 1) / rowsPerPage)

        val monthName = DateUtils.getMonthName(report.month, isHindi = true)

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Header Section
            var yPos = 35f
            canvas.drawText("कार्यालय प्रधानाध्यापक, ${school.schoolName}", (pageWidth / 2).toFloat(), yPos, titlePaint)
            yPos += 16f
            canvas.drawText("${school.address} | UDISE कोड: ${school.udiseCode} | प्रखंड: ${school.block}, जिला: ${school.district}", (pageWidth / 2).toFloat(), yPos, subTitlePaint)
            yPos += 16f
            canvas.drawText("माहवार शिक्षक एवं शिक्षकेत्तर कर्मचारी अनुपस्थिति विवरणी (Absentee Statement) - माह: $monthName ${report.year}", (pageWidth / 2).toFloat(), yPos, titlePaint)
            yPos += 14f
            canvas.drawText("कुल कार्य दिवस: ${report.totalWorkingDays} | विवरणी पृष्ठ: ${pageIndex + 1} / $totalPages", (pageWidth / 2).toFloat(), yPos, subTitlePaint)

            yPos += 15f
            val tableStartX = 25f
            val tableWidth = pageWidth - 50f

            // Table Column Widths
            // Cols: [Sl(25), Name(120), Desig(100), ID(60), Basic(50), WorkDays(40), Pres(35), CL(28), EL(28), Med(28), Mat/Pat(32), Trng(30), Duty(30), Oth(28), Abs(35), Rem(90)]
            val colWidths = floatArrayOf(
                25f, 130f, 105f, 55f, 50f, 40f, 35f, 28f, 28f, 28f, 32f, 30f, 30f, 28f, 35f, 95f
            )
            val colHeaders = arrayOf(
                "क्र०", "कर्मचारी का नाम", "पदनाम", "मानव संपदा/ID", "मूल वेतन", "कार्य दिवस",
                "उपस्थित", "CL", "EL", "चिकित्सा", "मातृ/पितृ", "प्रशिक्षण", "शासकीय", "अन्य", "अनुपस्थित", "मंतव्य"
            )

            val rowHeight = 22f
            // Draw Table Header Background
            canvas.drawRect(tableStartX, yPos, tableStartX + tableWidth, yPos + rowHeight, bgHeaderPaint)
            canvas.drawRect(tableStartX, yPos, tableStartX + tableWidth, yPos + rowHeight, linePaint)

            var curX = tableStartX
            for (c in colHeaders.indices) {
                val cw = colWidths[c]
                canvas.drawText(colHeaders[c], curX + cw / 2, yPos + 14f, headerPaint)
                canvas.drawLine(curX, yPos, curX, yPos + rowHeight, linePaint)
                curX += cw
            }
            canvas.drawLine(curX, yPos, curX, yPos + rowHeight, linePaint)

            yPos += rowHeight

            // Draw Data Rows
            val startIndex = pageIndex * rowsPerPage
            val endIndex = minOf(startIndex + rowsPerPage, details.size)

            for (i in startIndex until endIndex) {
                val item = details[i]
                canvas.drawRect(tableStartX, yPos, tableStartX + tableWidth, yPos + rowHeight, linePaint)

                var cellX = tableStartX
                fun drawCol(text: String, width: Float, alignCenter: Boolean = false) {
                    canvas.drawLine(cellX, yPos, cellX, yPos + rowHeight, linePaint)
                    if (alignCenter) {
                        canvas.drawText(text, cellX + width / 2, yPos + 14f, cellCenterPaint)
                    } else {
                        val truncated = if (text.length > 22) text.take(20) + ".." else text
                        canvas.drawText(truncated, cellX + 4f, yPos + 14f, cellPaint)
                    }
                    cellX += width
                }

                drawCol("${i + 1}", colWidths[0], true)
                drawCol(item.teacherName, colWidths[1], false)
                drawCol(item.designation, colWidths[2], false)
                drawCol(if (item.employeeId.isBlank()) "-" else item.employeeId, colWidths[3], true)
                drawCol("₹${item.basicPay.toInt()}", colWidths[4], true)
                drawCol("${item.workingDays}", colWidths[5], true)
                drawCol("${item.presentDays}", colWidths[6], true)
                drawCol("${item.clDays}", colWidths[7], true)
                drawCol("${item.elDays}", colWidths[8], true)
                drawCol("${item.medicalDays}", colWidths[9], true)
                drawCol("${item.maternityDays + item.paternityDays}", colWidths[10], true)
                drawCol("${item.trainingDays}", colWidths[11], true)
                drawCol("${item.officialDutyDays + item.electionDutyDays + item.censusDutyDays}", colWidths[12], true)
                drawCol("${item.otherLeaveDays}", colWidths[13], true)
                drawCol("${item.absentDays}", colWidths[14], true)
                drawCol(if (item.remarks.isBlank()) "संतोषप्रद" else item.remarks, colWidths[15], false)

                canvas.drawLine(cellX, yPos, cellX, yPos + rowHeight, linePaint)
                yPos += rowHeight
            }

            // Footer Signatures (on the last page or every page)
            val footerY = pageHeight - 65f
            canvas.drawLine(40f, footerY, 200f, footerY, linePaint)
            canvas.drawText("सत्यापनकर्ता / वरीय शिक्षक हस्ताक्षर", 120f, footerY + 15f, subTitlePaint)

            canvas.drawRect(350f, footerY - 20f, 470f, footerY + 40f, linePaint)
            canvas.drawText("विद्यालय गोल मुहर", 410f, footerY + 15f, subTitlePaint)

            canvas.drawLine(pageWidth - 220f, footerY, pageWidth - 40f, footerY, linePaint)
            canvas.drawText("हस्ताक्षर एवं मुहर प्रधानाध्यापक", pageWidth - 130f, footerY + 15f, titlePaint)
            canvas.drawText(school.headmasterName, pageWidth - 130f, footerY + 28f, subTitlePaint)

            pdfDocument.finishPage(page)
        }

        // Save PDF to cache dir
        val outputDir = File(context.cacheDir, "absentee_reports").apply { mkdirs() }
        val outputFile = File(outputDir, "Absentee_${report.month}_${report.year}.pdf")
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
        context.startActivity(Intent.createChooser(shareIntent, "अनुपस्थिति विवरणी साझा करें (Share PDF)"))
    }
}
