package com.example.cloudflare

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.example.admin.AdminViewModel
import com.example.ui.theme.Gold100
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Teal700
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class R2UploadMode {
    IMAGE_ONLY,
    PDF_ONLY,
    PDF_OR_IMAGE
}

/**
 * Shared Firebase Google Sign-In helper using Jetpack CredentialManager.
 * Strictly validates that the signed-in Google account is satyamprakash100@gmail.com.
 */
object AdminGoogleAuthHelper {
    private const val TAG = "AdminGoogleAuthHelper"

    suspend fun signInWithGoogle(
        context: Context,
        adminViewModel: AdminViewModel
    ): Result<String> {
        if (!adminViewModel.isOnline()) {
            return Result.failure(IllegalStateException("Google लॉगिन हेतु इंटरनेट कनेक्शन आवश्यक है।"))
        }
        return try {
            val credentialManager = CredentialManager.create(context)
            val webClientId = context.getString(R.string.default_web_client_id)

            // Fast path: check previously authorized Google accounts first (near-instant / auto-select)
            val authorizedOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()

            val result = try {
                credentialManager.getCredential(
                    request = GetCredentialRequest.Builder()
                        .addCredentialOption(authorizedOption)
                        .build(),
                    context = context
                )
            } catch (cancel: GetCredentialCancellationException) {
                throw cancel
            } catch (firstErr: GetCredentialException) {
                Log.d(TAG, "Authorized account fast-path skipped, showing Google Sign-In picker: ${firstErr.type}")
                val interactiveOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                credentialManager.getCredential(
                    request = GetCredentialRequest.Builder()
                        .addCredentialOption(interactiveOption)
                        .build(),
                    context = context
                )
            }

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleCred.idToken
                val authRes = adminViewModel.signInAdminWithGoogleIdToken(idToken)
                authRes.map { it.email }
            } else {
                Result.failure(IllegalStateException("Google ID टोकन प्राप्त नहीं हो सका।"))
            }
        } catch (cancel: GetCredentialCancellationException) {
            Log.i(TAG, "User cancelled Google sign-in", cancel)
            Result.failure(IllegalStateException("Google साइन-इन रद्द किया गया।"))
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential Manager sign-in error: ${e.type}", e)
            Result.failure(IllegalStateException("Google खाता सत्यापन त्रुटि: ${e.message ?: "पुनः प्रयास करें"}"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected sign-in error", e)
            Result.failure(e)
        }
    }

    suspend fun clearCredentialSession(context: Context) {
        withContext(NonCancellable) {
            runCatching {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            }.onFailure { e ->
                Log.w(TAG, "clearCredentialState warning", e)
            }
        }
    }
}

/**
 * Zero-permission R2 upload bar for Admin dialogs:
 * - Images use Android Photo Picker (`PickVisualMedia`)
 * - PDFs use Storage Access Framework (`OpenDocument`)
 * Directly streams the selected file to Cloudflare R2 via the authenticated Worker API
 * and populates the entity's URL, type, and size fields.
 */
@Composable
fun R2UploadPickerRow(
    adminViewModel: AdminViewModel,
    folder: String,
    titleHint: String,
    existingUrl: String,
    mode: R2UploadMode,
    testTagPrefix: String,
    onUploaded: (R2UploadResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isUploading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    fun startUpload(uri: Uri) {
        isUploading = true
        isError = false
        progress = 0.05f
        statusText = "Cloudflare R2 पर अपलोड हो रहा है..."
        scope.launch {
            val res = adminViewModel.uploadFileToR2(
                uri = uri,
                folder = folder,
                title = titleHint,
                replaceUrl = existingUrl,
                onProgress = { pct -> progress = pct }
            )
            isUploading = false
            res.onSuccess { uploaded ->
                isError = false
                progress = 1f
                statusText = "R2 अपलोड सफल (${uploaded.fileType} • ${uploaded.sizeFormatted})"
                onUploaded(uploaded)
                Toast.makeText(context, "Cloudflare R2 पर फाइल अपलोड हुई!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                isError = true
                progress = 0f
                statusText = err.message ?: "अपलोड विफल रहा"
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) startUpload(uri)
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) startUpload(uri)
    }

    Surface(
        color = Color(0xFFF0F4FA),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Cloudflare R2 Upload",
                    tint = Navy800,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Cloudflare R2 डायरेक्ट अपलोड",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (mode == R2UploadMode.PDF_ONLY || mode == R2UploadMode.PDF_OR_IMAGE) {
                    OutlinedButton(
                        onClick = {
                            pdfPickerLauncher.launch(arrayOf("application/pdf"))
                        },
                        enabled = !isUploading,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("${testTagPrefix}_r2_pdf_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF चुनें",
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (R2Config.isR2ManagedUrl(context, existingUrl)) {
                                "नया PDF बदलें"
                            } else {
                                "PDF अपलोड (R2)"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Navy900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (mode == R2UploadMode.IMAGE_ONLY || mode == R2UploadMode.PDF_OR_IMAGE) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isUploading,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("${testTagPrefix}_r2_image_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "इमेज चुनें",
                            tint = Teal700,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (R2Config.isR2ManagedUrl(context, existingUrl)) {
                                "इमेज बदलें"
                            } else {
                                "फोटो अपलोड (R2)"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Navy900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (isUploading) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp),
                    color = Navy800,
                    trackColor = Color(0xFFD6E0EF)
                )
            }

            if (!statusText.isNullOrBlank()) {
                Text(
                    text = statusText.orEmpty(),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isError) Color(0xFFC62828) else Color(0xFF1B5E20)
                )
            }
        }
    }
}

/**
 * Dedicated Admin Panel sub-screen for managing Cloudflare R2 Worker endpoint,
 * testing health, publishing static manifests, and inspecting/deleting stored R2 files.
 */
@Composable
fun AdminR2StorageTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentWorkerUrl by viewModel.r2WorkerUrl.collectAsState()
    val files by viewModel.r2Files.collectAsState()
    val isLoading by viewModel.isLoadingR2.collectAsState()
    val statusMsg by viewModel.r2StatusMessage.collectAsState()
    val errorMsg by viewModel.r2ErrorMessage.collectAsState()
    val authRequired by viewModel.r2AuthRequired.collectAsState()
    val health by viewModel.r2HealthStatus.collectAsState()

    var workerUrlInput by remember(currentWorkerUrl) { mutableStateOf(currentWorkerUrl) }
    var isReAuthenticating by remember { mutableStateOf(false) }

    val r2Folders = remember {
        listOf(
            "banners" to "बैनर",
            "notices" to "आदेश",
            "downloads" to "डाउनलोड",
            "news" to "समाचार",
            "misc" to "अन्य"
        )
    }
    var selectedUploadFolder by remember { mutableStateOf("banners") }
    var uploadTitleHint by remember { mutableStateOf("") }
    var lastUploadedUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.refreshR2Dashboard()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9)),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Navy900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Cloudflare R2",
                            tint = Gold500,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cloudflare R2 क्लाउड स्टोरेज व CDN",
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                fontSize = 14.5.sp
                            )
                            Text(
                                text = "PDF, आदेश, बैनर, समाचार फोटो व स्टैटिक मेनिफेस्ट (Zero Firestore Storage)",
                                color = Gold100,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                    if (health != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0x22FFFFFF),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF69F0AE),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Worker सक्रिय • Project: ${health?.projectId}",
                                    color = PureWhite,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!errorMsg.isNullOrBlank() || authRequired) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "R2 त्रुटि",
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cloudflare R2 सिंक समस्या",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = Color(0xFFB71C1C)
                                )
                                Text(
                                    text = errorMsg ?: "Google एडमिन टोकन सत्यापित नहीं हो सका। कृपया पुनः साइन-इन करें।",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }
                        if (authRequired) {
                            Button(
                                onClick = {
                                    isReAuthenticating = true
                                    scope.launch {
                                        val res = AdminGoogleAuthHelper.signInWithGoogle(context, viewModel)
                                        isReAuthenticating = false
                                        res.onSuccess {
                                            Toast.makeText(context, "Google सत्यापन सफल! R2 सिंक हो रहा है...", Toast.LENGTH_SHORT).show()
                                            viewModel.refreshR2Dashboard()
                                        }.onFailure { err ->
                                            Toast.makeText(context, err.message ?: "Google साइन-इन विफल", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                enabled = !isReAuthenticating,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("r2_google_reauth_btn")
                            ) {
                                if (isReAuthenticating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = PureWhite
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Google पुनः साइन-इन",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Google से पुनः साइन-इन करें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Cloudflare Worker URL (workers.dev)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Navy900
                    )
                    OutlinedTextField(
                        value = workerUrlInput,
                        onValueChange = { workerUrlInput = it },
                        label = { Text("Worker Public URL (बिना कस्टम डोमेन *.workers.dev)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("r2_worker_url_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val saved = viewModel.saveR2WorkerUrl(workerUrlInput)
                                workerUrlInput = saved
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("r2_save_worker_url_btn")
                        ) {
                            Text("सेव व कनेक्ट करें", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.refreshR2Dashboard()
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("r2_refresh_files_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "रिफ्रेश", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("जांचें", fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val resetUrl = viewModel.resetR2WorkerUrl()
                                workerUrlInput = resetUrl
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("r2_reset_url_btn")
                        ) {
                            Text("डिफ़ॉल्ट URL", fontSize = 11.5.sp, color = Navy900)
                        }
                        OutlinedButton(
                            onClick = { viewModel.publishLibraryManifestToR2() },
                            enabled = !isLoading,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("r2_publish_manifest_btn")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = Teal700)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Catalog Manifest प्रकाशित करें",
                                fontSize = 11.5.sp,
                                color = Navy900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    if (!statusMsg.isNullOrBlank()) {
                        Text(
                            text = statusMsg.orEmpty(),
                            fontSize = 11.5.sp,
                            color = Teal700,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        item {
            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Cloudflare R2 में सीधे फाइल अपलोड करें",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Navy900
                    )
                    Text(
                        text = "अपलोड के लिए फ़ोल्डर चुनें और PDF या फोटो सीधे R2 CDN में अपलोड करें:",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        r2Folders.forEach { (folderKey, label) ->
                            FilterChip(
                                selected = selectedUploadFolder == folderKey,
                                onClick = { selectedUploadFolder = folderKey },
                                label = { Text("$label ($folderKey)", fontSize = 11.sp) },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("r2_folder_chip_$folderKey")
                            )
                        }
                    }
                    OutlinedTextField(
                        value = uploadTitleHint,
                        onValueChange = { uploadTitleHint = it },
                        label = { Text("फाइल का शीर्षक / नाम (वैकल्पिक)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("r2_direct_upload_title_input")
                    )
                    R2UploadPickerRow(
                        adminViewModel = viewModel,
                        folder = selectedUploadFolder,
                        titleHint = uploadTitleHint.ifBlank { selectedUploadFolder },
                        existingUrl = lastUploadedUrl,
                        mode = R2UploadMode.PDF_OR_IMAGE,
                        testTagPrefix = "admin_r2_tab",
                        onUploaded = { uploaded ->
                            lastUploadedUrl = uploaded.url
                            uploadTitleHint = ""
                            viewModel.loadR2Files()
                        }
                    )
                    if (lastUploadedUrl.isNotBlank()) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CDN लिंक: $lastUploadedUrl",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF1B5E20),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("R2 URL", lastUploadedUrl))
                                        Toast.makeText(context, "CDN लिंक कॉपी हो गया!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("r2_copy_last_uploaded_btn")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "CDN लिंक कॉपी करें", tint = Color(0xFF1B5E20))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "R2 बकेट फाइलें (${files.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = Navy900
                )
                Text(
                    text = "Auth: Google (${AdminViewModel.SUPER_ADMIN_EMAIL})",
                    fontSize = 10.sp,
                    color = Teal700
                )
            }
        }

        if (files.isEmpty() && !isLoading) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (!errorMsg.isNullOrBlank()) {
                                "सिंक त्रुटि के कारण R2 फाइल सूची लोड नहीं हुई। ऊपर त्रुटि जांचें।"
                            } else {
                                "कोई R2 फाइल सूचीबद्ध नहीं है — ऊपर से नई फाइल अपलोड करें या Catalog Manifest प्रकाशित करें।"
                            },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            items(files, key = { it.key }) { item ->
                ElevatedCard(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title.ifBlank { item.key.substringAfterLast('/') },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Navy900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.key} • ${item.sizeFormatted}",
                                fontSize = 10.5.sp,
                                color = Teal700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("R2 URL", item.url))
                                Toast.makeText(context, "CDN लिंक कॉपी हो गया!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "लिंक कॉपी करें", tint = Navy800)
                        }
                        IconButton(onClick = { viewModel.deleteR2FileDirectly(item) }) {
                            Icon(Icons.Default.Delete, contentDescription = "हटाएं", tint = Color(0xFFC62828))
                        }
                    }
                }
            }
        }
    }
}
