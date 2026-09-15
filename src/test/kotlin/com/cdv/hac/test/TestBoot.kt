package com.cdv.hac.test

import com.cdv.hac.api.Account
import java.util.Scanner

fun main() {
    val input = Scanner(System.`in`)

    print("Input user: ") ; val user = input.nextLine()
    print("Input pass: ") ; val pass = input.nextLine()

    val acc = Account(user, pass)
    //print("Current Assignments: ") ; println(acc.returnCurrentAssignmentsHtml())

    acc.getClassesFromDocument()

    //println("Time Elapsed: ${System.currentTimeMillis() - preTime2}ms")
}

