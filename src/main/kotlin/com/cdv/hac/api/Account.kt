package com.cdv.hac.api

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.util.*

const val ASSIGNMENTS_URL = "https://hac.nisd.net/HomeAccess/Content/Student/Assignments.aspx"
const val CLASSES_URL     = "https://hac.nisd.net/HomeAccess/Content/Student/Classes.aspx"
const val TRANSCRIPT_URL  = "https://hac.nisd.net/HomeAccess/Content/Student/Registration.aspx"
const val LOGIN_URL       = "https://hac.nisd.net/HomeAccess/Account/LogOn?ReturnUrl=%2fHomeAccess%2f"
const val BASE_URL        = "https://hac.nisd.net"

class Account(private var username: String, private var password: String) {

    private val cookies = mutableMapOf<String, String>()



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
    
    fun resetCache() {
        cachedTranscript = null
        cachedAssignments.clear()
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    fun getClasses(quarter: Int): List<Class> {
        return cachedAssignments.getOrPut(quarter) { getClassesFromDocument(quarter) }
    }

    fun returnWeightedGpa(): Float {
        return _returnRegistrationTableContents()[0]
            ?.text()?.trim()?.filter { it.isDigit() || it == '.'}?.toFloat() ?: -1f
    }

    fun returnEstimatedQuarterGPA(quarter: Int, weighted: Boolean = true): Double {
        val classes = getClasses(quarter)
        val weightedAverages = mutableListOf<Double>()
        for ((name, _, _, _, displayedAverage) in classes) {
            val extra = if(weighted) getExtraPointsForClass(name) else 0.0
            if(displayedAverage != 0.0)
                weightedAverages.add(displayedAverage + extra)
        }
        return weightedAverages.average()
    }

    fun getExtraPointsForClass(str: String): Double {
        return if(str.contains("AP ")) 8.0
        else if (str.contains(" AP")) 8.0
        else if (str.contains("H ")) 5.0
        else if (str.contains(" H")) 5.0
        else if (str.contains("Adv ")) 5.0
        else if (str.contains(" Adv")) 5.0
        else if (str.contains("Advanced ")) 5.0
        else if (str.contains(" Advanced")) 5.0
        else { 0.0 }
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
        return _returnContactTableContents().first()?.getElementsByTag("tr")?.first()?.getElementsByTag("td")[0]?.text()?.split(',')[0] ?: ""
    }

    fun returnCollegeGpa(): Float {
        return (returnWeightedGpa() / 100) * 4.0f
    }

    fun getUsername(): String = username


    // End API
    
    private val cachedAssignments = mutableMapOf<Int, List<Class>>()

    private fun getClassesFromDocument(quarter: Int? = null): List<Class> {
        val classes = mutableListOf<Class>()
        val quarter: Int = quarter ?: -1
        val doc = Jsoup.parse(fetchAssignmentsPage(quarter))
        val scheduleDoc = Jsoup.parse(fetchClassesPage())
        val data = parseHacData(doc)

        val scheduleInfo = mutableMapOf<String, Map<String, Any>>()

        scheduleDoc.selectFirst("table[id~=plnMain_dgSchedule]")?.run {
            select("tr.sg-asp-table-data-row").mapNotNull { row ->
                val raw = row.select("td")
                val rawstr = raw.toString()
                val email = rawstr.substring(rawstr.indexOfFirst { c-> c == ':' } + 1, rawstr.length - 1).let { str -> str.substring(0, str.indexOfFirst { c -> c == ' ' }) }
                val cells = raw.map { it.text().trim() }

                scheduleInfo[cells[1]] = mutableMapOf(
                    "courseId" to cells[0].let { it.substring(0, it.indexOfFirst { c-> c == ' ' }).toIntOrNull() ?: -1 },
                    "period" to cells[2].toInt(),
                    "teacher" to Teacher(cells[3], email),
                    "room" to cells[4]
                )
            }
        }

        data.forEach { map ->
            val className = map["class"] as String

            val categories = (map["categories"] as List<Map<String, Any>>).map { e -> Category(e["name"] as String, e["points"] as? Double ?: 0.0) }

            val assignments = (map["assignments"] as List<Map<String, Any>>).map { e ->
                Assignment(
                    e["title"] as String,
                    Date((e["dateDue"] as String).ifEmpty { "01/01/1999" }),
                    Date((e["dateAssigned"] as String).ifEmpty { "01/01/1999" }),
                    categories.find { c -> c.name == e["category"] as String } ?: Category("MISSING", 0.0),
                    e["score"] as? Double ?: 0.0,
                    e["totalPoints"] as? Double ?: 0.0,
                    e["weightedScore"] as? Double ?: 0.0,
                    e["weight"] as? Double ?: 0.0,
                    e["weightedTotalPoints"] as? Double ?: 0.0,
                    e["averageScore"] as? Double ?: 0.0,
                    )
            }

            classes.add(Class(
                className,
                assignments,
                categories,
                scheduleInfo[className]?.get("period") as? Int ?: -1,
                (map["average"] as String).toDoubleOrNull() ?: 0.0, (scheduleInfo[className]?.get("teacher")) as Teacher, scheduleInfo[className]?.get("room") as? String ?: "", scheduleInfo[className]?.get("courseId") as? Int ?: -1,
            ))
        }


        return classes
    }

    private fun fetchAssignmentsPage(quarter: Int?): String {
        val html = get(ASSIGNMENTS_URL, referer = ASSIGNMENTS_URL, cookies = cookies)
        if (quarter == null || quarter == -1 /* placeholder for default html used for cached map indexing in #getCachedAssignments */)
            return html

        val doc = Jsoup.parse(html)
        val quarterValue = getQuarterValue(doc, quarter)
        val payload = getFormTokens(doc).toMutableMap().apply {
            put("ctl00\$plnMain\$ddlReportCardRuns", quarterValue)
            put("ctl00\$plnMain\$ddlClasses", "ALL")
            put("ctl00\$plnMain\$ddlOrderBy", "Class")
            put("__EVENTTARGET", "ctl00\$plnMain\$btnRefreshView")
        }
        return post(ASSIGNMENTS_URL, payload, referer = ASSIGNMENTS_URL, cookies = cookies)
    }

    private fun fetchClassesPage(): String {
        val html = get(CLASSES_URL, referer = CLASSES_URL, cookies = cookies)

        val doc = Jsoup.parse(html)
        val payload = getFormTokens(doc)
        return post(CLASSES_URL, payload, referer = CLASSES_URL, cookies = cookies)
    }

    private var cachedTranscript: Document? = null

    fun getTranscript(): Document {
        return cachedTranscript ?: Jsoup.parse(fetchTranscript()).also { cachedTranscript = it }
    }

    private fun fetchTranscript(): String = get(TRANSCRIPT_URL, referer = TRANSCRIPT_URL, cookies = cookies)

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

    private fun _returnRegistrationTableContents(): Elements {
        val doc = getTranscript()
        return doc
            .getElementById("MainContent")
            ?.getElementById("fmMain")
            ?.getElementById("plnMain_lblDDDisplay")
            ?.getElementById("plnMain_dlDDDisplay")
            ?.getElementsByClass("sg-standard-width")!!
    }

    private fun _returnContactTableContents(): Elements {
        val doc = getTranscript()

        return doc.getElementById("MainContent")
            ?.getElementById("fmMain")
            ?.getElementById("plnMain_lblContact")
            ?.getElementById("tblContactHeader")
            ?.getElementById("plnMain_dvContact")
            ?.getElementById("plnMain_dlContacts")
            ?.getElementsByTag("tbody")!!
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
                    "dateDue"     to cells[0],
                    "dateAssigned"     to cells[1],
                    "title"    to cells[2],
                    "category" to category,
                    "score"    to getSafeDoubleFromString(cells[4]),
                    "totalPoints" to getSafeDoubleFromString(cells[5]),
                    "weight" to getSafeDoubleFromString(cells[6]),
                    "weightedScore" to getSafeDoubleFromString(cells[7]),
                    "weightedTotalPoints" to getSafeDoubleFromString(cells[8]),
                    "averageScore" to getSafeDoubleFromString(cells[9]),
                )
            }

            val categories: MutableList<Map<String, Any>> = mutableListOf()
            classDiv.selectFirst("span[class~=LabelCatogery]").run {
                if(this == null) return@run
                val cells = select("td").map { it.text().trim() }
                val trimmedCells = cells.subList(6, cells.size - 1).let { it.subList(0, it.indexOfFirst { s -> s == "Total Points:" }) }

                trimmedCells.chunked(6) { o ->
                    categories.add(mapOf(
                        "name" to o[0],
                        "points" to getSafeDoubleFromString(o[4])
                    ))
                }
            }

            mapOf(
                "class"       to className,
                "average"     to classAvg,
                "assignments" to assignments,
                "categories" to categories
            )
        }
    }

    fun getSafeDoubleFromString(str: String): Double {
       return str.toDoubleOrNull()
           ?: str.substringBefore(".").toDoubleOrNull()
           ?: -1.0
    }
}