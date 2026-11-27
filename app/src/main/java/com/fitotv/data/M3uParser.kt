package com.fitotv.data

import java.io.BufferedReader

/**
 * Lee listas M3U / M3U8 (las de siempre y las "m3u_plus" de los paneles Xtream).
 * No depende de Android para poder probarlo aparte.
 */
object M3uParser {

    data class Result(val channels: List<Channel>, val epgUrl: String)

    private val attrRegex = Regex("""([A-Za-z0-9_-]+)\s*=\s*"([^"]*)"""")

    private val movieExt = listOf(".mp4", ".mkv", ".avi", ".mov", ".m4v", ".wmv", ".webm")

    fun parse(reader: BufferedReader, listId: String): Result {
        var epgUrl = ""
        var pendingName: String? = null
        var pendingAttrs: Map<String, String> = emptyMap()
        var pendingGroup = ""
        var userAgent = ""
        var referrer = ""
        val out = ArrayList<Channel>(1024)
        val seen = HashMap<String, Int>()
        var liveNum = 0

        fun add(url: String) {
            val attrs = pendingAttrs
            val name = (pendingName ?: attrs["tvg-name"] ?: url.substringAfterLast('/')).trim().ifEmpty { "Canal" }
            val group = (attrs["group-title"] ?: pendingGroup).trim().ifEmpty { "Sin grupo" }
            val kind = classify(url, group)
            val num = if (kind == ContentKind.LIVE) {
                liveNum += 1
                attrs["tvg-chno"]?.trim()?.toIntOrNull() ?: liveNum
            } else 0
            val base = stableId(url, name)
            val dup = seen.getOrDefault(base, 0)
            seen[base] = dup + 1
            out.add(
                Channel(
                    id = if (dup == 0) base else "$base-$dup",
                    num = num,
                    name = name,
                    url = url,
                    logo = attrs["tvg-logo"]?.trim().orEmpty(),
                    group = group,
                    tvgId = (attrs["tvg-id"] ?: "").trim(),
                    kind = kind,
                    userAgent = attrs["user-agent"] ?: userAgent,
                    referrer = referrer,
                )
            )
            pendingName = null
            pendingAttrs = emptyMap()
            pendingGroup = ""
            userAgent = ""
            referrer = ""
        }

        reader.lineSequence().forEach { raw ->
            val line = raw.trim().removePrefix("﻿")
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXTM3U", ignoreCase = true) -> {
                    val attrs = attributes(line)
                    val tvg = attrs["x-tvg-url"] ?: attrs["url-tvg"] ?: attrs["tvg-url"] ?: ""
                    if (tvg.isNotBlank()) epgUrl = tvg.split(',').first().trim()
                }
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    pendingAttrs = attributes(line)
                    pendingName = displayName(line)
                }
                line.startsWith("#EXTGRP:", ignoreCase = true) -> pendingGroup = line.substringAfter(':').trim()
                line.startsWith("#EXTVLCOPT:http-user-agent=", ignoreCase = true) -> userAgent = line.substringAfter('=').trim()
                line.startsWith("#EXTVLCOPT:http-referrer=", ignoreCase = true) -> referrer = line.substringAfter('=').trim()
                line.startsWith("#") -> Unit
                else -> if (looksLikeUrl(line)) add(line)
            }
        }
        return Result(out, epgUrl)
    }

    private fun attributes(line: String): Map<String, String> {
        val map = HashMap<String, String>()
        attrRegex.findAll(line).forEach { m -> map[m.groupValues[1].lowercase()] = m.groupValues[2] }
        return map
    }

    /** El nombre va después de la primera coma que no esté entre comillas. */
    private fun displayName(line: String): String? {
        var inQuotes = false
        for (i in line.indices) {
            val ch = line[i]
            if (ch == '"') inQuotes = !inQuotes
            else if (ch == ',' && !inQuotes) return line.substring(i + 1).trim().ifEmpty { null }
        }
        return null
    }

    private fun looksLikeUrl(line: String): Boolean {
        val l = line.lowercase()
        return l.startsWith("http://") || l.startsWith("https://") || l.startsWith("rtmp://") ||
            l.startsWith("rtsp://") || l.startsWith("udp://") || l.startsWith("rtp://")
    }

    fun classify(url: String, group: String): ContentKind {
        val path = url.substringBefore('?').lowercase()
        val g = group.lowercase()
        if (path.contains("/series/")) return ContentKind.SERIES
        if (path.contains("/movie/") || path.contains("/movies/")) return ContentKind.MOVIE
        if (movieExt.any { path.endsWith(it) }) {
            return if (g.contains("serie")) ContentKind.SERIES else ContentKind.MOVIE
        }
        return ContentKind.LIVE
    }

    private fun stableId(url: String, name: String): String {
        val h1 = url.hashCode().toLong() and 0xffffffffL
        val h2 = name.hashCode().toLong() and 0xffffffffL
        return java.lang.Long.toString(h1, 36) + java.lang.Long.toString(h2, 36)
    }
}
