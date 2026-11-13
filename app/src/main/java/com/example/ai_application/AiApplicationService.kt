package com.example.ai_application

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class AiApplicationRequest(
    val recipient: String,
    val applicationType: String,
    val reason: String,
    val dates: String,
    val applicantName: String,
    val designation: String,
    val schoolName: String,
    val district: String,
    val language: String = "hi", // "hi" or "en"
    val mode: String = "write" // "write", "improve", "translate"
)

data class AiApplicationResponse(
    val subject: String,
    val body: String,
    val isAiGenerated: Boolean,
    val notice: String = ""
)

object AiApplicationService {
    private const val TAG = "AiApplicationService"
    private const val AI_TIMEOUT_MS = 30_000L

    private val generativeModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    suspend fun generateApplication(request: AiApplicationRequest): AiApplicationResponse = withContext(Dispatchers.IO) {
        // Try Firebase AI Logic if available and permitted
        try {
            val promptText = buildPrompt(request)
            val response = withTimeout(AI_TIMEOUT_MS) {
                generativeModel.generateContent(promptText)
            }
            val text = response.text ?: ""
            if (text.isNotBlank()) {
                val parsed = parseAiOutput(text, request)
                return@withContext parsed.copy(isAiGenerated = true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Firebase AI Logic generation failed; using offline format", e)
        }

        // High-Quality Rule-Based Offline Application Generation
        generateOfflineFormalApplication(request)
    }

    private fun buildPrompt(request: AiApplicationRequest): String {
        return """
            You are an expert official letter writer for Bihar Government School Teachers.
            Task: Write a strictly formal, respectful official application.
            Details:
            Recipient: ${request.recipient}
            Application Type: ${request.applicationType}
            Reason/Context: ${request.reason}
            Dates/Duration: ${request.dates}
            Applicant: ${request.applicantName}, ${request.designation}, ${request.schoolName}, District: ${request.district}
            Language: ${if (request.language == "hi") "Hindi (शुद्ध मानक प्रशासनिक हिंदी)" else "English"}
            Mode: ${request.mode}

            Output Format:
            SUBJECT: <One line precise subject>
            BODY:
            <Full respectful body formatted with salutation, paragraphs, and prayer for approval. Do not include signature section in body.>
        """.trimIndent()
    }

    private fun parseAiOutput(text: String, request: AiApplicationRequest): AiApplicationResponse {
        val subjectRegex = Regex("SUBJECT:\\s*(.+)", RegexOption.IGNORE_CASE)
        val bodyRegex = Regex("BODY:\\s*([\\s\\S]+)", RegexOption.IGNORE_CASE)

        val subjectMatch = subjectRegex.find(text)
        val bodyMatch = bodyRegex.find(text)

        val subject = subjectMatch?.groupValues?.get(1)?.trim()
            ?: "${request.applicationType} के संबंध में।"
        val body = bodyMatch?.groupValues?.get(1)?.trim()
            ?: text.trim()

        return AiApplicationResponse(
            subject = subject,
            body = body,
            isAiGenerated = true
        )
    }

    private fun generateOfflineFormalApplication(request: AiApplicationRequest): AiApplicationResponse {
        val isHi = request.language == "hi"

        val subject = if (isHi) {
            when {
                request.applicationType.contains("आकस्मिक") || request.applicationType.contains("CL") ->
                    "आकस्मिक अवकाश (CL) की स्वीकृति के संबंध में।"
                request.applicationType.contains("चिकित्सा") || request.applicationType.contains("Medical") ->
                    "चिकित्सा अवकाश स्वीकृत करने एवं योगदान प्रतिवेदन के संबंध में।"
                request.applicationType.contains("वेतन") || request.applicationType.contains("Salary") ->
                    "बकाया वेतन भुगतान / वेतन विसंगति निराकरण के संबंध में।"
                request.applicationType.contains("स्थानांतरण") || request.applicationType.contains("Transfer") ->
                    "ऐच्छिक / प्रशासनिक स्थानांतरण के संबंध में।"
                request.applicationType.contains("प्रशिक्षण") ->
                    "विभागीय प्रशिक्षण में प्रतिभागिता एवं कार्यमुक्ति के संबंध में।"
                request.applicationType.contains("स्पष्टीकरण") ->
                    "निर्गत स्पष्टीकरण पत्र के संतोषप्रद उत्तर के संबंध में।"
                else -> "${request.applicationType} के संबंध में।"
            }
        } else {
            when {
                request.applicationType.contains("Leave") || request.applicationType.contains("CL") ->
                    "Application for sanction of Leave for ${request.dates}."
                request.applicationType.contains("Salary") ->
                    "Representation regarding pending salary clearance."
                request.applicationType.contains("Transfer") ->
                    "Application for transfer on compassionate grounds."
                else -> "Official representation regarding ${request.applicationType}."
            }
        }

        val body = if (isHi) {
            """
महाशय,
सविनय निवेदन है कि मैं अधोहस्ताक्षरी ${request.applicantName}, ${request.designation}, ${request.schoolName}, प्रखंड सदर, जिला ${request.district} में पदस्थापित हूँ।

निवेदन पूर्वक उल्लेख करना है कि ${request.reason}। इस संदर्भ में आवश्यक अवधि ${request.dates} है।

अतः श्रीमान से सादर एवं विनम्र प्रार्थना है कि उपर्युक्त परिस्थिति एवं तथ्यों के आलोक में ${request.applicationType} को सहृदयतापूर्वक स्वीकृत / अग्रेषित करने की कृपा की जाए। इसके लिए मैं श्रीमान का सदैव आभारी रहूँगा/रहूँगी।
            """.trimIndent()
        } else {
            """
Respected Sir/Madam,
With utmost respect, I beg to submit that I, ${request.applicantName}, am presently serving as ${request.designation} at ${request.schoolName}, District ${request.district}.

I respectfully bring to your kind notice that ${request.reason}. The relevant period / dates involved are: ${request.dates}.

Therefore, I humbly request you to kindly grant approval for ${request.applicationType} at your earliest convenience. For this act of kindness, I shall ever remain grateful.
            """.trimIndent()
        }

        return AiApplicationResponse(
            subject = subject,
            body = body,
            isAiGenerated = false,
            notice = "ऑफलाइन मानक सरकारी प्रारूप द्वारा सृजित (Offline Government Format Generated)"
        )
    }
}
