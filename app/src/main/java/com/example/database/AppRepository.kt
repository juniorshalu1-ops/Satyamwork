package com.example.database

import android.content.Context
import com.example.database.entities.AdminAuditLogEntity
import com.example.database.entities.DocumentDraftEntity
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AppRepository(
    private val database: AppDatabase
) {
    private val ytRssMutex = Mutex()
    private val newsRssMutex = Mutex()
    // Teacher Profile
    val teacherProfile: Flow<TeacherProfileEntity?> = database.teacherProfileDao().getTeacherProfile()
    suspend fun saveTeacherProfile(profile: TeacherProfileEntity) = withContext(Dispatchers.IO) {
        database.teacherProfileDao().saveTeacherProfile(profile)
    }
    suspend fun getTeacherProfileOnce(): TeacherProfileEntity? = withContext(Dispatchers.IO) {
        database.teacherProfileDao().getTeacherProfileOnce()
    }

    // School Profile
    val schoolProfile: Flow<SchoolProfileEntity?> = database.schoolProfileDao().getSchoolProfile()
    suspend fun saveSchoolProfile(profile: SchoolProfileEntity) = withContext(Dispatchers.IO) {
        database.schoolProfileDao().saveSchoolProfile(profile)
    }
    suspend fun getSchoolProfileOnce(): SchoolProfileEntity? = withContext(Dispatchers.IO) {
        database.schoolProfileDao().getSchoolProfileOnce()
    }

    // Staff Directory
    val allStaff: Flow<List<StaffEntity>> = database.staffDao().getAllStaff()
    val activeStaff: Flow<List<StaffEntity>> = database.staffDao().getActiveStaff()
    suspend fun insertStaff(staff: StaffEntity) = withContext(Dispatchers.IO) {
        database.staffDao().insertStaff(staff)
    }
    suspend fun updateStaff(staff: StaffEntity) = withContext(Dispatchers.IO) {
        database.staffDao().updateStaff(staff)
    }
    suspend fun deleteStaff(staff: StaffEntity) = withContext(Dispatchers.IO) {
        database.staffDao().deleteStaff(staff)
    }

    // Absentee Reports
    val allReports: Flow<List<MonthlyAbsenteeReportEntity>> = database.absenteeDao().getAllReports()
    fun getDetailsForReport(reportId: Long): Flow<List<MonthlyAbsenteeDetailEntity>> =
        database.absenteeDao().getDetailsForReport(reportId)

    suspend fun getReportById(reportId: Long): MonthlyAbsenteeReportEntity? = withContext(Dispatchers.IO) {
        database.absenteeDao().getReportById(reportId)
    }

    suspend fun getDetailsForReportOnce(reportId: Long): List<MonthlyAbsenteeDetailEntity> = withContext(Dispatchers.IO) {
        database.absenteeDao().getDetailsForReportOnce(reportId)
    }

    suspend fun updateDetail(detail: MonthlyAbsenteeDetailEntity) = withContext(Dispatchers.IO) {
        database.absenteeDao().updateDetail(detail)
    }

    suspend fun insertDetail(detail: MonthlyAbsenteeDetailEntity) = withContext(Dispatchers.IO) {
        database.absenteeDao().insertDetail(detail)
    }

    suspend fun createMonthlyReport(
        month: Int,
        year: Int,
        workingDays: Int,
        copyFromPrevious: Boolean = true
    ): Long = withContext(Dispatchers.IO) {
        val school = database.schoolProfileDao().getSchoolProfileOnce() ?: SchoolProfileEntity()
        val existing = database.absenteeDao().getReportByMonthYear(month, year)
        val reportId = if (existing != null) {
            existing.id
        } else {
            val newReport = MonthlyAbsenteeReportEntity(
                month = month,
                year = year,
                totalWorkingDays = workingDays,
                schoolName = school.schoolName,
                udiseCode = school.udiseCode,
                district = school.district,
                block = school.block
            )
            database.absenteeDao().insertReport(newReport)
        }

        // Check if details already exist
        val existingDetails = database.absenteeDao().getDetailsForReportOnce(reportId)
        if (existingDetails.isEmpty()) {
            val staffList = database.staffDao().getAllStaff().firstOrNull() ?: emptyList()
            val newDetails = staffList.map { staff ->
                MonthlyAbsenteeDetailEntity(
                    reportId = reportId,
                    staffId = staff.id,
                    teacherName = staff.name,
                    designation = staff.designation,
                    employeeId = staff.employeeId,
                    basicPay = staff.basicPay,
                    daPercent = staff.daPercent,
                    hraCategory = staff.hraCategory,
                    joiningDate = staff.joiningDate,
                    workingDays = workingDays,
                    presentDays = workingDays, // default 100% present
                    clDays = 0,
                    elDays = 0,
                    medicalDays = 0,
                    maternityDays = 0,
                    paternityDays = 0,
                    trainingDays = 0,
                    officialDutyDays = 0,
                    electionDutyDays = 0,
                    censusDutyDays = 0,
                    otherLeaveDays = 0,
                    absentDays = 0
                )
            }
            if (newDetails.isNotEmpty()) {
                database.absenteeDao().insertDetails(newDetails)
            }
        }
        reportId
    }

    suspend fun deleteReport(report: MonthlyAbsenteeReportEntity) = withContext(Dispatchers.IO) {
        database.absenteeDao().deleteReport(report)
    }

    // Salary Records
    val allSalaryRecords: Flow<List<SalaryRecordEntity>> = database.salaryDao().getAllSalaryRecords()
    suspend fun saveSalaryRecord(record: SalaryRecordEntity) = withContext(Dispatchers.IO) {
        database.salaryDao().insertSalaryRecord(record)
    }
    suspend fun deleteSalaryRecord(record: SalaryRecordEntity) = withContext(Dispatchers.IO) {
        database.salaryDao().deleteSalaryRecord(record)
    }

    // Smart Documents
    val allDrafts: Flow<List<DocumentDraftEntity>> = database.contentDao().getAllDrafts()
    suspend fun saveDraft(draft: DocumentDraftEntity) = withContext(Dispatchers.IO) {
        database.contentDao().saveDraft(draft)
    }
    suspend fun deleteDraft(draft: DocumentDraftEntity) = withContext(Dispatchers.IO) {
        database.contentDao().deleteDraft(draft)
    }

    // Notices
    val allNotices: Flow<List<NoticeEntity>> = database.contentDao().getAllNotices()
    suspend fun allNoticesOnce(): List<NoticeEntity> = withContext(Dispatchers.IO) {
        database.contentDao().getAllNoticesOnce()
    }
    val featuredNotices: Flow<List<NoticeEntity>> = database.contentDao().getFeaturedNotices()
    val bookmarkedNotices: Flow<List<NoticeEntity>> = database.contentDao().getBookmarkedNotices()
    suspend fun toggleBookmarkNotice(notice: NoticeEntity) = withContext(Dispatchers.IO) {
        database.contentDao().updateNotice(notice.copy(isBookmarked = !notice.isBookmarked))
    }
    suspend fun insertNotice(notice: NoticeEntity) = withContext(Dispatchers.IO) {
        database.contentDao().insertNotice(notice)
    }
    suspend fun updateNotice(notice: NoticeEntity) = withContext(Dispatchers.IO) {
        database.contentDao().updateNotice(notice)
    }
    suspend fun deleteNotice(notice: NoticeEntity) = withContext(Dispatchers.IO) {
        database.contentDao().deleteNotice(notice)
    }

    // Holidays
    val allHolidays: Flow<List<HolidayEntity>> = database.contentDao().getAllHolidays()
    suspend fun allHolidaysOnce(): List<HolidayEntity> = withContext(Dispatchers.IO) {
        database.contentDao().getAllHolidays().firstOrNull() ?: emptyList()
    }
    fun getHolidaysByMonth(month: Int): Flow<List<HolidayEntity>> =
        database.contentDao().getHolidaysByMonth(month)
    suspend fun insertHoliday(holiday: HolidayEntity) = withContext(Dispatchers.IO) {
        database.contentDao().insertHoliday(holiday)
    }
    suspend fun updateHoliday(holiday: HolidayEntity) = withContext(Dispatchers.IO) {
        database.contentDao().updateHoliday(holiday)
    }
    suspend fun deleteHoliday(holiday: HolidayEntity) = withContext(Dispatchers.IO) {
        database.contentDao().deleteHoliday(holiday)
    }

    // Notes & Tasks
    val allNotes: Flow<List<PersonalNoteEntity>> = database.contentDao().getAllNotes()
    suspend fun saveNote(note: PersonalNoteEntity) = withContext(Dispatchers.IO) {
        database.contentDao().saveNote(note)
    }
    suspend fun deleteNote(note: PersonalNoteEntity) = withContext(Dispatchers.IO) {
        database.contentDao().deleteNote(note)
    }

    val allTasks: Flow<List<TeacherTaskEntity>> = database.contentDao().getAllTasks()
    suspend fun saveTask(task: TeacherTaskEntity) = withContext(Dispatchers.IO) {
        database.contentDao().saveTask(task)
    }
    suspend fun updateTask(task: TeacherTaskEntity) = withContext(Dispatchers.IO) {
        database.contentDao().updateTask(task)
    }
    suspend fun deleteTask(task: TeacherTaskEntity) = withContext(Dispatchers.IO) {
        database.contentDao().deleteTask(task)
    }

    // Audit Logs
    val auditLogs: Flow<List<AdminAuditLogEntity>> = database.contentDao().getAuditLogs()
    suspend fun logAdminAction(adminUser: String, action: String, details: String) = withContext(Dispatchers.IO) {
        database.contentDao().insertAuditLog(
            AdminAuditLogEntity(
                adminUser = adminUser,
                action = action,
                details = details
            )
        )
    }

    // Educational Activities & Campaigns
    val allActivities: Flow<List<com.example.database.entities.EducationActivityEntity>> = database.contentDao().getAllActivities()
    suspend fun allActivitiesOnce(): List<com.example.database.entities.EducationActivityEntity> = withContext(Dispatchers.IO) {
        database.contentDao().getAllActivitiesOnce()
    }
    suspend fun insertActivity(activity: com.example.database.entities.EducationActivityEntity) = withContext(Dispatchers.IO) {
        database.contentDao().insertActivity(activity)
    }
    suspend fun updateActivity(activity: com.example.database.entities.EducationActivityEntity) = withContext(Dispatchers.IO) {
        database.contentDao().updateActivity(activity)
    }
    suspend fun deleteActivity(activity: com.example.database.entities.EducationActivityEntity) = withContext(Dispatchers.IO) {
        database.contentDao().deleteActivity(activity)
    }

    // Quick Portals & Useful Links
    val allPortals: Flow<List<com.example.database.entities.QuickPortalEntity>> = database.contentDao().getAllPortals()
    suspend fun allPortalsOnce(): List<com.example.database.entities.QuickPortalEntity> = withContext(Dispatchers.IO) {
        database.contentDao().getAllPortalsOnce()
    }
    suspend fun insertPortal(portal: com.example.database.entities.QuickPortalEntity) = withContext(Dispatchers.IO) {
        database.contentDao().insertPortal(portal)
    }
    suspend fun updatePortal(portal: com.example.database.entities.QuickPortalEntity) = withContext(Dispatchers.IO) {
        database.contentDao().updatePortal(portal)
    }
    suspend fun deletePortal(portal: com.example.database.entities.QuickPortalEntity) = withContext(Dispatchers.IO) {
        database.contentDao().deletePortal(portal)
    }

    // Atomic sync from cloud
    suspend fun syncNoticesFromCloud(noticesList: List<NoticeEntity>) = withContext(Dispatchers.IO) {
        database.contentDao().syncNoticesFromCloud(noticesList)
    }

    suspend fun syncHolidaysFromCloud(holidaysList: List<HolidayEntity>) = withContext(Dispatchers.IO) {
        database.contentDao().syncHolidaysFromCloud(holidaysList)
    }

    suspend fun syncActivitiesFromCloud(activitiesList: List<com.example.database.entities.EducationActivityEntity>) = withContext(Dispatchers.IO) {
        database.contentDao().syncActivitiesFromCloud(activitiesList)
    }

    suspend fun syncPortalsFromCloud(portalsList: List<com.example.database.entities.QuickPortalEntity>) = withContext(Dispatchers.IO) {
        database.contentDao().syncPortalsFromCloud(portalsList)
    }

    // -------------------------------------------------------------
    // PROMOTIONAL BANNERS
    // -------------------------------------------------------------
    val allBanners: Flow<List<com.example.database.entities.PromoBannerEntity>> = database.contentDao().getAllBanners()
    suspend fun allBannersOnce(): List<com.example.database.entities.PromoBannerEntity> = withContext(Dispatchers.IO) {
        database.contentDao().getAllBannersOnce()
    }
    suspend fun insertBanner(banner: com.example.database.entities.PromoBannerEntity) = withContext(Dispatchers.IO) {
        database.contentDao().insertBanner(banner)
    }
    suspend fun updateBanner(banner: com.example.database.entities.PromoBannerEntity) = withContext(Dispatchers.IO) {
        database.contentDao().updateBanner(banner)
    }
    suspend fun deleteBanner(banner: com.example.database.entities.PromoBannerEntity) = withContext(Dispatchers.IO) {
        database.contentDao().deleteBanner(banner)
    }
    suspend fun syncBannersFromCloud(bannersList: List<com.example.database.entities.PromoBannerEntity>) = withContext(Dispatchers.IO) {
        database.contentDao().syncBannersFromCloud(bannersList)
    }

    // -------------------------------------------------------------
    // SCHOOL TIMINGS
    // -------------------------------------------------------------
    val schoolTiming: Flow<com.example.database.entities.SchoolTimingEntity?> = database.contentDao().getSchoolTiming()
    suspend fun getSchoolTimingOnce(): com.example.database.entities.SchoolTimingEntity? = withContext(Dispatchers.IO) {
        database.contentDao().getSchoolTimingOnce()
    }
    suspend fun saveSchoolTiming(timing: com.example.database.entities.SchoolTimingEntity) = withContext(Dispatchers.IO) {
        database.contentDao().saveSchoolTiming(timing)
    }

    // -------------------------------------------------------------
    // VIDEO TUTORIALS
    // -------------------------------------------------------------
    val allVideos: Flow<List<com.example.database.entities.VideoTutorialEntity>> = database.contentDao().getAllVideos()
    val featuredHomeVideos: Flow<List<com.example.database.entities.VideoTutorialEntity>> = database.contentDao().getFeaturedHomeVideos()
    suspend fun allVideosOnce(): List<com.example.database.entities.VideoTutorialEntity> = withContext(Dispatchers.IO) {
        database.contentDao().getAllVideosOnce()
    }
    suspend fun insertVideo(video: com.example.database.entities.VideoTutorialEntity) = withContext(Dispatchers.IO) {
        database.contentDao().insertVideo(video)
    }
    suspend fun updateVideo(video: com.example.database.entities.VideoTutorialEntity) = withContext(Dispatchers.IO) {
        database.contentDao().updateVideo(video)
    }
    suspend fun deleteVideo(video: com.example.database.entities.VideoTutorialEntity) = withContext(Dispatchers.IO) {
        database.contentDao().deleteVideo(video)
    }
    suspend fun syncVideosFromCloud(videosList: List<com.example.database.entities.VideoTutorialEntity>) = withContext(Dispatchers.IO) {
        database.contentDao().syncVideosFromCloud(videosList)
    }

    // -------------------------------------------------------------
    // DOWNLOAD RESOURCES
    // -------------------------------------------------------------
    val allDownloads: Flow<List<com.example.database.entities.DownloadResourceEntity>> = database.contentDao().getAllDownloads()
    suspend fun allDownloadsOnce(): List<com.example.database.entities.DownloadResourceEntity> = withContext(Dispatchers.IO) {
        database.contentDao().getAllDownloadsOnce()
    }
    suspend fun insertDownload(download: com.example.database.entities.DownloadResourceEntity) = withContext(Dispatchers.IO) {
        database.contentDao().insertDownload(download)
    }
    suspend fun updateDownload(download: com.example.database.entities.DownloadResourceEntity) = withContext(Dispatchers.IO) {
        database.contentDao().updateDownload(download)
    }
    suspend fun deleteDownload(download: com.example.database.entities.DownloadResourceEntity) = withContext(Dispatchers.IO) {
        database.contentDao().deleteDownload(download)
    }
    suspend fun syncDownloadsFromCloud(downloadsList: List<com.example.database.entities.DownloadResourceEntity>) = withContext(Dispatchers.IO) {
        database.contentDao().syncDownloadsFromCloud(downloadsList)
    }

    // -------------------------------------------------------------
    // GOOGLE DRIVE CONFIG (ON/OFF Control)
    // -------------------------------------------------------------
    val googleDriveConfig: Flow<com.example.database.entities.GoogleDriveConfigEntity?> = database.contentDao().getGoogleDriveConfig()
    suspend fun getGoogleDriveConfigOnce(): com.example.database.entities.GoogleDriveConfigEntity? = withContext(Dispatchers.IO) {
        database.contentDao().getGoogleDriveConfigOnce()
    }
    suspend fun saveGoogleDriveConfig(config: com.example.database.entities.GoogleDriveConfigEntity) = withContext(Dispatchers.IO) {
        database.contentDao().saveGoogleDriveConfig(config)
    }

    // -------------------------------------------------------------
    // WHATSAPP CONFIG
    // -------------------------------------------------------------
    val whatsAppData: Flow<com.example.database.entities.WhatsAppLinkConfigEntity?> = database.contentDao().getWhatsAppData()
    suspend fun getWhatsAppDataOnce(): com.example.database.entities.WhatsAppLinkConfigEntity? = withContext(Dispatchers.IO) {
        database.contentDao().getWhatsAppDataOnce()
    }
    suspend fun saveWhatsAppData(data: com.example.database.entities.WhatsAppLinkConfigEntity) = withContext(Dispatchers.IO) {
        database.contentDao().saveWhatsAppData(data)
    }

    // -------------------------------------------------------------
    // NEWS ARTICLES
    // -------------------------------------------------------------
    val allNews: Flow<List<com.example.database.entities.NewsArticleEntity>> = database.contentDao().getAllNews()
    suspend fun allNewsOnce(): List<com.example.database.entities.NewsArticleEntity> = withContext(Dispatchers.IO) {
        database.contentDao().getAllNewsOnce()
    }
    suspend fun insertNews(news: com.example.database.entities.NewsArticleEntity) = withContext(Dispatchers.IO) {
        database.contentDao().insertNews(news)
    }
    suspend fun updateNews(news: com.example.database.entities.NewsArticleEntity) = withContext(Dispatchers.IO) {
        database.contentDao().updateNews(news)
    }
    suspend fun deleteNews(news: com.example.database.entities.NewsArticleEntity) = withContext(Dispatchers.IO) {
        database.contentDao().deleteNews(news)
    }
    suspend fun syncNewsFromCloud(newsList: List<com.example.database.entities.NewsArticleEntity>) = withContext(Dispatchers.IO) {
        database.contentDao().syncNewsFromCloud(newsList)
    }

    // -------------------------------------------------------------
    // 14 FEATURE BOXES (मुख्य शिक्षक सुविधाएं)
    // -------------------------------------------------------------
    val allFeatureBoxes: Flow<List<com.example.database.entities.FeatureBoxEntity>> = database.contentDao().getAllFeatureBoxes()
    suspend fun getAllFeatureBoxesOnce(): List<com.example.database.entities.FeatureBoxEntity> = withContext(Dispatchers.IO) {
        database.contentDao().getAllFeatureBoxesOnce()
    }
    suspend fun insertFeatureBox(box: com.example.database.entities.FeatureBoxEntity) = withContext(Dispatchers.IO) {
        database.contentDao().insertFeatureBox(box)
    }
    suspend fun updateFeatureBox(box: com.example.database.entities.FeatureBoxEntity) = withContext(Dispatchers.IO) {
        database.contentDao().updateFeatureBox(box)
    }
    suspend fun deleteFeatureBox(box: com.example.database.entities.FeatureBoxEntity) = withContext(Dispatchers.IO) {
        database.contentDao().deleteFeatureBox(box)
    }
    suspend fun syncFeatureBoxesFromCloud(boxes: List<com.example.database.entities.FeatureBoxEntity>) = withContext(Dispatchers.IO) {
        database.contentDao().syncFeatureBoxesFromCloud(boxes)
    }

    // -------------------------------------------------------------
    // TOP 3 OFFICIAL LINKS (eShikshakosh, HRMS, UDISE+)
    // -------------------------------------------------------------
    val topThreeLinks: Flow<com.example.database.entities.TopThreeLinksEntity?> = database.contentDao().getTopThreeLinks()
    suspend fun getTopThreeLinksOnce(): com.example.database.entities.TopThreeLinksEntity? = withContext(Dispatchers.IO) {
        database.contentDao().getTopThreeLinksOnce()
    }
    suspend fun saveTopThreeLinks(links: com.example.database.entities.TopThreeLinksEntity) = withContext(Dispatchers.IO) {
        database.contentDao().saveTopThreeLinks(links)
    }

    // -------------------------------------------------------------
    // FINANCIAL SERVICES BANNER
    // -------------------------------------------------------------
    val financialBanner: Flow<com.example.database.entities.FinancialBannerEntity?> = database.contentDao().getFinancialBanner()
    suspend fun getFinancialBannerOnce(): com.example.database.entities.FinancialBannerEntity? = withContext(Dispatchers.IO) {
        database.contentDao().getFinancialBannerOnce()
    }
    suspend fun saveFinancialBanner(banner: com.example.database.entities.FinancialBannerEntity) = withContext(Dispatchers.IO) {
        database.contentDao().saveFinancialBanner(banner)
    }

    // -------------------------------------------------------------
    // WHATSAPP CHANNEL JOINING BANNER
    // -------------------------------------------------------------
    val whatsAppBanner: Flow<com.example.database.entities.WhatsAppBannerEntity?> = database.contentDao().getWhatsAppBanner()
    suspend fun getWhatsAppBannerOnce(): com.example.database.entities.WhatsAppBannerEntity? = withContext(Dispatchers.IO) {
        database.contentDao().getWhatsAppBannerOnce()
    }
    suspend fun saveWhatsAppBanner(banner: com.example.database.entities.WhatsAppBannerEntity) = withContext(Dispatchers.IO) {
        database.contentDao().saveWhatsAppBanner(banner)
    }

    // -------------------------------------------------------------
    // COMPLAINT OPTIONS (शिकायत निवारण)
    // -------------------------------------------------------------
    val allComplaintOptions: Flow<List<com.example.database.entities.ComplaintOptionEntity>> = database.contentDao().getAllComplaintOptions()
    val activeComplaintOptions: Flow<List<com.example.database.entities.ComplaintOptionEntity>> = database.contentDao().getActiveComplaintOptions()
    suspend fun getAllComplaintOptionsOnce(): List<com.example.database.entities.ComplaintOptionEntity> = withContext(Dispatchers.IO) {
        database.contentDao().getAllComplaintOptionsOnce()
    }
    suspend fun insertComplaintOption(option: com.example.database.entities.ComplaintOptionEntity) = withContext(Dispatchers.IO) {
        database.contentDao().insertComplaintOption(option)
    }
    suspend fun updateComplaintOption(option: com.example.database.entities.ComplaintOptionEntity) = withContext(Dispatchers.IO) {
        database.contentDao().updateComplaintOption(option)
    }
    suspend fun deleteComplaintOption(option: com.example.database.entities.ComplaintOptionEntity) = withContext(Dispatchers.IO) {
        database.contentDao().deleteComplaintOption(option)
    }
    suspend fun syncComplaintOptionsFromCloud(list: List<com.example.database.entities.ComplaintOptionEntity>) = withContext(Dispatchers.IO) {
        database.contentDao().syncComplaintOptionsFromCloud(list)
    }

    // -------------------------------------------------------------
    // HOME TOP LINKS (होम पेज लिंक्स)
    // -------------------------------------------------------------
    val allHomeTopLinks: Flow<List<com.example.database.entities.HomeTopLinkEntity>> = database.contentDao().getAllHomeTopLinks()
    val activeHomeTopLinks: Flow<List<com.example.database.entities.HomeTopLinkEntity>> = database.contentDao().getActiveHomeTopLinks()
    suspend fun getAllHomeTopLinksOnce(): List<com.example.database.entities.HomeTopLinkEntity> = withContext(Dispatchers.IO) {
        database.contentDao().getAllHomeTopLinksOnce()
    }
    suspend fun insertHomeTopLink(link: com.example.database.entities.HomeTopLinkEntity) = withContext(Dispatchers.IO) {
        database.contentDao().insertHomeTopLink(link)
    }
    suspend fun updateHomeTopLink(link: com.example.database.entities.HomeTopLinkEntity) = withContext(Dispatchers.IO) {
        database.contentDao().updateHomeTopLink(link)
    }
    suspend fun deleteHomeTopLink(link: com.example.database.entities.HomeTopLinkEntity) = withContext(Dispatchers.IO) {
        database.contentDao().deleteHomeTopLink(link)
    }
    suspend fun syncHomeTopLinksFromCloud(list: List<com.example.database.entities.HomeTopLinkEntity>) = withContext(Dispatchers.IO) {
        database.contentDao().syncHomeTopLinksFromCloud(list)
    }

    // -------------------------------------------------------------
    // FINANCIAL SERVICES (लोन, कार्ड, निवेश)
    // -------------------------------------------------------------
    val allFinancialServices: Flow<List<com.example.database.entities.FinancialServiceEntity>> = database.contentDao().getAllFinancialServices()
    fun getActiveFinancialServicesByCategory(category: String): Flow<List<com.example.database.entities.FinancialServiceEntity>> =
        database.contentDao().getActiveFinancialServicesByCategory(category)
    fun getAllFinancialServicesByCategory(category: String): Flow<List<com.example.database.entities.FinancialServiceEntity>> =
        database.contentDao().getAllFinancialServicesByCategory(category)
    suspend fun getAllFinancialServicesOnce(): List<com.example.database.entities.FinancialServiceEntity> = withContext(Dispatchers.IO) {
        database.contentDao().getAllFinancialServicesOnce()
    }
    suspend fun insertFinancialService(service: com.example.database.entities.FinancialServiceEntity) = withContext(Dispatchers.IO) {
        database.contentDao().insertFinancialService(service)
    }
    suspend fun updateFinancialService(service: com.example.database.entities.FinancialServiceEntity) = withContext(Dispatchers.IO) {
        database.contentDao().updateFinancialService(service)
    }
    suspend fun deleteFinancialService(service: com.example.database.entities.FinancialServiceEntity) = withContext(Dispatchers.IO) {
        database.contentDao().deleteFinancialService(service)
    }
    suspend fun syncFinancialServicesFromCloud(list: List<com.example.database.entities.FinancialServiceEntity>) = withContext(Dispatchers.IO) {
        database.contentDao().syncFinancialServicesFromCloud(list)
    }

    // -------------------------------------------------------------
    // YOUTUBE RSS FEEDS (यूट्यूब चैनल फीड्स)
    // -------------------------------------------------------------
    val allYouTubeFeeds: Flow<List<com.example.database.entities.YouTubeFeedEntity>> = database.contentDao().getAllYouTubeFeeds()
    val activeYouTubeFeeds: Flow<List<com.example.database.entities.YouTubeFeedEntity>> = database.contentDao().getActiveYouTubeFeeds()
    suspend fun getAllYouTubeFeedsOnce(): List<com.example.database.entities.YouTubeFeedEntity> = withContext(Dispatchers.IO) {
        database.contentDao().getAllYouTubeFeedsOnce()
    }
    suspend fun insertYouTubeFeed(feed: com.example.database.entities.YouTubeFeedEntity) = withContext(Dispatchers.IO) {
        database.contentDao().insertYouTubeFeed(feed)
    }
    suspend fun updateYouTubeFeed(feed: com.example.database.entities.YouTubeFeedEntity) = withContext(Dispatchers.IO) {
        database.contentDao().updateYouTubeFeed(feed)
    }
    suspend fun deleteYouTubeFeed(feed: com.example.database.entities.YouTubeFeedEntity) = withContext(Dispatchers.IO) {
        database.contentDao().deleteYouTubeFeed(feed)
    }
    suspend fun syncYouTubeFeedsFromCloud(list: List<com.example.database.entities.YouTubeFeedEntity>) = withContext(Dispatchers.IO) {
        database.contentDao().syncYouTubeFeedsFromCloud(list)
    }

    suspend fun updateGlobalSalaryRules(newDa: Double, ruralHra: Double, urbanHra: Double) = withContext(Dispatchers.IO) {
        database.teacherProfileDao().updateDaPercent(newDa)
        database.staffDao().updateStaffDaPercent(newDa)
    }

    suspend fun getCurrentDaPercent(): Double = withContext(Dispatchers.IO) {
        database.teacherProfileDao().getTeacherProfileOnce()?.daPercent ?: 53.0
    }

    suspend fun removeInvalidPlaceholderFeeds() = withContext(Dispatchers.IO) {
        database.contentDao().removeInvalidPlaceholderFeeds()
    }

    suspend fun refreshYouTubeRssVideos(forceRefresh: Boolean = false): Int = withContext(Dispatchers.IO) {
        ytRssMutex.withLock {
            var totalFetched = 0
            try {
                database.contentDao().removeInvalidPlaceholderFeeds()
                database.contentDao().removeDuplicateVideos()

                val existing = database.contentDao().getAllVideosOnce()
                if (!forceRefresh && !com.example.core.RssSyncCache.shouldAutoRefreshYouTube(existing.isNotEmpty())) {
                    return@withLock 0
                }

                val feeds = database.contentDao().getAllYouTubeFeedsOnce()
                    .filter {
                        it.isVisible &&
                            !it.channelId.equals("UCshikshakbihar123", ignoreCase = true) &&
                            !it.feedUrl.contains("UCshikshakbihar123", ignoreCase = true)
                    }
                if (feeds.isEmpty()) return@withLock 0

                val existingIds = existing.mapNotNull { it.youtubeId.ifBlank { null } }.toMutableSet()
                val existingUrls = existing.map { it.youtubeUrl.trim() }.toMutableSet()

                for (feed in feeds) {
                    val videos = com.example.core.YouTubeRssHelper.fetchVideosFromRssFeed(feed.feedUrl, feed.channelTitle)
                    val uniqueNewVideos = mutableListOf<com.example.database.entities.VideoTutorialEntity>()
                    for (vid in videos) {
                        val hasId = vid.youtubeId.isNotBlank() && existingIds.contains(vid.youtubeId)
                        val hasUrl = vid.youtubeUrl.isNotBlank() && existingUrls.contains(vid.youtubeUrl.trim())
                        if (!hasId && !hasUrl) {
                            uniqueNewVideos.add(vid)
                            if (vid.youtubeId.isNotBlank()) existingIds.add(vid.youtubeId)
                            if (vid.youtubeUrl.isNotBlank()) existingUrls.add(vid.youtubeUrl.trim())
                        }
                    }
                    if (uniqueNewVideos.isNotEmpty()) {
                        database.contentDao().insertAllVideos(uniqueNewVideos)
                        totalFetched += uniqueNewVideos.size
                    }
                }
                com.example.core.RssSyncCache.markYouTubeSyncedNow()
                if (totalFetched > 0) {
                    database.contentDao().pruneOldRssVideos()
                }
            } catch (_: Exception) {}
            totalFetched
        }
    }

    // -------------------------------------------------------------
    // NEWS RSS FEEDS (लाइव समाचार RSS फीड्स)
    // -------------------------------------------------------------
    val allNewsRssFeeds: Flow<List<com.example.database.entities.NewsRssFeedEntity>> = database.contentDao().getAllNewsRssFeeds()
    val activeNewsRssFeeds: Flow<List<com.example.database.entities.NewsRssFeedEntity>> = database.contentDao().getActiveNewsRssFeeds()
    suspend fun getAllNewsRssFeedsOnce(): List<com.example.database.entities.NewsRssFeedEntity> = withContext(Dispatchers.IO) {
        database.contentDao().getAllNewsRssFeedsOnce()
    }
    suspend fun insertNewsRssFeed(feed: com.example.database.entities.NewsRssFeedEntity) = withContext(Dispatchers.IO) {
        database.contentDao().insertNewsRssFeed(feed)
    }
    suspend fun updateNewsRssFeed(feed: com.example.database.entities.NewsRssFeedEntity) = withContext(Dispatchers.IO) {
        database.contentDao().updateNewsRssFeed(feed)
    }
    suspend fun deleteNewsRssFeed(feed: com.example.database.entities.NewsRssFeedEntity) = withContext(Dispatchers.IO) {
        database.contentDao().deleteNewsRssFeed(feed)
    }
    suspend fun syncNewsRssFeedsFromCloud(list: List<com.example.database.entities.NewsRssFeedEntity>) = withContext(Dispatchers.IO) {
        database.contentDao().syncNewsRssFeedsFromCloud(list)
    }

    private fun normalizeArticleUrl(raw: String): String {
        val trimmed = raw.trim().lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
        return if (trimmed == "themagadhtimes.blogspot.com" || trimmed == "state.bihar.gov.in/educationbihar") "" else trimmed
    }

    suspend fun refreshNewsRssArticles(forceRefresh: Boolean = false): Int = withContext(Dispatchers.IO) {
        newsRssMutex.withLock {
            var totalFetched = 0
            try {
                val existingNews = database.contentDao().getAllNewsOnce()
                val hasCachedRss = existingNews.any { it.category == "लाइव RSS" }
                if (!forceRefresh && !com.example.core.RssSyncCache.shouldAutoRefreshNews(hasCachedRss)) {
                    return@withLock 0
                }

                val feeds = database.contentDao().getAllNewsRssFeedsOnce().filter { it.isVisible }
                if (feeds.isEmpty()) return@withLock 0

                val seenTitles = existingNews.map { it.title.trim().lowercase() }.toMutableSet()
                val seenUrls = existingNews.mapNotNull { normalizeArticleUrl(it.sourceUrl).ifBlank { null } }.toMutableSet()
                val byTitle = existingNews.associateBy { it.title.trim().lowercase() }
                val byUrl = existingNews.mapNotNull { n ->
                    val u = normalizeArticleUrl(n.sourceUrl)
                    if (u.isNotBlank()) u to n else null
                }.toMap()

                val toInsert = mutableListOf<com.example.database.entities.NewsArticleEntity>()

                for (feed in feeds) {
                    val articles = com.example.core.NewsRssHelper.fetchNewsFromRssFeed(feed.feedUrl, feed.title)
                    for (art in articles) {
                        val normTitle = art.title.trim().lowercase()
                        val normUrl = normalizeArticleUrl(art.sourceUrl)
                        val matchByUrl = if (normUrl.isNotBlank()) byUrl[normUrl] else null
                        val matchByTitle = byTitle[normTitle]
                        val existing = matchByUrl ?: matchByTitle

                        val alreadyQueued = seenTitles.contains(normTitle) || (normUrl.isNotBlank() && seenUrls.contains(normUrl))
                        if (existing == null && !alreadyQueued) {
                            toInsert.add(art)
                            seenTitles.add(normTitle)
                            if (normUrl.isNotBlank()) seenUrls.add(normUrl)
                        } else if (existing != null) {
                            // Update headline/summary if same article URL had its title/summary edited, preserving pin state
                            val needsUpdate = existing.title != art.title ||
                                existing.summary != art.summary ||
                                (art.imageUrl.isNotBlank() && existing.imageUrl != art.imageUrl)
                            if (needsUpdate && (existing.isPinned || matchByUrl != null)) {
                                database.contentDao().updateNews(
                                    art.copy(
                                        id = existing.id,
                                        isPinned = existing.isPinned,
                                        pinOrder = existing.pinOrder,
                                        isBreaking = existing.isBreaking
                                    )
                                )
                            }
                        }
                    }
                }

                if (toInsert.isNotEmpty()) {
                    database.contentDao().insertAllNews(toInsert)
                    totalFetched = toInsert.size
                }
                com.example.core.RssSyncCache.markNewsSyncedNow()
                if (totalFetched > 0) {
                    database.contentDao().removeDuplicateNews()
                    database.contentDao().pruneOldRssNews()
                }
            } catch (_: Exception) {}
            totalFetched
        }
    }

    // Backup & Restore (JSON & Encrypted Data)
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val teacher = database.teacherProfileDao().getTeacherProfileOnce()
        val school = database.schoolProfileDao().getSchoolProfileOnce()
        val staff = database.staffDao().getAllStaff().firstOrNull() ?: emptyList()

        teacher?.let {
            root.put("teacher_name", it.name)
            root.put("teacher_designation", it.designation)
            root.put("hrms_id", it.hrmsId)
            root.put("eshikshakosh_id", it.eshikshakoshId)
            root.put("pran_number", it.pranNumber)
            root.put("school_name", it.schoolName)
            root.put("udise_code", it.udiseCode)
            root.put("teacher_district", it.district)
            root.put("teacher_block", it.block)
            root.put("panchayat", it.panchayat)
            root.put("teacher_basic", it.basicPay)
            root.put("da_percent", it.daPercent)
            root.put("hra_category", it.hraCategory)
        }
        school?.let {
            root.put("school_name", it.schoolName)
            root.put("school_udise", it.udiseCode)
            root.put("school_district", it.district)
            root.put("school_block", it.block)
            root.put("school_headmaster", it.headmasterName)
        }

        val staffArray = JSONArray()
        staff.forEach { s ->
            val sObj = JSONObject()
            sObj.put("name", s.name)
            sObj.put("designation", s.designation)
            sObj.put("basicPay", s.basicPay)
            sObj.put("employeeId", s.employeeId)
            staffArray.put(sObj)
        }
        root.put("staff", staffArray)

        root.put("version", "2.1")
        root.put("appName", "Shikshak Sathi")
        root.put("exportedAt", System.currentTimeMillis())
        root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (root.has("teacher_name")) {
                val current = database.teacherProfileDao().getTeacherProfileOnce() ?: TeacherProfileEntity()
                database.teacherProfileDao().saveTeacherProfile(
                    current.copy(
                        name = root.optString("teacher_name", current.name),
                        designation = root.optString("teacher_designation", current.designation),
                        hrmsId = root.optString("hrms_id", current.hrmsId),
                        eshikshakoshId = root.optString("eshikshakosh_id", current.eshikshakoshId),
                        pranNumber = root.optString("pran_number", current.pranNumber),
                        schoolName = root.optString("school_name", current.schoolName),
                        udiseCode = root.optString("udise_code", current.udiseCode),
                        district = root.optString("teacher_district", current.district),
                        block = root.optString("teacher_block", current.block),
                        panchayat = root.optString("panchayat", current.panchayat),
                        basicPay = root.optDouble("teacher_basic", current.basicPay),
                        daPercent = root.optDouble("da_percent", current.daPercent),
                        hraCategory = root.optString("hra_category", current.hraCategory)
                    )
                )
            }
            if (root.has("school_name")) {
                val currentSchool = database.schoolProfileDao().getSchoolProfileOnce() ?: SchoolProfileEntity()
                database.schoolProfileDao().saveSchoolProfile(
                    currentSchool.copy(
                        schoolName = root.optString("school_name", currentSchool.schoolName),
                        udiseCode = root.optString("school_udise", currentSchool.udiseCode),
                        district = root.optString("school_district", currentSchool.district),
                        block = root.optString("school_block", currentSchool.block),
                        headmasterName = root.optString("school_headmaster", currentSchool.headmasterName)
                    )
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

