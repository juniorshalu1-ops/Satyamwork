package com.example.firebase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.database.AppDatabase
import com.example.database.AppRepository
import com.example.database.entities.HolidayEntity
import com.example.database.entities.NewsArticleEntity
import com.example.database.entities.NoticeEntity
import com.example.database.entities.PromoBannerEntity
import com.example.database.entities.VideoTutorialEntity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FirestoreRulesRuleTest : FirestoreEmulatorTestBase() {

    private suspend fun expectPermissionDenied(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected FirebaseFirestoreException(PERMISSION_DENIED)")
        } catch (t: Throwable) {
            val fsEx = t.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    @Test
    fun unauthenticated_publicRead_succeeds_and_writesRejected() = runBlocking {
        auth.signOut()

        // Public content read must succeed
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("notices").limit(5).get(Source.SERVER).await()
            firestore.collection("promo_banners").limit(5).get(Source.SERVER).await()
        }

        // Unauthenticated write & sensitive reads must be rejected
        expectPermissionDenied {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("notices").document("unauth_${UUID.randomUUID()}")
                    .set(
                        mapOf(
                            "title" to "Unauthorized Notice",
                            "summary" to "Denied",
                            "category" to "आदेश",
                            "issueDate" to "11/04/2026",
                            "isPublished" to true,
                            "timestamp" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
        }
        expectPermissionDenied {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("admin_audit_logs").limit(1).get(Source.SERVER).await()
            }
        }
    }

    @Test
    fun unverifiedAdminEmail_spoofRejected() = runBlocking {
        signInTestUser(ADMIN_EMAIL, verifyEmail = false)

        val notice = NoticeEntity(
            title = "Spoofed Notice ${UUID.randomUUID()}",
            summary = "Should fail without verified email",
            category = "आदेश",
            issueDate = "11/04/2026"
        )
        val ok = withTimeout(DEFAULT_TIMEOUT_MS) {
            AdminCloudPublisher.publishNoticeToCloud(
                firestore = firestore,
                notice = notice,
                notifyFn = { _, _, _ -> true },
                sendNotification = false
            )
        }
        assertFalse(ok)
    }

    @Test
    fun nonAdminTeacher_cannotWriteContentOrReadLogs_butCanSubmitOwnFeedback() = runBlocking {
        val teacherUid = signInTestUser(TEACHER_EMAIL, verifyEmail = true)

        expectPermissionDenied {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("promo_banners").document("teacher_${UUID.randomUUID()}")
                    .set(
                        mapOf(
                            "bannerId" to "b1",
                            "title" to "Unauthorized",
                            "orderIndex" to 1,
                            "isActive" to true,
                            "publishedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
        }
        expectPermissionDenied {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("admin_audit_logs").limit(1).get(Source.SERVER).await()
            }
        }

        val fbId = "fb_${UUID.randomUUID()}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("feedback").document(fbId).set(
                mapOf(
                    "userId" to teacherUid,
                    "message" to "Useful teacher tool",
                    "category" to "APPRECIATION",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val snap = firestore.collection("feedback").document(fbId).get(Source.SERVER).await()
            assertTrue(snap.exists())
        }
    }

    @Test
    fun verifiedAdmin_realPublisherAndSyncEngineRoundTrip_succeeds() = runBlocking {
        signInTestUser(ADMIN_EMAIL, verifyEmail = true)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = AppRepository(db)
            val uidSuffix = UUID.randomUUID().toString().take(8)

            // Publish notice (with Hindi characters in title to verify ASCII sanitized docId)
            val notice = NoticeEntity(
                title = "बिहार शिक्षक अवकाश आदेश $uidSuffix",
                summary = "सभी शिक्षकों के लिए महत्वपूर्ण सूचना",
                category = "सरकारी आदेश",
                issueDate = "11/04/2026",
                orderNumber = "ORD-$uidSuffix"
            )
            assertTrue(
                withTimeout(DEFAULT_TIMEOUT_MS) {
                    AdminCloudPublisher.publishNoticeToCloud(
                        firestore = firestore,
                        notice = notice,
                        notifyFn = { _, _, _ -> true },
                        sendNotification = false
                    )
                }
            )

            // Publish holiday
            val holiday = HolidayEntity(
                name = "बिहार दिवस $uidSuffix",
                dateStr = "22/03/2026",
                dayOfWeek = "रविवार",
                month = 3,
                type = "राजकीय अवकाश",
                durationDays = 1
            )
            assertTrue(
                withTimeout(DEFAULT_TIMEOUT_MS) {
                    AdminCloudPublisher.publishHolidayToCloud(
                        firestore = firestore,
                        holiday = holiday,
                        notifyFn = { _, _, _ -> true },
                        sendNotification = false
                    )
                }
            )

            // Publish promo banner (with serverTimestamp + canonical bannerId)
            val banner = PromoBannerEntity(
                bannerId = "banner_$uidSuffix",
                title = "नया अभियान बैनर $uidSuffix",
                description = "सक्रिय बैनर परीक्षण",
                category = "प्रमोशनल",
                imageUrl = "https://shikshak-sathi-r2-worker.satyamprakash100.workers.dev/banner.jpg",
                destinationUrl = "https://state.bihar.gov.in/educationbihar",
                orderIndex = 1,
                isActive = true
            )
            assertTrue(
                withTimeout(DEFAULT_TIMEOUT_MS) {
                    AdminCloudPublisher.publishBannerToCloud(
                        firestore = firestore,
                        banner = banner,
                        notifyFn = { _, _, _ -> true },
                        sendNotification = false
                    )
                }
            )

            // Sync back through CloudSyncEngine as an unauthenticated teacher
            auth.signOut()
            val salaryFlow = MutableStateFlow(Triple(53.0, 4.0, 8.0))
            assertTrue(
                withTimeout(DEFAULT_TIMEOUT_MS) {
                    CloudSyncEngine.fetchAndApplySection(firestore, CloudSection.NOTICES, repository, Source.SERVER, salaryFlow) &&
                        CloudSyncEngine.fetchAndApplySection(firestore, CloudSection.HOLIDAYS, repository, Source.SERVER, salaryFlow) &&
                        CloudSyncEngine.fetchAndApplySection(firestore, CloudSection.BANNERS, repository, Source.SERVER, salaryFlow)
                }
            )

            assertTrue(repository.allNoticesOnce().any { it.orderNumber == "ORD-$uidSuffix" })
            assertTrue(repository.allBannersOnce().any { it.bannerId == "banner_$uidSuffix" })

            // Delete banner as verified admin and ensure cloud cleanup + empty-sync Room pruning succeed
            signInTestUser(ADMIN_EMAIL, verifyEmail = true)
            assertTrue(
                withTimeout(DEFAULT_TIMEOUT_MS) {
                    AdminCloudPublisher.deleteBannerFromCloud(firestore, banner.bannerId, banner.title)
                }
            )
            auth.signOut()
            assertTrue(
                withTimeout(DEFAULT_TIMEOUT_MS) {
                    CloudSyncEngine.fetchAndApplySection(
                        firestore = firestore,
                        section = CloudSection.BANNERS,
                        repository = repository,
                        preferredSource = Source.SERVER,
                        currentSalaryRules = salaryFlow,
                        allowEmptySync = true
                    )
                }
            )
            assertFalse(repository.allBannersOnce().any { it.bannerId == "banner_$uidSuffix" })

            // Verify cloud video/news deletion pruning preserves live RSS items
            repository.insertVideo(
                VideoTutorialEntity(
                    title = "Cloud Video Deleted",
                    youtubeUrl = "https://youtu.be/abc12345678",
                    youtubeId = "abc12345678",
                    duration = "10:00"
                )
            )
            repository.insertVideo(
                VideoTutorialEntity(
                    title = "Live RSS Video Kept",
                    youtubeUrl = "https://youtu.be/rss12345678",
                    youtubeId = "rss12345678",
                    duration = "यूट्यूब"
                )
            )
            repository.syncVideosFromCloud(emptyList())
            val remainingVideos = repository.allVideosOnce()
            assertFalse(remainingVideos.any { it.title == "Cloud Video Deleted" })
            assertTrue(remainingVideos.any { it.title == "Live RSS Video Kept" })

            repository.insertNews(
                NewsArticleEntity(
                    title = "Cloud News Deleted",
                    summary = "Removed by admin",
                    category = "शिक्षा समाचार",
                    publishedAt = "11/04/2026"
                )
            )
            repository.insertNews(
                NewsArticleEntity(
                    title = "Live RSS News Kept",
                    summary = "Kept from RSS",
                    category = "लाइव RSS",
                    publishedAt = "11/04/2026"
                )
            )
            repository.syncNewsFromCloud(emptyList())
            val remainingNews = repository.allNewsOnce()
            assertFalse(remainingNews.any { it.title == "Cloud News Deleted" })
            assertTrue(remainingNews.any { it.title == "Live RSS News Kept" })
        } finally {
            db.close()
        }
    }

    @Test
    fun verifiedAdmin_auditLogsAreAppendOnly_andBroadcastTransactionWorks() = runBlocking {
        signInTestUser(ADMIN_EMAIL, verifyEmail = true)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("rule_test_prefs", Context.MODE_PRIVATE)

        // Append-only log check
        val logId = "log_${UUID.randomUUID()}"
        val logRef = firestore.collection("admin_audit_logs").document(logId)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            logRef.set(
                mapOf(
                    "username" to ADMIN_EMAIL,
                    "action" to "PUBLISH_NOTICE",
                    "details" to "Rule test log",
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        expectPermissionDenied {
            withTimeout(DEFAULT_TIMEOUT_MS) { logRef.update("details", "Tampered").await() }
        }
        expectPermissionDenied {
            withTimeout(DEFAULT_TIMEOUT_MS) { logRef.delete().await() }
        }

        // Atomic broadcast transaction check
        val broadcastOk = withTimeout(DEFAULT_TIMEOUT_MS) {
            AdminCloudPublisher.broadcastAdminUpload(
                context = context,
                firestore = firestore,
                sharedPrefs = prefs,
                knownBroadcastSeq = 0L,
                onSeqUpdated = {},
                onHistoryPrepended = {},
                title = "Rule Test Broadcast",
                section = "परीक्षण",
                details = "Emulator transaction test"
            )
        }
        assertTrue(broadcastOk)
    }

    private companion object {
        const val ADMIN_EMAIL = "satyamprakash100@gmail.com"
        const val TEACHER_EMAIL = "teacher.test@bihar.gov.in"
        const val DEFAULT_TIMEOUT_MS = 10000L
    }
}
