package com.example.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.database.entities.AdminAuditLogEntity
import com.example.database.entities.DocumentDraftEntity
import com.example.database.entities.EducationActivityEntity
import com.example.database.entities.HolidayEntity
import com.example.database.entities.MonthlyAbsenteeDetailEntity
import com.example.database.entities.MonthlyAbsenteeReportEntity
import com.example.database.entities.NoticeEntity
import com.example.database.entities.PersonalNoteEntity
import com.example.database.entities.SalaryRecordEntity
import com.example.database.entities.SchoolProfileEntity
import com.example.database.entities.StaffEntity
import com.example.database.entities.TeacherProfileEntity
import com.example.database.entities.TeacherTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherProfileDao {
    @Query("SELECT * FROM teacher_profile WHERE id = 1 LIMIT 1")
    fun getTeacherProfile(): Flow<TeacherProfileEntity?>

    @Query("SELECT * FROM teacher_profile WHERE id = 1 LIMIT 1")
    suspend fun getTeacherProfileOnce(): TeacherProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTeacherProfile(profile: TeacherProfileEntity)

    @Query("UPDATE teacher_profile SET daPercent = :newDa WHERE id = 1")
    suspend fun updateDaPercent(newDa: Double)
}

@Dao
interface SchoolProfileDao {
    @Query("SELECT * FROM school_profile WHERE id = 1 LIMIT 1")
    fun getSchoolProfile(): Flow<SchoolProfileEntity?>

    @Query("SELECT * FROM school_profile WHERE id = 1 LIMIT 1")
    suspend fun getSchoolProfileOnce(): SchoolProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSchoolProfile(profile: SchoolProfileEntity)
}

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff_members ORDER BY id ASC")
    fun getAllStaff(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff_members WHERE isActive = 1 ORDER BY id ASC")
    fun getActiveStaff(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff_members WHERE id = :id LIMIT 1")
    suspend fun getStaffById(id: Long): StaffEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStaff(staffList: List<StaffEntity>)

    @Update
    suspend fun updateStaff(staff: StaffEntity)

    @Delete
    suspend fun deleteStaff(staff: StaffEntity)

    @Query("DELETE FROM staff_members WHERE id = :id")
    suspend fun deleteStaffById(id: Long)

    @Query("UPDATE staff_members SET daPercent = :newDa")
    suspend fun updateStaffDaPercent(newDa: Double)
}

@Dao
interface AbsenteeDao {
    @Query("SELECT * FROM monthly_absentee_reports ORDER BY year DESC, month DESC")
    fun getAllReports(): Flow<List<MonthlyAbsenteeReportEntity>>

    @Query("SELECT * FROM monthly_absentee_reports WHERE month = :month AND year = :year LIMIT 1")
    suspend fun getReportByMonthYear(month: Int, year: Int): MonthlyAbsenteeReportEntity?

    @Query("SELECT * FROM monthly_absentee_reports WHERE id = :reportId LIMIT 1")
    suspend fun getReportById(reportId: Long): MonthlyAbsenteeReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: MonthlyAbsenteeReportEntity): Long

    @Update
    suspend fun updateReport(report: MonthlyAbsenteeReportEntity)

    @Delete
    suspend fun deleteReport(report: MonthlyAbsenteeReportEntity)

    @Query("SELECT * FROM monthly_absentee_details WHERE reportId = :reportId ORDER BY id ASC")
    fun getDetailsForReport(reportId: Long): Flow<List<MonthlyAbsenteeDetailEntity>>

    @Query("SELECT * FROM monthly_absentee_details WHERE reportId = :reportId ORDER BY id ASC")
    suspend fun getDetailsForReportOnce(reportId: Long): List<MonthlyAbsenteeDetailEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: List<MonthlyAbsenteeDetailEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetail(detail: MonthlyAbsenteeDetailEntity): Long

    @Update
    suspend fun updateDetail(detail: MonthlyAbsenteeDetailEntity)

    @Query("DELETE FROM monthly_absentee_details WHERE reportId = :reportId")
    suspend fun deleteDetailsForReport(reportId: Long)
}

@Dao
interface SalaryDao {
    @Query("SELECT * FROM salary_records ORDER BY year DESC, month DESC, timestamp DESC")
    fun getAllSalaryRecords(): Flow<List<SalaryRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalaryRecord(record: SalaryRecordEntity): Long

    @Delete
    suspend fun deleteSalaryRecord(record: SalaryRecordEntity)
}

@Dao
interface ContentDao {
    // Drafts
    @Query("SELECT * FROM document_drafts ORDER BY lastUpdated DESC")
    fun getAllDrafts(): Flow<List<DocumentDraftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: DocumentDraftEntity): Long

    @Delete
    suspend fun deleteDraft(draft: DocumentDraftEntity)

    // Notices
    @Query("SELECT * FROM notices ORDER BY timestamp DESC")
    fun getAllNotices(): Flow<List<NoticeEntity>>

    @Query("SELECT * FROM notices ORDER BY timestamp DESC")
    suspend fun getAllNoticesOnce(): List<NoticeEntity>

    @Query("SELECT * FROM notices WHERE isFeatured = 1 ORDER BY timestamp DESC LIMIT 5")
    fun getFeaturedNotices(): Flow<List<NoticeEntity>>

    @Query("SELECT * FROM notices WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    fun getBookmarkedNotices(): Flow<List<NoticeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotices(notices: List<NoticeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotice(notice: NoticeEntity): Long

    @Update
    suspend fun updateNotice(notice: NoticeEntity)

    @Delete
    suspend fun deleteNotice(notice: NoticeEntity)

    // Holidays
    @Query("SELECT * FROM holidays ORDER BY year ASC, month ASC, id ASC")
    fun getAllHolidays(): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays WHERE month = :month ORDER BY id ASC")
    fun getHolidaysByMonth(month: Int): Flow<List<HolidayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolidays(holidays: List<HolidayEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoliday(holiday: HolidayEntity): Long

    @Update
    suspend fun updateHoliday(holiday: HolidayEntity)

    @Delete
    suspend fun deleteHoliday(holiday: HolidayEntity)

    // Notes
    @Query("SELECT * FROM personal_notes ORDER BY isPinned DESC, timestamp DESC")
    fun getAllNotes(): Flow<List<PersonalNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNote(note: PersonalNoteEntity): Long

    @Delete
    suspend fun deleteNote(note: PersonalNoteEntity)

    // Tasks
    @Query("SELECT * FROM teacher_tasks ORDER BY isCompleted ASC, timestamp DESC")
    fun getAllTasks(): Flow<List<TeacherTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTask(task: TeacherTaskEntity): Long

    @Update
    suspend fun updateTask(task: TeacherTaskEntity)

    @Delete
    suspend fun deleteTask(task: TeacherTaskEntity)

    // Audit logs
    @Query("SELECT * FROM admin_audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAuditLogs(): Flow<List<AdminAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AdminAuditLogEntity)

    // Educational Activities & Campaigns
    @Query("SELECT * FROM education_activities WHERE isActive = 1 ORDER BY timestamp DESC")
    fun getAllActivities(): Flow<List<EducationActivityEntity>>

    @Query("SELECT * FROM education_activities WHERE isActive = 1 ORDER BY timestamp DESC")
    suspend fun getAllActivitiesOnce(): List<EducationActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: EducationActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllActivities(activities: List<EducationActivityEntity>)

    @Update
    suspend fun updateActivity(activity: EducationActivityEntity)

    @Delete
    suspend fun deleteActivity(activity: EducationActivityEntity)

    // Quick Portals & Links
    @Query("SELECT * FROM quick_portals WHERE isActive = 1 ORDER BY id ASC")
    fun getAllPortals(): Flow<List<com.example.database.entities.QuickPortalEntity>>

    @Query("SELECT * FROM quick_portals WHERE isActive = 1 ORDER BY id ASC")
    suspend fun getAllPortalsOnce(): List<com.example.database.entities.QuickPortalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortal(portal: com.example.database.entities.QuickPortalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPortals(portals: List<com.example.database.entities.QuickPortalEntity>)

    @Update
    suspend fun updatePortal(portal: com.example.database.entities.QuickPortalEntity)

    @Delete
    suspend fun deletePortal(portal: com.example.database.entities.QuickPortalEntity)

    // Atomic Cloud Sync Transactions
    @Query("DELETE FROM notices")
    suspend fun clearAllNotices()

    @Transaction
    suspend fun syncNoticesFromCloud(noticesList: List<NoticeEntity>) {
        val existing = getAllNoticesOnce()
        val merged = noticesList.map { incoming ->
            val prev = existing.find {
                (incoming.orderNumber.isNotBlank() && it.orderNumber == incoming.orderNumber) ||
                    it.title.trim().equals(incoming.title.trim(), ignoreCase = true)
            }
            if (prev != null) incoming.copy(isBookmarked = prev.isBookmarked) else incoming
        }
        val existingSig = existing.map {
            listOf(it.title, it.summary, it.category, it.issueDate, it.department, it.orderNumber, it.officialUrl, it.pdfUrl, it.imageUrl, it.isFeatured, it.isBookmarked)
        }.sortedBy { it[0].toString() }
        val newSig = merged.map {
            listOf(it.title, it.summary, it.category, it.issueDate, it.department, it.orderNumber, it.officialUrl, it.pdfUrl, it.imageUrl, it.isFeatured, it.isBookmarked)
        }.sortedBy { it[0].toString() }
        if (existingSig == newSig) return

        clearAllNotices()
        if (merged.isNotEmpty()) {
            insertNotices(merged)
        }
    }

    @Query("SELECT * FROM holidays ORDER BY year ASC, month ASC, id ASC")
    suspend fun getAllHolidaysOnce(): List<HolidayEntity>

    @Query("DELETE FROM holidays")
    suspend fun clearAllHolidays()

    @Transaction
    suspend fun syncHolidaysFromCloud(holidaysList: List<HolidayEntity>) {
        val existing = getAllHolidaysOnce()
        val existingSig = existing.map { listOf(it.name, it.dateStr, it.dayOfWeek, it.month, it.year, it.type, it.durationDays, it.description) }.sortedBy { it[0].toString() }
        val newSig = holidaysList.map { listOf(it.name, it.dateStr, it.dayOfWeek, it.month, it.year, it.type, it.durationDays, it.description) }.sortedBy { it[0].toString() }
        if (existingSig == newSig) return

        clearAllHolidays()
        if (holidaysList.isNotEmpty()) {
            insertHolidays(holidaysList)
        }
    }

    @Query("DELETE FROM education_activities")
    suspend fun clearAllActivities()

    @Transaction
    suspend fun syncActivitiesFromCloud(activitiesList: List<EducationActivityEntity>) {
        val existing = getAllActivitiesOnce()
        val existingSig = existing.map { listOf(it.title, it.description, it.category, it.linkUrl, it.orderNumber, it.isActive) }.sortedBy { it[0].toString() }
        val newSig = activitiesList.map { listOf(it.title, it.description, it.category, it.linkUrl, it.orderNumber, it.isActive) }.sortedBy { it[0].toString() }
        if (existingSig == newSig) return

        clearAllActivities()
        if (activitiesList.isNotEmpty()) {
            insertAllActivities(activitiesList)
        }
    }

    @Query("DELETE FROM quick_portals")
    suspend fun clearAllPortals()

    @Transaction
    suspend fun syncPortalsFromCloud(portalsList: List<com.example.database.entities.QuickPortalEntity>) {
        val existing = getAllPortalsOnce()
        val existingSig = existing.map { listOf(it.title, it.subtitle, it.url, it.badge, it.colorHex, it.isActive) }.sortedBy { it[0].toString() }
        val newSig = portalsList.map { listOf(it.title, it.subtitle, it.url, it.badge, it.colorHex, it.isActive) }.sortedBy { it[0].toString() }
        if (existingSig == newSig) return

        clearAllPortals()
        if (portalsList.isNotEmpty()) {
            insertAllPortals(portalsList)
        }
    }

    // -------------------------------------------------------------
    // PROMO BANNERS
    // -------------------------------------------------------------
    @Query("SELECT * FROM promo_banners ORDER BY orderIndex ASC")
    fun getAllBanners(): Flow<List<com.example.database.entities.PromoBannerEntity>>

    @Query("SELECT * FROM promo_banners ORDER BY orderIndex ASC")
    suspend fun getAllBannersOnce(): List<com.example.database.entities.PromoBannerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanner(banner: com.example.database.entities.PromoBannerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBanners(banners: List<com.example.database.entities.PromoBannerEntity>)

    @Update
    suspend fun updateBanner(banner: com.example.database.entities.PromoBannerEntity)

    @Delete
    suspend fun deleteBanner(banner: com.example.database.entities.PromoBannerEntity)

    @Query("DELETE FROM promo_banners")
    suspend fun clearAllBanners()

    @Transaction
    suspend fun syncBannersFromCloud(bannersList: List<com.example.database.entities.PromoBannerEntity>) {
        val existing = getAllBannersOnce()
        val existingSig = existing.map { listOf(it.bannerId, it.title, it.description, it.category, it.imageUrl, it.destinationUrl, it.buttonText, it.orderIndex, it.isActive) }.sortedBy { it[0].toString() }
        val newSig = bannersList.map { listOf(it.bannerId, it.title, it.description, it.category, it.imageUrl, it.destinationUrl, it.buttonText, it.orderIndex, it.isActive) }.sortedBy { it[0].toString() }
        if (existingSig == newSig) return

        clearAllBanners()
        if (bannersList.isNotEmpty()) {
            insertAllBanners(bannersList)
        }
    }

    // -------------------------------------------------------------
    // SCHOOL TIMINGS
    // -------------------------------------------------------------
    @Query("SELECT * FROM school_timings WHERE id = 1 LIMIT 1")
    fun getSchoolTiming(): Flow<com.example.database.entities.SchoolTimingEntity?>

    @Query("SELECT * FROM school_timings WHERE id = 1 LIMIT 1")
    suspend fun getSchoolTimingOnce(): com.example.database.entities.SchoolTimingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSchoolTiming(timing: com.example.database.entities.SchoolTimingEntity)

    // -------------------------------------------------------------
    // VIDEO TUTORIALS
    // -------------------------------------------------------------
    @Query("SELECT * FROM video_tutorials ORDER BY isPinned DESC, displayOrder ASC, timestamp DESC")
    fun getAllVideos(): Flow<List<com.example.database.entities.VideoTutorialEntity>>

    @Query("SELECT * FROM video_tutorials ORDER BY isPinned DESC, displayOrder ASC, timestamp DESC")
    suspend fun getAllVideosOnce(): List<com.example.database.entities.VideoTutorialEntity>

    @Query("SELECT * FROM video_tutorials WHERE isFeaturedOnHome = 1 OR isPinned = 1 ORDER BY isPinned DESC, displayOrder ASC, timestamp DESC")
    fun getFeaturedHomeVideos(): Flow<List<com.example.database.entities.VideoTutorialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: com.example.database.entities.VideoTutorialEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllVideos(videos: List<com.example.database.entities.VideoTutorialEntity>)

    @Update
    suspend fun updateVideo(video: com.example.database.entities.VideoTutorialEntity)

    @Delete
    suspend fun deleteVideo(video: com.example.database.entities.VideoTutorialEntity)

    @Query("DELETE FROM video_tutorials")
    suspend fun clearAllVideos()

    @Query("DELETE FROM video_tutorials WHERE id NOT IN (SELECT MIN(id) FROM video_tutorials GROUP BY CASE WHEN youtubeId != '' THEN youtubeId ELSE youtubeUrl END)")
    suspend fun removeDuplicateVideos()

    @Query("DELETE FROM video_tutorials WHERE duration = 'यूट्यूब' AND isPinned = 0 AND isFeaturedOnHome = 0 AND id NOT IN (SELECT id FROM video_tutorials WHERE duration = 'यूट्यूब' AND isPinned = 0 AND isFeaturedOnHome = 0 ORDER BY timestamp DESC LIMIT 50)")
    suspend fun pruneOldRssVideos()

    @Transaction
    suspend fun syncVideosFromCloud(videosList: List<com.example.database.entities.VideoTutorialEntity>) {
        val existing = getAllVideosOnce()
        val matchedIds = mutableSetOf<Long>()
        val toInsert = mutableListOf<com.example.database.entities.VideoTutorialEntity>()
        for (v in videosList) {
            val match = existing.find { local ->
                local.id !in matchedIds && (
                    (v.id > 0 && local.id == v.id) ||
                    (v.youtubeId.isNotBlank() && local.youtubeId == v.youtubeId) ||
                    (v.youtubeUrl.isNotBlank() && local.youtubeUrl.trim() == v.youtubeUrl.trim()) ||
                    (local.title.trim().equals(v.title.trim(), ignoreCase = true))
                )
            }
            if (match != null) {
                matchedIds.add(match.id)
                val changed = match.title != v.title ||
                    match.category != v.category ||
                    match.youtubeUrl != v.youtubeUrl ||
                    match.youtubeId != v.youtubeId ||
                    match.thumbnailUrl != v.thumbnailUrl ||
                    match.duration != v.duration ||
                    match.description != v.description ||
                    match.isPinned != v.isPinned ||
                    match.isFeaturedOnHome != v.isFeaturedOnHome ||
                    match.displayOrder != v.displayOrder
                if (changed) {
                    updateVideo(v.copy(id = match.id))
                }
            } else {
                toInsert.add(v.copy(id = 0L))
            }
        }
        for (local in existing) {
            if (local.id !in matchedIds && local.duration != "यूट्यूब") {
                deleteVideo(local)
            }
        }
        if (toInsert.isNotEmpty()) {
            insertAllVideos(toInsert)
        }
        removeDuplicateVideos()
    }

    // -------------------------------------------------------------
    // DOWNLOAD RESOURCES & FORMS
    // -------------------------------------------------------------
    @Query("SELECT * FROM download_resources ORDER BY displayOrder ASC, timestamp DESC")
    fun getAllDownloads(): Flow<List<com.example.database.entities.DownloadResourceEntity>>

    @Query("SELECT * FROM download_resources ORDER BY displayOrder ASC, timestamp DESC")
    suspend fun getAllDownloadsOnce(): List<com.example.database.entities.DownloadResourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: com.example.database.entities.DownloadResourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDownloads(downloads: List<com.example.database.entities.DownloadResourceEntity>)

    @Update
    suspend fun updateDownload(download: com.example.database.entities.DownloadResourceEntity)

    @Delete
    suspend fun deleteDownload(download: com.example.database.entities.DownloadResourceEntity)

    @Query("DELETE FROM download_resources")
    suspend fun clearAllDownloads()

    @Transaction
    suspend fun syncDownloadsFromCloud(downloadsList: List<com.example.database.entities.DownloadResourceEntity>) {
        val normalizedIncoming = com.example.core.DownloadSyncHelper.deduplicate(
            downloadsList.map { item ->
                val resolvedCid = item.cloudId.ifBlank {
                    com.example.core.DownloadSyncHelper.resolveCanonicalDocId(item)
                }
                item.copy(cloudId = resolvedCid)
            }
        )
        val existing = getAllDownloadsOnce()
        val matchedLocalIds = mutableSetOf<Long>()
        val toInsert = mutableListOf<com.example.database.entities.DownloadResourceEntity>()

        for (incoming in normalizedIncoming) {
            val match = existing.find { local ->
                local.id !in matchedLocalIds && (
                    (incoming.cloudId.isNotBlank() && local.cloudId.equals(incoming.cloudId, ignoreCase = true)) ||
                        (incoming.pdfUrl.isNotBlank() && local.pdfUrl.trim().equals(incoming.pdfUrl.trim(), ignoreCase = true)) ||
                        (local.title.trim().equals(incoming.title.trim(), ignoreCase = true) &&
                            local.fileType.equals(incoming.fileType, ignoreCase = true))
                    )
            }
            if (match != null) {
                matchedLocalIds.add(match.id)
                val changed = match.cloudId != incoming.cloudId ||
                    match.title != incoming.title ||
                    match.category != incoming.category ||
                    match.fileType != incoming.fileType ||
                    match.fileSize != incoming.fileSize ||
                    match.pdfUrl != incoming.pdfUrl ||
                    match.publicationDate != incoming.publicationDate ||
                    match.orderNumber != incoming.orderNumber ||
                    match.description != incoming.description ||
                    match.isVisible != incoming.isVisible ||
                    match.displayOrder != incoming.displayOrder
                if (changed) {
                    updateDownload(
                        incoming.copy(
                            id = match.id,
                            localFilePath = match.localFilePath,
                            downloadCount = maxOf(match.downloadCount, incoming.downloadCount)
                        )
                    )
                }
            } else {
                toInsert.add(incoming.copy(id = 0L))
            }
        }

        // Remove any local records that no longer exist in cloud or were duplicates
        for (local in existing) {
            if (local.id !in matchedLocalIds) {
                deleteDownload(local)
            }
        }
        if (toInsert.isNotEmpty()) {
            insertAllDownloads(toInsert)
        }
    }

    // -------------------------------------------------------------
    // GOOGLE DRIVE CONFIG (ON/OFF Control)
    // -------------------------------------------------------------
    @Query("SELECT * FROM google_drive_config WHERE id = 1 LIMIT 1")
    fun getGoogleDriveConfig(): Flow<com.example.database.entities.GoogleDriveConfigEntity?>

    @Query("SELECT * FROM google_drive_config WHERE id = 1 LIMIT 1")
    suspend fun getGoogleDriveConfigOnce(): com.example.database.entities.GoogleDriveConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGoogleDriveConfig(config: com.example.database.entities.GoogleDriveConfigEntity)

    // -------------------------------------------------------------
    // WHATSAPP CONFIG
    // -------------------------------------------------------------
    @Query("SELECT * FROM whatsapp_links WHERE id = 1 LIMIT 1")
    fun getWhatsAppData(): Flow<com.example.database.entities.WhatsAppLinkConfigEntity?>

    @Query("SELECT * FROM whatsapp_links WHERE id = 1 LIMIT 1")
    suspend fun getWhatsAppDataOnce(): com.example.database.entities.WhatsAppLinkConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWhatsAppData(data: com.example.database.entities.WhatsAppLinkConfigEntity)

    // -------------------------------------------------------------
    // NEWS ARTICLES
    // -------------------------------------------------------------
    @Query("SELECT * FROM news_articles ORDER BY isPinned DESC, pinOrder ASC, timestamp DESC")
    fun getAllNews(): Flow<List<com.example.database.entities.NewsArticleEntity>>

    @Query("SELECT * FROM news_articles ORDER BY isPinned DESC, pinOrder ASC, timestamp DESC")
    suspend fun getAllNewsOnce(): List<com.example.database.entities.NewsArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(news: com.example.database.entities.NewsArticleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNews(newsList: List<com.example.database.entities.NewsArticleEntity>)

    @Update
    suspend fun updateNews(news: com.example.database.entities.NewsArticleEntity)

    @Delete
    suspend fun deleteNews(news: com.example.database.entities.NewsArticleEntity)

    @Query("DELETE FROM news_articles WHERE id NOT IN (SELECT MIN(id) FROM news_articles GROUP BY title)")
    suspend fun removeDuplicateNews()

    @Query("DELETE FROM news_articles WHERE category = 'लाइव RSS' AND isPinned = 0 AND id NOT IN (SELECT id FROM news_articles WHERE category = 'लाइव RSS' AND isPinned = 0 ORDER BY timestamp DESC LIMIT 50)")
    suspend fun pruneOldRssNews()

    @Transaction
    suspend fun syncNewsFromCloud(newsList: List<com.example.database.entities.NewsArticleEntity>) {
        val existing = getAllNewsOnce()
        val matchedIds = mutableSetOf<Long>()
        val toInsert = mutableListOf<com.example.database.entities.NewsArticleEntity>()
        for (n in newsList) {
            val match = existing.find { local ->
                local.id !in matchedIds && local.title.trim().equals(n.title.trim(), ignoreCase = true)
            }
            if (match != null) {
                matchedIds.add(match.id)
                val changed = match.title != n.title ||
                    match.summary != n.summary ||
                    match.content != n.content ||
                    match.category != n.category ||
                    match.publishedAt != n.publishedAt ||
                    match.source != n.source ||
                    match.sourceUrl != n.sourceUrl ||
                    match.imageUrl != n.imageUrl ||
                    match.isPinned != n.isPinned ||
                    match.pinOrder != n.pinOrder ||
                    match.isBreaking != n.isBreaking ||
                    match.isVisible != n.isVisible
                if (changed) {
                    updateNews(n.copy(id = match.id))
                }
            } else {
                toInsert.add(n.copy(id = 0L))
            }
        }
        for (local in existing) {
            if (local.id !in matchedIds && local.category != "लाइव RSS") {
                deleteNews(local)
            }
        }
        if (toInsert.isNotEmpty()) {
            insertAllNews(toInsert)
        }
        removeDuplicateNews()
    }

    // -------------------------------------------------------------
    // 14 FEATURE BOXES (मुख्य शिक्षक सुविधाएं)
    // -------------------------------------------------------------
    @Query("SELECT * FROM feature_boxes ORDER BY displayOrder ASC")
    fun getAllFeatureBoxes(): Flow<List<com.example.database.entities.FeatureBoxEntity>>

    @Query("SELECT * FROM feature_boxes ORDER BY displayOrder ASC")
    suspend fun getAllFeatureBoxesOnce(): List<com.example.database.entities.FeatureBoxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeatureBox(box: com.example.database.entities.FeatureBoxEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFeatureBoxes(boxes: List<com.example.database.entities.FeatureBoxEntity>)

    @Update
    suspend fun updateFeatureBox(box: com.example.database.entities.FeatureBoxEntity)

    @Delete
    suspend fun deleteFeatureBox(box: com.example.database.entities.FeatureBoxEntity)

    @Query("DELETE FROM feature_boxes")
    suspend fun clearAllFeatureBoxes()

    @Transaction
    suspend fun syncFeatureBoxesFromCloud(boxes: List<com.example.database.entities.FeatureBoxEntity>) {
        val existing = getAllFeatureBoxesOnce()
        val existingSig = existing.map { listOf(it.id, it.title, it.subtitle, it.iconType, it.destinationRoute, it.displayOrder, it.isVisible, it.isExternal, it.externalUrl) }.sortedBy { it[0].toString() }
        val newSig = boxes.map { listOf(it.id, it.title, it.subtitle, it.iconType, it.destinationRoute, it.displayOrder, it.isVisible, it.isExternal, it.externalUrl) }.sortedBy { it[0].toString() }
        if (existingSig == newSig) return

        clearAllFeatureBoxes()
        if (boxes.isNotEmpty()) {
            insertAllFeatureBoxes(boxes)
        }
    }

    // -------------------------------------------------------------
    // TOP 3 OFFICIAL LINKS (eShikshakosh, HRMS, UDISE+)
    // -------------------------------------------------------------
    @Query("SELECT * FROM top_three_links WHERE id = 1 LIMIT 1")
    fun getTopThreeLinks(): Flow<com.example.database.entities.TopThreeLinksEntity?>

    @Query("SELECT * FROM top_three_links WHERE id = 1 LIMIT 1")
    suspend fun getTopThreeLinksOnce(): com.example.database.entities.TopThreeLinksEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTopThreeLinks(links: com.example.database.entities.TopThreeLinksEntity)

    // -------------------------------------------------------------
    // FINANCIAL SERVICES BANNER
    // -------------------------------------------------------------
    @Query("SELECT * FROM financial_banner WHERE id = 1 LIMIT 1")
    fun getFinancialBanner(): Flow<com.example.database.entities.FinancialBannerEntity?>

    @Query("SELECT * FROM financial_banner WHERE id = 1 LIMIT 1")
    suspend fun getFinancialBannerOnce(): com.example.database.entities.FinancialBannerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFinancialBanner(banner: com.example.database.entities.FinancialBannerEntity)

    // -------------------------------------------------------------
    // WHATSAPP CHANNEL JOINING BANNER
    // -------------------------------------------------------------
    @Query("SELECT * FROM whatsapp_banner WHERE id = 1 LIMIT 1")
    fun getWhatsAppBanner(): Flow<com.example.database.entities.WhatsAppBannerEntity?>

    @Query("SELECT * FROM whatsapp_banner WHERE id = 1 LIMIT 1")
    suspend fun getWhatsAppBannerOnce(): com.example.database.entities.WhatsAppBannerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWhatsAppBanner(banner: com.example.database.entities.WhatsAppBannerEntity)

    // -------------------------------------------------------------
    // COMPLAINT OPTIONS (शिकायत निवारण)
    // -------------------------------------------------------------
    @Query("SELECT * FROM complaint_options ORDER BY displayOrder ASC, id ASC")
    fun getAllComplaintOptions(): Flow<List<com.example.database.entities.ComplaintOptionEntity>>

    @Query("SELECT * FROM complaint_options WHERE isVisible = 1 ORDER BY displayOrder ASC, id ASC")
    fun getActiveComplaintOptions(): Flow<List<com.example.database.entities.ComplaintOptionEntity>>

    @Query("SELECT * FROM complaint_options ORDER BY displayOrder ASC, id ASC")
    suspend fun getAllComplaintOptionsOnce(): List<com.example.database.entities.ComplaintOptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaintOption(option: com.example.database.entities.ComplaintOptionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllComplaintOptions(options: List<com.example.database.entities.ComplaintOptionEntity>)

    @Update
    suspend fun updateComplaintOption(option: com.example.database.entities.ComplaintOptionEntity)

    @Delete
    suspend fun deleteComplaintOption(option: com.example.database.entities.ComplaintOptionEntity)

    @Query("DELETE FROM complaint_options")
    suspend fun clearAllComplaintOptions()

    @Transaction
    suspend fun syncComplaintOptionsFromCloud(list: List<com.example.database.entities.ComplaintOptionEntity>) {
        val existing = getAllComplaintOptionsOnce()
        val existingSig = existing.map { listOf(it.title, it.description, it.phone, it.email, it.linkUrl, it.isVisible, it.displayOrder) }.sortedBy { it[0].toString() }
        val newSig = list.map { listOf(it.title, it.description, it.phone, it.email, it.linkUrl, it.isVisible, it.displayOrder) }.sortedBy { it[0].toString() }
        if (existingSig == newSig) return

        clearAllComplaintOptions()
        if (list.isNotEmpty()) {
            insertAllComplaintOptions(list)
        }
    }

    // -------------------------------------------------------------
    // HOME TOP LINKS (होम पेज लिंक्स)
    // -------------------------------------------------------------
    @Query("SELECT * FROM home_top_links ORDER BY displayOrder ASC, id ASC")
    fun getAllHomeTopLinks(): Flow<List<com.example.database.entities.HomeTopLinkEntity>>

    @Query("SELECT * FROM home_top_links WHERE isVisible = 1 ORDER BY displayOrder ASC, id ASC")
    fun getActiveHomeTopLinks(): Flow<List<com.example.database.entities.HomeTopLinkEntity>>

    @Query("SELECT * FROM home_top_links ORDER BY displayOrder ASC, id ASC")
    suspend fun getAllHomeTopLinksOnce(): List<com.example.database.entities.HomeTopLinkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeTopLink(link: com.example.database.entities.HomeTopLinkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHomeTopLinks(links: List<com.example.database.entities.HomeTopLinkEntity>)

    @Update
    suspend fun updateHomeTopLink(link: com.example.database.entities.HomeTopLinkEntity)

    @Delete
    suspend fun deleteHomeTopLink(link: com.example.database.entities.HomeTopLinkEntity)

    @Query("DELETE FROM home_top_links")
    suspend fun clearAllHomeTopLinks()

    @Transaction
    suspend fun syncHomeTopLinksFromCloud(list: List<com.example.database.entities.HomeTopLinkEntity>) {
        val existing = getAllHomeTopLinksOnce()
        val existingSig = existing.map { listOf(it.title, it.url, it.isVisible, it.displayOrder) }.sortedBy { it[0].toString() }
        val newSig = list.map { listOf(it.title, it.url, it.isVisible, it.displayOrder) }.sortedBy { it[0].toString() }
        if (existingSig == newSig) return

        clearAllHomeTopLinks()
        if (list.isNotEmpty()) {
            insertAllHomeTopLinks(list)
        }
    }

    // -------------------------------------------------------------
    // FINANCIAL SERVICES (लोन, कार्ड, निवेश)
    // -------------------------------------------------------------
    @Query("SELECT * FROM financial_services ORDER BY displayOrder ASC, id ASC")
    fun getAllFinancialServices(): Flow<List<com.example.database.entities.FinancialServiceEntity>>

    @Query("SELECT * FROM financial_services WHERE category = :category AND isVisible = 1 ORDER BY displayOrder ASC, id ASC")
    fun getActiveFinancialServicesByCategory(category: String): Flow<List<com.example.database.entities.FinancialServiceEntity>>

    @Query("SELECT * FROM financial_services WHERE category = :category ORDER BY displayOrder ASC, id ASC")
    fun getAllFinancialServicesByCategory(category: String): Flow<List<com.example.database.entities.FinancialServiceEntity>>

    @Query("SELECT * FROM financial_services ORDER BY displayOrder ASC, id ASC")
    suspend fun getAllFinancialServicesOnce(): List<com.example.database.entities.FinancialServiceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialService(service: com.example.database.entities.FinancialServiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFinancialServices(services: List<com.example.database.entities.FinancialServiceEntity>)

    @Update
    suspend fun updateFinancialService(service: com.example.database.entities.FinancialServiceEntity)

    @Delete
    suspend fun deleteFinancialService(service: com.example.database.entities.FinancialServiceEntity)

    @Query("DELETE FROM financial_services")
    suspend fun clearAllFinancialServices()

    @Transaction
    suspend fun syncFinancialServicesFromCloud(list: List<com.example.database.entities.FinancialServiceEntity>) {
        val existing = getAllFinancialServicesOnce()
        val existingSig = existing.map { listOf(it.category, it.title, it.description, it.applyUrl, it.isGoogleForm, it.buttonText, it.isVisible, it.displayOrder) }.sortedBy { it[1].toString() }
        val newSig = list.map { listOf(it.category, it.title, it.description, it.applyUrl, it.isGoogleForm, it.buttonText, it.isVisible, it.displayOrder) }.sortedBy { it[1].toString() }
        if (existingSig == newSig) return

        clearAllFinancialServices()
        if (list.isNotEmpty()) {
            insertAllFinancialServices(list)
        }
    }

    // -------------------------------------------------------------
    // YOUTUBE RSS FEEDS (यूट्यूब चैनल फीड्स)
    // -------------------------------------------------------------
    @Query("SELECT * FROM youtube_feeds ORDER BY id ASC")
    fun getAllYouTubeFeeds(): Flow<List<com.example.database.entities.YouTubeFeedEntity>>

    @Query("SELECT * FROM youtube_feeds WHERE isVisible = 1 ORDER BY id ASC")
    fun getActiveYouTubeFeeds(): Flow<List<com.example.database.entities.YouTubeFeedEntity>>

    @Query("SELECT * FROM youtube_feeds ORDER BY id ASC")
    suspend fun getAllYouTubeFeedsOnce(): List<com.example.database.entities.YouTubeFeedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertYouTubeFeed(feed: com.example.database.entities.YouTubeFeedEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllYouTubeFeeds(feeds: List<com.example.database.entities.YouTubeFeedEntity>)

    @Update
    suspend fun updateYouTubeFeed(feed: com.example.database.entities.YouTubeFeedEntity)

    @Delete
    suspend fun deleteYouTubeFeed(feed: com.example.database.entities.YouTubeFeedEntity)

    @Query("DELETE FROM youtube_feeds")
    suspend fun clearAllYouTubeFeeds()

    @Query("DELETE FROM youtube_feeds WHERE channelId = 'UCshikshakbihar123' OR feedUrl LIKE '%UCshikshakbihar123%'")
    suspend fun removeInvalidPlaceholderFeeds()

    @Transaction
    suspend fun syncYouTubeFeedsFromCloud(list: List<com.example.database.entities.YouTubeFeedEntity>) {
        val cleanedList = list.filterNot {
            it.channelId.equals("UCshikshakbihar123", ignoreCase = true) ||
                it.feedUrl.contains("UCshikshakbihar123", ignoreCase = true)
        }
        val existing = getAllYouTubeFeedsOnce()
        val existingSig = existing.map { listOf(it.channelTitle, it.feedUrl, it.channelId, it.isVisible) }.sortedBy { it[0].toString() }
        val newSig = cleanedList.map { listOf(it.channelTitle, it.feedUrl, it.channelId, it.isVisible) }.sortedBy { it[0].toString() }
        if (existingSig == newSig) return

        clearAllYouTubeFeeds()
        if (cleanedList.isNotEmpty()) {
            insertAllYouTubeFeeds(cleanedList)
        }
    }

    // -------------------------------------------------------------
    // NEWS RSS FEEDS (लाइव समाचार RSS फीड्स)
    // -------------------------------------------------------------
    @Query("SELECT * FROM news_rss_feeds ORDER BY id ASC")
    fun getAllNewsRssFeeds(): Flow<List<com.example.database.entities.NewsRssFeedEntity>>

    @Query("SELECT * FROM news_rss_feeds WHERE isVisible = 1 ORDER BY id ASC")
    fun getActiveNewsRssFeeds(): Flow<List<com.example.database.entities.NewsRssFeedEntity>>

    @Query("SELECT * FROM news_rss_feeds ORDER BY id ASC")
    suspend fun getAllNewsRssFeedsOnce(): List<com.example.database.entities.NewsRssFeedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNewsRssFeed(feed: com.example.database.entities.NewsRssFeedEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNewsRssFeeds(feeds: List<com.example.database.entities.NewsRssFeedEntity>)

    @Update
    suspend fun updateNewsRssFeed(feed: com.example.database.entities.NewsRssFeedEntity)

    @Delete
    suspend fun deleteNewsRssFeed(feed: com.example.database.entities.NewsRssFeedEntity)

    @Query("DELETE FROM news_rss_feeds")
    suspend fun clearAllNewsRssFeeds()

    @Transaction
    suspend fun syncNewsRssFeedsFromCloud(list: List<com.example.database.entities.NewsRssFeedEntity>) {
        val existing = getAllNewsRssFeedsOnce()
        val existingSig = existing.map { listOf(it.title, it.feedUrl, it.isVisible) }.sortedBy { it[0].toString() }
        val newSig = list.map { listOf(it.title, it.feedUrl, it.isVisible) }.sortedBy { it[0].toString() }
        if (existingSig == newSig) return

        clearAllNewsRssFeeds()
        if (list.isNotEmpty()) {
            insertAllNewsRssFeeds(list)
        }
    }
}


