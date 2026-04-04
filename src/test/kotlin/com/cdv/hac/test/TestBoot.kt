package com.cdv.hac.test

import com.cdv.hac.api.Account
import java.util.Scanner

fun main() {
    val input = Scanner(System.`in`)

    print("Input user: ") ; val user = input.nextLine()
    print("Input pass: ") ; val pass = input.nextLine()

    val acc = Account(user, pass)

    val preTime1 = System.currentTimeMillis()
    print("Weighted GPA: ") ; println(acc.returnWeightedGpa())
    println("Time Elapsed: ${System.currentTimeMillis() - preTime1}ms")

    val preTime2 = System.currentTimeMillis()
    print("Current Assignments: ") ; println(acc.returnCurrentAssignmentsHtml())
    println("Time Elapsed: ${System.currentTimeMillis() - preTime2}ms")
}