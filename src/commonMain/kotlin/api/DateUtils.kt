package com.cdv.hac.api

import java.time.Year
import java.util.Date

fun getSchoolYear(): Int {
    val year = Year.now().value
    return if (year % 2 == 0) {
        year
    } else {
        year - 1
    }
}

fun getCurrentQuarter() {
    TODO("Get quarter from month")
}