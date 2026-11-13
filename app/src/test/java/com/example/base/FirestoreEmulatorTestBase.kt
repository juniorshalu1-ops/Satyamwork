package com.example.base

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, manifest = Config.NONE, sdk = [36])
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
abstract class FirestoreEmulatorTestBase {

    protected lateinit var firestore: FirebaseFirestore
    protected lateinit var auth: FirebaseAuth
    protected lateinit var databaseId: String

    @Before
    open fun setUpFirebase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
        databaseId = if (resId != 0) {
            context.getString(resId)
        } else {
            FALLBACK_DATABASE_ID
        }

        val app = FirebaseApp.getApps(context).firstOrNull { it.name == TEST_APP_NAME }
            ?: FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("com.example")
                    .setProjectId(PROJECT_ID)
                    .setApiKey("fake-api-key-for-emulator")
                    .build(),
                TEST_APP_NAME
            ).also { testApp ->
                val testFirestore = FirebaseFirestore.getInstance(testApp, databaseId)
                testFirestore.useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
                testFirestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build()

                val testAuth = FirebaseAuth.getInstance(testApp)
                testAuth.useEmulator(EMULATOR_HOST, AUTH_PORT)
            }

        firestore = FirebaseFirestore.getInstance(app, databaseId)
        auth = FirebaseAuth.getInstance(app)
    }

    @After
    open fun tearDownFirebase() {
        auth.signOut()
    }

    protected suspend fun signInTestUser(email: String, verifyEmail: Boolean = true): String =
        withContext(Dispatchers.IO) {
            withTimeout(AUTH_TIMEOUT_MS) {
                val result = try {
                    auth.signInWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
                } catch (_: FirebaseAuthException) {
                    try {
                        auth.createUserWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
                    } catch (_: FirebaseAuthUserCollisionException) {
                        auth.signInWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
                    }
                }
                val user = checkNotNull(result.user) { "User auth failed" }
                setEmailVerifiedInAuthEmulator(user.uid, verifyEmail)
                user.reload().await()
                user.getIdToken(true).await()
                user.uid
            }
        }

    private fun setEmailVerifiedInAuthEmulator(localId: String, verified: Boolean) {
        val url = URL("http://$EMULATOR_HOST:$AUTH_PORT/identitytoolkit.googleapis.com/v1/projects/$PROJECT_ID/accounts:update")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Authorization", "Bearer owner")
            connectTimeout = 4000
            readTimeout = 4000
        }
        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write("""{"localId":"$localId","emailVerified":$verified}""")
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                val errBody = runCatching { conn.errorStream?.bufferedReader()?.readText() }.getOrNull().orEmpty()
                error("Auth emulator accounts:update failed with HTTP $code: $errBody")
            }
        } finally {
            conn.disconnect()
        }
    }

    protected fun Throwable.unwrapFirestoreException(): FirebaseFirestoreException? {
        var current: Throwable? = this
        while (current != null) {
            if (current is FirebaseFirestoreException) return current
            current = current.cause
        }
        return null
    }

    private companion object {
        const val TEST_APP_NAME = "RuleTestEmulatorApp"
        const val EMULATOR_HOST = "127.0.0.1"
        const val FIRESTORE_PORT = 8085
        const val AUTH_PORT = 9099
        const val PROJECT_ID = "demo-no-project"
        const val FALLBACK_DATABASE_ID = "ai-studio-android-shikshak-dd2aa215-bf07-4d91-a7ca-4cdbcad45c7d"
        const val DEFAULT_PASSWORD = "password123"
        const val AUTH_TIMEOUT_MS = 8000L
    }
}
