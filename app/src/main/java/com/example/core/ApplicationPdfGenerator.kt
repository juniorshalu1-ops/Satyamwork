package com.example.core

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.database.entities.DocumentDraftEntity
import java.io.File
import java.io.FileOutputStream

object ApplicationPdfGenerator {

    /**
     * Generates an official A4 Portrait Application / Letter PDF
     */
    fun generatePdf(
        context: Context,
        draft: DocumentDraftEntity
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 37, 55)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val linePaint = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 0.8f
        }

        var y = 50f
        canvas.drawText("आवेदन पत्र / Official Letter", (pageWidth / 2).toFloat(), y, titlePaint)
        y += 20f
        canvas.drawLine(40f, y, (pageWidth - 40).toFloat(), y, linePaint)
        y += 30f

        // Recipient
        canvas.drawText("सेवा में,", 50f, y, headerPaint)
        y += 18f
        val recipientLines = draft.recipient.split("\n")
        for (r in recipientLines) {
            canvas.drawText(r.trim(), 50f, y, textPaint)
            y += 16f
        }
        y += 15f

        // Subject
        val subPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("विषय: ${draft.subject}", 50f, y, subPaint)
        y += 24f

        // Salutation
        canvas.drawText("महाशय / महोदया,", 50f, y, headerPaint)
        y += 22f

        // Body text with word wrap
        val margin = 50f
        val maxTextWidth = pageWidth - (margin * 2)
        val words = draft.body.replace("\n", " \n ").split(" ")

        var line = ""
        for (word in words) {
            if (word == "\n") {
                canvas.drawText(line, margin, y, textPaint)
                line = ""
                y += 18f
            } else {
                val testLine = if (line.isEmpty()) word else "$line $word"
                val measuredWidth = textPaint.measureText(testLine)
                if (measuredWidth > maxTextWidth) {
                    canvas.drawText(line, margin, y, textPaint)
                    line = word
                    y += 18f
                } else {
                    line = testLine
                }
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, margin, y, textPaint)
            y += 18f
        }

        y += 40f
        // Date and Applicant Info
        canvas.drawText("दिनांक: ${draft.dateStr}", 50f, y, textPaint)
        canvas.drawText("स्थान: ${draft.district}, बिहार", 50f, y + 18f, textPaint)

        val applicantRightPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val rightX = (pageWidth - 50).toFloat()
        canvas.drawText("भवदीय / भवदीया,", rightX, y, applicantRightPaint)
        y += 18f
        canvas.drawText(draft.applicantName, rightX, y, headerPaint.apply { textAlign = Paint.Align.RIGHT })
        y += 16f
        canvas.drawText(draft.designation, rightX, y, applicantRightPaint)
        y += 16f
        canvas.drawText(draft.schoolName, rightX, y, applicantRightPaint)
        y += 16f
        canvas.drawText("प्रखंड: सदर, जिला: ${draft.district}", rightX, y, applicantRightPaint)

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "applications").apply { mkdirs() }
        val outputFile = File(outputDir, "Application_${System.currentTimeMillis()}.pdf")
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
        context.startActivity(Intent.createChooser(shareIntent, "आवेदन पत्र साझा करें (Share Application PDF)"))
    }
}
