package com.cdv.hac.test

import com.cdv.hac.api.Account
import java.util.Scanner

fun main() {
    val input = Scanner(System.`in`)

    print("Input user: ") ; val user = input.nextLine()
    print("Input pass: ") ; val pass = input.nextLine()

    val acc = Account(user, pass)

    println(acc.getClasses(1))
}

