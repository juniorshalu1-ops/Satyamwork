package com.example

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.core.DeepLinkHelper
import com.example.core.DeepLinkTarget
import com.example.core.DeepLinkType
import com.example.core.DownloadSyncHelper
import com.example.database.AppDatabase
import com.example.database.entities.DownloadResourceEntity
import com.example.database.entities.HolidayEntity
import com.example.database.entities.NewsArticleEntity
import com.example.database.entities.NoticeEntity
import com.example.database.entities.VideoTutorialEntity
import com.example.ui.components.ImageUrlHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppFixesUnitTest {

    // =========================================================
    // 1. IMAGE URL HELPER & CACHE-BUSTING TESTS
    // =========================================================
    @Test
    fun `image url helper converts drive share links and appends cache version`() {
        val driveShare = "https://drive.google.com/file/d/1AbCdEfGhIjK_987/view?usp=sharing"
        val normalized = ImageUrlHelper.normalizeImageUrl(driveShare)
        assertEquals("https://drive.google.com/uc?export=view&id=1AbCdEfGhIjK_987", normalized)

        val driveOpen = "https://drive.google.com/open?id=XYZ_12345"
        assertEquals("https://drive.google.com/uc?export=view&id=XYZ_12345", ImageUrlHelper.normalizeImageUrl(driveOpen))

        val v1Key = ImageUrlHelper.buildCacheKey(driveShare, versionTag = 100L)
        val v2Key = ImageUrlHelper.buildCacheKey(driveShare, versionTag = 200L)
        assertFalse(v1Key == v2Key)
        assertTrue(v2Key.endsWith("#v=200"))

        assertTrue(ImageUrlHelper.isLikelyImageUrl("https://example.com/form.jpg", "JPG"))
        assertTrue(ImageUrlHelper.isLikelyImageUrl("https://example.com/photo.png"))
        assertFalse(ImageUrlHelper.isLikelyImageUrl("https://example.com/order.pdf", "PDF"))
    }

    // =========================================================
    // 2. DOWNLOAD SYNC HELPER & DEDUPLICATION TESTS
    // =========================================================
    @Test
    fun `download canonical doc id stays stable across id and title changes when cloudId exists`() {
        val original = DownloadResourceEntity(
            id = 5L,
            cloudId = "dl_fixed_abc123",
            title = "CL Form 2026",
            category = "प्रपत्र",
            fileType = "PDF",
            pdfUrl = "https://example.com/cl.pdf",
            timestamp = 1000L
        )
        val editedOnAnotherDevice = original.copy(
            id = 99L,
            title = "CL Form 2026 Updated",
            timestamp = 2000L
        )
        assertEquals(
            DownloadSyncHelper.resolveCanonicalDocId(original),
            DownloadSyncHelper.resolveCanonicalDocId(editedOnAnotherDevice)
        )
        assertEquals("dl_fixed_abc123", DownloadSyncHelper.resolveCanonicalDocId(original))
    }

    @Test
    fun `download deduplicate collapses duplicates while preserving local state`() {
        val localDownloaded = DownloadResourceEntity(
            id = 12L,
            cloudId = "",
            title = "Joining Letter",
            category = "प्रपत्र",
            fileType = "PDF",
            pdfUrl = "https://example.com/joining.pdf",
            localFilePath = "/storage/joining.pdf",
            downloadCount = 3,
            timestamp = 1000L
        )
        val cloudCopy = DownloadResourceEntity(
            id = 0L,
            cloudId = "dl_cloud_join_1",
            title = "Joining Letter Updated",
            category = "प्रपत्र",
            fileType = "PDF",
            pdfUrl = "https://example.com/joining.pdf",
            localFilePath = "",
            downloadCount = 0,
            timestamp = 2000L
        )
        val deduped = DownloadSyncHelper.deduplicate(listOf(localDownloaded, cloudCopy))
        assertEquals(1, deduped.size)
        val merged = deduped.first()
        assertEquals("dl_cloud_join_1", merged.cloudId)
        assertEquals(12L, merged.id)
        assertEquals("/storage/joining.pdf", merged.localFilePath)
        assertEquals(3, merged.downloadCount)
    }

    // =========================================================
    // 3. PUSH NOTIFICATION DEEP LINKING TESTS
    // =========================================================
    @Test
    fun `deep link helper writes parses clears intent and finds exact entities`() {
        val intent = Intent()
        DeepLinkHelper.writeToIntent(
            intent = intent,
            type = DeepLinkType.NEWS,
            contentId = "42",
            title = "शिक्षक वेतन अपडेट",
            targetUrl = "https://example.com/news/42",
            eventId = 555L
        )

        val parsed = DeepLinkHelper.parseIntent(intent)
        assertNotNull(parsed)
        assertEquals(DeepLinkType.NEWS, parsed!!.type)
        assertEquals("42", parsed.contentId)
        assertEquals("शिक्षक वेतन अपडेट", parsed.title)
        assertEquals(555L, parsed.eventId)

        DeepLinkHelper.clearExtras(intent)
        assertNull(DeepLinkHelper.parseIntent(intent))

        val newsList = listOf(
            NewsArticleEntity(id = 10L, title = "अन्य समाचार", summary = "s", publishedAt = "01/01/2026"),
            NewsArticleEntity(id = 42L, title = "शिक्षक वेतन अपडेट", summary = "s2", publishedAt = "02/01/2026", sourceUrl = "https://example.com/news/42")
        )
        assertEquals(42L, DeepLinkHelper.findMatchingNews(newsList, parsed)?.id)

        val notices = listOf(
            NoticeEntity(id = 7L, title = "अवकाश आदेश", summary = "s", category = "आदेश", issueDate = "05/01/2026", orderNumber = "ORD-77")
        )
        assertEquals(
            7L,
            DeepLinkHelper.findMatchingNotice(
                notices,
                DeepLinkTarget(DeepLinkType.NOTICE, contentId = "ORD-77")
            )?.id
        )

        val holidays = listOf(
            HolidayEntity(id = 3L, name = "दीपावली", dateStr = "08/11/2026", dayOfWeek = "रविवार", month = 11, type = "त्योहार")
        )
        val holidayCid = DeepLinkHelper.buildHolidayContentId(holidays.first())
        assertEquals(
            3L,
            DeepLinkHelper.findMatchingHoliday(
                holidays,
                DeepLinkTarget(DeepLinkType.HOLIDAY, contentId = holidayCid)
            )?.id
        )

        val downloads = listOf(
            DownloadResourceEntity(id = 8L, cloudId = "dl_test_8", title = "मेडिकल फॉर्म", pdfUrl = "https://example.com/med.pdf")
        )
        assertEquals(
            8L,
            DeepLinkHelper.findMatchingDownload(
                downloads,
                DeepLinkTarget(DeepLinkType.DOWNLOAD, contentId = "dl_test_8")
            )?.id
        )

        val videos = listOf(
            VideoTutorialEntity(id = 9L, title = "ई-शिक्षाकोष गाइड", youtubeUrl = "https://youtu.be/abc123", youtubeId = "abc123")
        )
        assertEquals(
            9L,
            DeepLinkHelper.findMatchingVideo(
                videos,
                DeepLinkTarget(DeepLinkType.VIDEO, contentId = "abc123")
            )?.id
        )
    }

    // =========================================================
    // 4. ROOM MIGRATION 8 -> 9 & CLOUD SYNC IDEMPOTENCY TEST
    // =========================================================
    @Test
    fun `room migration 8 to 9 preserves user data and syncDownloadsFromCloud does not duplicate rows`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_8_9_${System.currentTimeMillis()}.db"
        context.deleteDatabase(dbName)

        // Step 1: Create v8 SQLite DB manually and seed existing notices & downloads
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(8) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `notices` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `summary` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `issueDate` TEXT NOT NULL,
                            `department` TEXT NOT NULL,
                            `orderNumber` TEXT NOT NULL,
                            `officialUrl` TEXT NOT NULL,
                            `pdfUrl` TEXT NOT NULL,
                            `isFeatured` INTEGER NOT NULL,
                            `isBookmarked` INTEGER NOT NULL,
                            `timestamp` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_notices_orderNumber` ON `notices` (`orderNumber`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_notices_category` ON `notices` (`category`)")
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `download_resources` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `fileType` TEXT NOT NULL,
                            `fileSize` TEXT NOT NULL,
                            `pdfUrl` TEXT NOT NULL,
                            `publicationDate` TEXT NOT NULL,
                            `orderNumber` TEXT NOT NULL,
                            `description` TEXT NOT NULL,
                            `localFilePath` TEXT NOT NULL,
                            `isVisible` INTEGER NOT NULL,
                            `downloadCount` INTEGER NOT NULL,
                            `displayOrder` INTEGER NOT NULL,
                            `timestamp` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO notices (id, title, summary, category, issueDate, department, orderNumber, officialUrl, pdfUrl, isFeatured, isBookmarked, timestamp)
                        VALUES (1, 'Saved Notice', 'Important Summary', 'आदेश', '01/01/2026', 'शिक्षा विभाग', '101', 'https://example.com', '', 1, 1, 123456)
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO download_resources (id, title, category, fileType, fileSize, pdfUrl, publicationDate, orderNumber, description, localFilePath, isVisible, downloadCount, displayOrder, timestamp)
                        VALUES (5, 'Saved Form', 'प्रपत्र', 'PDF', '500 KB', 'https://example.com/saved.pdf', '01/01/2026', 'F-5', 'Desc', '/local/saved.pdf', 1, 4, 1, 123456)
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val sqliteHelper = FrameworkSQLiteOpenHelperFactory().create(config)
        val rawDb = sqliteHelper.writableDatabase
        // Apply actual MIGRATION_8_9
        AppDatabase.MIGRATION_8_9.migrate(rawDb)

        // Verify columns added and data preserved in SQLite
        rawDb.query("SELECT title, imageUrl FROM notices WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Saved Notice", c.getString(0))
            assertEquals("", c.getString(1))
        }
        rawDb.query("SELECT title, cloudId, localFilePath, downloadCount FROM download_resources WHERE id = 5").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Saved Form", c.getString(0))
            assertEquals("", c.getString(1))
            assertEquals("/local/saved.pdf", c.getString(2))
            assertEquals(4, c.getInt(3))
        }
        sqliteHelper.close()
        context.deleteDatabase(dbName)

        // Step 2: Test Room ContentDao.syncDownloadsFromCloud deduplication & deletion propagation
        val roomDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = roomDb.contentDao()
            dao.insertDownload(
                DownloadResourceEntity(
                    id = 0L,
                    cloudId = "",
                    title = "Saved Form",
                    fileType = "PDF",
                    pdfUrl = "https://example.com/saved.pdf",
                    localFilePath = "/local/saved.pdf",
                    downloadCount = 4,
                    timestamp = 1000L
                )
            )
            val initialRow = dao.getAllDownloadsOnce().single()

            // Cloud returns 2 copies of the same document (duplicate legacy doc + canonical doc) + 1 new item
            val cloudItems = listOf(
                DownloadResourceEntity(
                    id = 0L,
                    cloudId = "dl_saved_canonical",
                    title = "Saved Form",
                    fileType = "PDF",
                    pdfUrl = "https://example.com/saved.pdf",
                    timestamp = 2000L
                ),
                DownloadResourceEntity(
                    id = 0L,
                    cloudId = "",
                    title = "Saved Form",
                    fileType = "PDF",
                    pdfUrl = "https://example.com/saved.pdf",
                    timestamp = 1500L
                ),
                DownloadResourceEntity(
                    id = 0L,
                    cloudId = "dl_second_item",
                    title = "New JPG Circular",
                    fileType = "JPG",
                    pdfUrl = "https://example.com/circular.jpg",
                    timestamp = 3000L
                )
            )

            // Sync repeatedly -> must converge to 2 items and preserve initialRow.id & localFilePath
            dao.syncDownloadsFromCloud(cloudItems)
            dao.syncDownloadsFromCloud(cloudItems)

            val synced = dao.getAllDownloadsOnce()
            assertEquals(2, synced.size)
            val updatedSaved = synced.find { it.pdfUrl == "https://example.com/saved.pdf" }
            assertNotNull(updatedSaved)
            assertEquals(initialRow.id, updatedSaved!!.id)
            assertEquals("dl_saved_canonical", updatedSaved.cloudId)
            assertEquals("/local/saved.pdf", updatedSaved.localFilePath)
            assertEquals(4, updatedSaved.downloadCount)

            // Cross-device delete: cloud removes dl_second_item -> local Room deletes it cleanly
            dao.syncDownloadsFromCloud(listOf(cloudItems.first()))
            val afterDelete = dao.getAllDownloadsOnce()
            assertEquals(1, afterDelete.size)
            assertEquals("dl_saved_canonical", afterDelete.single().cloudId)
        } finally {
            roomDb.close()
        }
    }
}
