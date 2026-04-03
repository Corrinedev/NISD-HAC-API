package com.cdv.hac.api

import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.String
import kotlin.collections.forEach

fun post(urlStr: String, data: Map<String, String>, referer: String?, cookies: MutableMap<String, String>): String {
    val body = data.entries.joinToString("&") {
        "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
    }
    val conn = buildConnection(urlStr, referer, followRedirects = true, cookies).apply {
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    }
    OutputStreamWriter(conn.outputStream).use { it.write(body) }
    return readResponse(conn, cookies)
}

fun buildConnection(urlStr: String, referer: String?, followRedirects: Boolean, cookies: MutableMap<String, String>): HttpURLConnection {
    return (URL(urlStr).openConnection() as HttpURLConnection).apply {
        instanceFollowRedirects = followRedirects
        setRequestProperty("User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36")
        setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        setRequestProperty("Accept-Language", "en-US,en;q=0.5")
        if (cookies.isNotEmpty())
            setRequestProperty("Cookie", cookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
        referer?.let { setRequestProperty("Referer", it) }
    }
}

fun readResponse(conn: HttpURLConnection, cookies: MutableMap<String, String>): String {
    storeCookies(conn, cookies)
    return try {
        conn.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        conn.errorStream?.bufferedReader()?.readText() ?: ""
    } finally {
        conn.disconnect()
    }
}

fun get(urlStr: String, referer: String? = null, cookies: MutableMap<String, String>): String {
    val conn = buildConnection(urlStr, referer, followRedirects = true, cookies).apply {
        requestMethod = "GET"
    }
    return readResponse(conn, cookies)
}

fun postNoRedirect(urlStr: String, data: Map<String, String>, referer: String?, cookies: MutableMap<String, String>): Pair<String, String?> {
    val body = data.entries.joinToString("&") {
        "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
    }
    val conn = buildConnection(urlStr, referer, followRedirects = false, cookies).apply {
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    }
    OutputStreamWriter(conn.outputStream).use { it.write(body) }
    val location = conn.getHeaderField("Location")
    return Pair(readResponse(conn, cookies), location)
}

fun storeCookies(conn: HttpURLConnection, cookies: MutableMap<String, String>) {
    conn.headerFields["Set-Cookie"]?.forEach { header ->
        val cookiePart = header.split(";").first().trim()
        val eqIdx = cookiePart.indexOf('=')
        if (eqIdx > 0)
            cookies[cookiePart.substring(0, eqIdx)] = cookiePart.substring(eqIdx + 1)
    }
}
