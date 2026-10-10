package com.cdv.hac.api

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*

private const val MAX_REDIRECTS = 7

private fun HttpRequestBuilder.applyCommonHeaders(referer: String?) {
    referer?.let { header(HttpHeaders.Referrer, it) }
}

private fun resolveAbsoluteUrl(currentUrl: String, redirectLocation: String): String {
    return if (redirectLocation.startsWith("http")) {
        redirectLocation
    } else {
        val origin = Url(currentUrl)
        "${origin.protocol.name}://${origin.hostWithPort}$redirectLocation"
    }
}

suspend fun Account.get(urlStr: String, referer: String? = null): HttpResponse {
    var currentUrl = urlStr
    var currentReferer = referer
    var redirectCount = 0

    while (true) {
        val response = client.get(currentUrl) {
            applyCommonHeaders(currentReferer)
        }

        if (response.status.value in 300..399 && redirectCount < MAX_REDIRECTS) {
            val redirectUrl = response.headers[HttpHeaders.Location] ?: return response
            currentReferer = currentUrl
            currentUrl = resolveAbsoluteUrl(currentUrl, redirectUrl)
            redirectCount++
        } else {
            return response
        }
    }
}

suspend fun Account.post(urlStr: String, data: Map<String, String>, referer: String?): HttpResponse {
    val response = client.post(urlStr) {
        applyCommonHeaders(referer)
        setBody(FormDataContent(parametersOf(data.mapValues { listOf(it.value) })))
    }

    return if (response.status.value in 300..399) {
        val redirectUrl = response.headers[HttpHeaders.Location] ?: return response
        val absoluteUrl = resolveAbsoluteUrl(urlStr, redirectUrl)
        get(absoluteUrl, referer = urlStr)
    } else {
        response
    }
}

