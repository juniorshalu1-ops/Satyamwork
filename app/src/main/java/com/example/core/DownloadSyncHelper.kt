package com.example.core

import com.example.database.entities.DownloadResourceEntity
import java.security.MessageDigest
import java.util.UUID

object DownloadSyncHelper {

    fun generateCloudId(): String {
        val raw = UUID.randomUUID().toString().replace("-", "").take(16)
        return "dl_$raw"
    }

    fun resolveCanonicalDocId(download: DownloadResourceEntity): String {
        val cleanCloudId = download.cloudId.trim()
        if (cleanCloudId.isNotBlank()) {
            return cleanCloudId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        }
        val keyMaterial = buildString {
            append(download.title.trim().lowercase())
            append("|")
            append(download.pdfUrl.trim().lowercase())
            append("|")
            append(download.orderNumber.trim().lowercase())
            append("|")
            append(download.fileType.trim().lowercase())
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(keyMaterial.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(20)
        return "dl_$digest"
    }

    fun legacyDocId(download: DownloadResourceEntity): String {
        val safeTitle = download.title.take(30).replace(Regex("[^a-zA-Z0-9]"), "_")
        return "doc_${download.id}_$safeTitle"
    }

    fun deduplicationKey(download: DownloadResourceEntity): String {
        val cid = download.cloudId.trim()
        if (cid.isNotBlank()) return "cid:${cid.lowercase()}"

        val url = download.pdfUrl.trim().lowercase()
        if (url.isNotBlank()) return "url:$url"

        return "meta:${download.title.trim().lowercase()}|${download.category.trim().lowercase()}|${download.fileType.trim().lowercase()}|${download.orderNumber.trim().lowercase()}"
    }

    fun deduplicate(list: List<DownloadResourceEntity>): List<DownloadResourceEntity> {
        if (list.size <= 1) return list
        val results = mutableListOf<DownloadResourceEntity>()
        val byCloudId = mutableMapOf<String, Int>()
        val byUrl = mutableMapOf<String, Int>()
        val byMeta = mutableMapOf<String, Int>()
        val byTitle = mutableMapOf<String, Int>()

        fun registerKeys(entry: DownloadResourceEntity, index: Int) {
            val cid = entry.cloudId.trim().lowercase()
            if (cid.isNotBlank()) byCloudId[cid] = index
            val url = entry.pdfUrl.trim().lowercase()
            if (url.isNotBlank()) byUrl[url] = index
            val meta = "${entry.title.trim().lowercase()}|${entry.category.trim().lowercase()}|${entry.fileType.trim().lowercase()}|${entry.orderNumber.trim().lowercase()}"
            byMeta[meta] = index
            val cleanTitle = entry.title.trim().lowercase()
            if (cleanTitle.isNotBlank()) {
                byTitle["$cleanTitle|${entry.fileType.trim().lowercase()}"] = index
            }
        }

        for (item in list) {
            val cid = item.cloudId.trim().lowercase()
            val url = item.pdfUrl.trim().lowercase()
            val meta = "${item.title.trim().lowercase()}|${item.category.trim().lowercase()}|${item.fileType.trim().lowercase()}|${item.orderNumber.trim().lowercase()}"
            val titleKey = if (item.title.isNotBlank()) "${item.title.trim().lowercase()}|${item.fileType.trim().lowercase()}" else ""

            val existingIdx = when {
                cid.isNotBlank() && byCloudId.containsKey(cid) -> byCloudId[cid]
                url.isNotBlank() && byUrl.containsKey(url) -> byUrl[url]
                url.isBlank() && byMeta.containsKey(meta) -> byMeta[meta]
                url.isBlank() && titleKey.isNotBlank() && byTitle.containsKey(titleKey) -> byTitle[titleKey]
                titleKey.isNotBlank() && byTitle[titleKey]?.let { results[it].pdfUrl.isBlank() } == true -> byTitle[titleKey]
                else -> null
            }

            if (existingIdx == null) {
                val newIdx = results.size
                results.add(item)
                registerKeys(item, newIdx)
            } else {
                val merged = mergeDownloadEntries(results[existingIdx], item)
                results[existingIdx] = merged
                registerKeys(merged, existingIdx)
            }
        }
        return results
    }

    private fun mergeDownloadEntries(
        first: DownloadResourceEntity,
        second: DownloadResourceEntity
    ): DownloadResourceEntity {
        val preferSecond = when {
            second.cloudId.isNotBlank() && first.cloudId.isBlank() -> true
            first.cloudId.isNotBlank() && second.cloudId.isBlank() -> false
            second.timestamp > first.timestamp -> true
            else -> false
        }
        val winner = if (preferSecond) second else first
        val loser = if (preferSecond) first else second
        return winner.copy(
            id = if (first.id > 0L) first.id else second.id,
            cloudId = winner.cloudId.ifBlank { loser.cloudId },
            pdfUrl = winner.pdfUrl.ifBlank { loser.pdfUrl },
            localFilePath = winner.localFilePath.ifBlank { loser.localFilePath },
            downloadCount = maxOf(first.downloadCount, second.downloadCount)
        )
    }
}
