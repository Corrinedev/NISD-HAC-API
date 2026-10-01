package com.cdv.hac.api

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock


fun getSchoolYear(): Int {
    val year = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
    return if (year % 2 == 0) {
        year
    } else {
        year - 1
    }
}

fun getCurrentQuarter() {
    TODO("Get quarter from month")
}