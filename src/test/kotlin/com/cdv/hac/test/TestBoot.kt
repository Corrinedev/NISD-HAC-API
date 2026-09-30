package com.cdv.hac.test

import com.cdv.hac.api.Account
import com.cdv.hac.api.Assignment
import java.sql.Date
import java.time.Instant
import java.util.Scanner

fun main() {
    val input = Scanner(System.`in`)

    print("Input user: ") ; val user = input.nextLine()
    print("Input pass: ") ; val pass = input.nextLine()

    val acc = Account(user, pass)

    val classes = acc.getClasses(1)

    val nowDate = Date.from(Instant.now())
    val importantAssignments = mutableListOf<Assignment>()
    for (klass in classes) {
        val filterAfterNow = klass.assignments.filter { a ->
            a.dateDue.after(nowDate)
        }.ifEmpty { continue }
        importantAssignments.addAll(filterAfterNow)
    }

    importantAssignments.sortBy { it.dateDue }

    for ((name, dateDue, dateAssigned, category, _, _, _, _, _, _) in importantAssignments) {
        println("[$name]")
        println("{---Due: $dateDue")
        println("{---Category: ${category.name}")
    }

    println(acc.returnEstimatedQuarterGPA(1))
}

