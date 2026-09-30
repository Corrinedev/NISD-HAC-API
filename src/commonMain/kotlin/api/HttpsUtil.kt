package com.cdv.hac.api

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*

// Global or shared Ktor HttpClient configuration
val client = HttpClient {
    // Disable automatic redirect handling so we can intercept
    // redirects manually inside "postNoRedirect"
    followRedirects = false

    // Default browser headers
    defaultRequest {
        header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36")
        header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.5")
    }
}

// Helper function to build headers (Cookies and Referer)
private fun HttpRequestBuilder.applyCommonHeaders(referer: String?, cookies: Map<String, String>) {
    referer?.let { header(HttpHeaders.Referer, it) }
    if (cookies.isNotEmpty()) {
        val cookieString = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
        header(HttpHeaders.Cookie, cookieString)
    }
}

// Intercepts response headers to manually track cookies
private fun storeCookies(response: HttpResponse, cookies: MutableMap<String, String>) {
    response.headers.getAll(HttpHeaders.SetCookie)?.forEach { header ->
        val cookiePart = header.split(";").first().trim()
        val eqIdx = cookiePart.indexOf('=')
        if (eqIdx > 0) {
            cookies[cookiePart.substring(0, eqIdx)] = cookiePart.substring(eqIdx + 1)
        }
    }
}

suspend fun get(urlStr: String, referer: String? = null, cookies: MutableMap<String, String>): String {
    val response = client.get(urlStr) {
        applyCommonHeaders(referer, cookies)
    }
    storeCookies(response, cookies)

    // Ktor handles redirects natively if enabled, but since followRedirects = false globally,
    // manual handling is required here if a 3xx status is returned:
    return if (response.status.value in 300..399) {
        val redirectUrl = response.headers[HttpHeaders.Location] ?: return response.bodyAsText()
        get(redirectUrl, referer, cookies) // Follow redirect
    } else {
        response.bodyAsText()
    }
}

suspend fun post(urlStr: String, data: Map<String, String>, referer: String?, cookies: MutableMap<String, String>): String {
    val response = client.post(urlStr) {
        applyCommonHeaders(referer, cookies)
        // Ktor handles form URL encoding safely across platforms
        setBody(FormDataContent(Parameters.build {
            data.forEach { (key, value) -> append(key, value) }
        }))
    }
    storeCookies(response, cookies)

    return if (response.status.value in 300..399) {
        val redirectUrl = response.headers[HttpHeaders.Location] ?: return response.bodyAsText()
        get(redirectUrl, referer, cookies) // Follow redirect via GET
    } else {
        response.bodyAsText()
    }
}

suspend fun postNoRedirect(urlStr: String, data: Map<String, String>, referer: String?, cookies: MutableMap<String, String>): Pair<String, String?> {
    val response = client.post(urlStr) {
        applyCommonHeaders(referer, cookies)
        setBody(FormDataContent(Parameters.build {
            data.forEach { (key, value) -> append(key, value) }
        }))
    }
    storeCookies(response, cookies)

    val location = response.headers[HttpHeaders.Location]
    return Pair(response.bodyAsText(), location)
}
