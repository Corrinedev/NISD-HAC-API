package com.cdv.hac.api

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class Account(private var username: String, private var password: String) {

    private val cookies = mutableMapOf<String, String>()

    private val ASSIGNMENTS_URL = "https://hac.nisd.net/HomeAccess/Content/Student/Assignments.aspx"
    private val TRANSCRIPT_URL  = "https://hac.nisd.net/HomeAccess/Content/Student/Registration.aspx"
    private val LOGIN_URL       = "https://hac.nisd.net/HomeAccess/Account/LogOn?ReturnUrl=%2fHomeAccess%2f"
    private val BASE_URL        = "https://hac.nisd.net"

    init { login() }

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------

    private fun login() {
        val loginPage = get(LOGIN_URL, cookies = cookies)
        val doc = Jsoup.parse(loginPage)

        val token = doc.selectFirst("input[name=__RequestVerificationToken]")?.attr("value")
            ?: throw Exception("Could not find verification token on login page")

        val payload = mapOf(
            "VerificationOption"         to "UsernamePassword",
            "Database"                   to "10",
            "LogOnDetails.Password"      to password,
            "__RequestVerificationToken" to token,
            "LogOnDetails.UserName"      to username
        )

        val (_, location) = postNoRedirect(LOGIN_URL, payload, referer = LOGIN_URL, cookies = cookies)

        if (location == null || location.contains("LogOn", ignoreCase = true)) {
            throw InvalidCredentialsException()
        }

        get(if (location.startsWith("http")) location else "$BASE_URL$location", cookies = cookies)
    }

    fun reset() {
        cookies.clear()
        login()
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    fun returnCurrentGrades(): Pair<List<Double>, List<String>> {
        val doc = Jsoup.parse(fetchAssignmentsPage(quarter = null))
        return initializeClasses(doc)
    }

    fun returnQuarterGrade(quarter: Int): Pair<List<Double>, List<String>> {
        val doc = Jsoup.parse(fetchAssignmentsPage(quarter = quarter))
        return initializeClasses(doc)
    }

    fun returnCurrentAssignmentsDf(): List<List<Map<String, String>>> {
        val doc = Jsoup.parse(fetchAssignmentsPage(quarter = null))
        return extractTableListFromDoc(doc)
    }

    fun returnQuarterAssignmentsDf(quarter: Int): List<List<Map<String, String>>> {
        val doc = Jsoup.parse(fetchAssignmentsPage(quarter = quarter))
        return extractTableListFromDoc(doc)
    }

    fun returnCurrentAssignmentsHtml(): List<List<Map<String, String>>> =
        returnCurrentAssignmentsDf()

    fun returnQuarterAssignmentsHtml(quarter: Int? = null): List<Map<String, Any>> {
        val doc = Jsoup.parse(fetchAssignmentsPage(quarter = quarter))
        return parseHacData(doc)
    }

    fun returnWeightedGpa(): Float {
        return _returnRegistrationTableContents()[0]
            ?.text()?.trim()?.filter { it.isDigit() || it == '.'}?.toFloat() ?: -1f
    }

    fun returnFullRank(): String {
        return _returnRegistrationTableContents()[1]?.text() ?: "Error fetching rank"
    }

    fun returnRank(): Int {
        val fr = returnFullRank()
        val firstNum = fr.indexOfFirst(Char::isDigit)
        var lastNumIndex = firstNum
        for(i in firstNum..fr.length) {
            if(!fr[i].isDigit()) break
            lastNumIndex += 1
        }

        return fr.substring(firstNum, lastNumIndex).toInt()
    }

    fun returnAddress(): String {
        val fr = returnFullAddress()
        return fr.substring(fr.indexOfFirst(Char::isDigit))
    }

    fun returnFullAddress(): String {
        return _returnContactTableContents().first.getElementsByTag("tr").first()?.getElementsByTag("td")[0]?.text()?.split(',')[0]!!
    }

    private fun _returnRegistrationTableContents(): Elements {
        val doc = Jsoup.parse(fetchTranscript())
        return doc
            .getElementById("MainContent")
            ?.getElementById("fmMain")
            ?.getElementById("plnMain_lblDDDisplay")
            ?.getElementById("plnMain_dlDDDisplay")
            ?.getElementsByClass("sg-standard-width")!!
    }

    private fun _returnContactTableContents(): Elements {
        val doc = Jsoup.parse(fetchTranscript())

        return doc.getElementById("MainContent")
            ?.getElementById("fmMain")
            ?.getElementById("plnMain_lblContact")
            ?.getElementById("tblContactHeader")
            ?.getElementById("plnMain_dvContact")
            ?.getElementById("plnMain_dlContacts")
            ?.getElementsByTag("tbody")!!
    }

    fun returnCollegeGpa(): Float {
        return (returnWeightedGpa() / 100) * 4.0f
    }

    fun getUsername(): String = username

    // -------------------------------------------------------------------------
    // Core fetch helpers
    // -------------------------------------------------------------------------

    private fun fetchAssignmentsPage(quarter: Int?): String {
        val html = get(ASSIGNMENTS_URL, referer = ASSIGNMENTS_URL, cookies = cookies)
        if (quarter == null) return html

        val doc = Jsoup.parse(html)
        val quarterValue = getQuarterValue(doc, quarter)
        val payload = getFormTokens(doc).toMutableMap().apply {
            put("ctl00\$plnMain\$ddlReportCardRuns", quarterValue)
            put("ctl00\$plnMain\$ddlClasses",        "ALL")
            put("ctl00\$plnMain\$ddlOrderBy",        "Class")
            put("__EVENTTARGET", "ctl00\$plnMain\$btnRefreshView")
        }
        return post(ASSIGNMENTS_URL, payload, referer = ASSIGNMENTS_URL, cookies = cookies)
    }

    private fun fetchTranscript(): String = get(TRANSCRIPT_URL, referer = TRANSCRIPT_URL, cookies = cookies)

    // -------------------------------------------------------------------------
    // Parsing helpers
    // -------------------------------------------------------------------------

    private fun initializeClasses(doc: Document): Pair<List<Double>, List<String>> {
        val grades = doc.select("span[id~=lblHdrAverage]")
            .map { it.text().removePrefix("AVG ").trim().toDouble() }

        val names = doc.select("a.sg-header-heading")
            .map { el ->
                val text = el.text().trim()
                val idx  = text.indexOf("-")
                if (idx >= 0) text.substring(idx + 4).trim() else text
            }

        return Pair(grades, names)
    }

    private fun getFormTokens(doc: Document): Map<String, String> =
        listOf("__VIEWSTATE", "__VIEWSTATEGENERATOR", "__EVENTVALIDATION",
            "__EVENTTARGET", "__EVENTARGUMENT", "__LASTFOCUS")
            .associateWith { doc.selectFirst("input[name=$it]")?.attr("value") ?: "" }

    private fun getQuarterValue(doc: Document, quarter: Int): String {
        val options = doc.select("select[name=ctl00\$plnMain\$ddlReportCardRuns] option")
            .map { it.attr("value") }
            .filter { it != "ALL" }
        return if (quarter <= options.size) options[quarter - 1] else "$quarter-${getSchoolYear()}"
    }

    private fun extractTableListFromDoc(doc: Element): List<List<Map<String, String>>> {
        return doc.select("table").mapNotNull { table ->
            val rows    = table.select("tr")
            if (rows.isEmpty()) return@mapNotNull null
            val headers = rows[0].select("th, td").map { it.text().trim() }
            if (headers.size != 10) return@mapNotNull null
            rows.drop(1).map { row ->
                val cells = row.select("td").map { it.text().trim() }
                headers.zip(cells).toMap()
            }
        }
    }

    private fun extractTableList(table: Elements): List<List<Map<String, String>>> {
        return table.mapNotNull { table ->
            val rows    = table.select("tr")
            if (rows.isEmpty()) return@mapNotNull null
            val headers = rows[0].select("th, td").map { it.text().trim() }
            if (headers.size != 10) return@mapNotNull null
            rows.drop(1).map { row ->
                val cells = row.select("td").map { it.text().trim() }
                headers.zip(cells).toMap()
            }
        }
    }

    fun parseHacData(doc: Document): List<Map<String, Any>> {
        return doc.select("div.AssignmentClass").map { classDiv ->
            val className = classDiv.selectFirst("a.sg-header-heading")
                ?.text()?.trim()?.let { raw ->
                    val idx = raw.indexOf("-")
                    if (idx >= 0) raw.substring(idx + 4).trim() else raw
                } ?: "Unknown"

            val classAvg = classDiv.selectFirst("span[id~=lblHdrAverage]")
                ?.text()?.removePrefix("AVG ")?.trim() ?: "N/A"

            val assignments = classDiv.select("tr.sg-asp-table-data-row").mapNotNull { row ->
                val cells = row.select("td").map { it.text().trim() }
                if (cells.size < 5) return@mapNotNull null

                val category = cells[3]
                if (category != "Formative" && category != "Summative") return@mapNotNull null

                val scoreStr = cells[4]
                if (scoreStr.isBlank()) return@mapNotNull null

                mapOf(
                    "date"     to cells[0],
                    "title"    to cells[2],
                    "category" to category,
                    "score"    to (scoreStr.toDoubleOrNull()
                        ?: scoreStr.substringBefore(".").toDoubleOrNull()
                        ?: -1.0)
                )
            }

            mapOf(
                "class"       to className,
                "average"     to classAvg,
                "assignments" to assignments
            )
        }
    }
}