package com.cdv.hac.api

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*

// Global or shared Ktor HttpClient configuration
val client = HttpClient {
    // Disable automatic redirect handling so we can intercept
    // redirects manually inside "postNoRedirect"
    followRedirects = false
    expectSuccess = false // Prevents automatic routing panics on redirects
    install(HttpCookies)
    HttpResponseValidator {
        // Tells Ktor to skip throwing exceptions when the raw decompressed
        // byte count differs from the server's compressed header definition
        validateResponse { response ->
            // Leave empty or add your custom status code checks (e.g., 404 handling)
        }
    }

    // Default browser headers
    defaultRequest {
        header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36")
        header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.5")

        header(HttpHeaders.AcceptEncoding, "identity")
    }

}


private fun HttpRequestBuilder.applyCommonHeaders(referer: String?) {
    referer?.let { header(HttpHeaders.Referrer, it) }
}

// Helper to resolve relative redirects dynamically based on the current request domain
private fun resolveAbsoluteUrl(currentUrl: String, redirectLocation: String): String {
    return if (redirectLocation.startsWith("http")) {
        redirectLocation
    } else {
        val origin = Url(currentUrl)
        "${origin.protocol.name}://${origin.hostWithPort}$redirectLocation"
    }
}

suspend fun get(urlStr: String, referer: String? = null): HttpResponse {
    val response = client.get(urlStr) {
        applyCommonHeaders(referer)
    }

    // 2. Handle manual redirect chains sequentially so cookies save perfectly
    return if (response.status.value in 300..399) {
        val redirectUrl = response.headers[HttpHeaders.Location] ?: return response
        val absoluteUrl = resolveAbsoluteUrl(urlStr, redirectUrl)
        get(absoluteUrl, referer = urlStr)
    } else {
        response
    }
}

suspend fun post(urlStr: String, data: Map<String, String>, referer: String?): HttpResponse {
    val response = client.post(urlStr) {
        applyCommonHeaders(referer)
        setBody(FormDataContent(Parameters.build {
            data.forEach { (key, value) -> append(key, value) }
        }))
    }

    return if (response.status.value in 300..399) {
        val redirectUrl = response.headers[HttpHeaders.Location] ?: return response
        val absoluteUrl = resolveAbsoluteUrl(urlStr, redirectUrl)
        get(absoluteUrl, referer = urlStr) // Switch to GET on redirect per standard HTTP guidelines
    } else {
        response
    }
}

