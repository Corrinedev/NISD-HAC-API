package com.cdv.hac

fun main(args: Array<String>) {
    val root = args[0]
    when(root) {
        "help" -> {
            help()
        }
    }
}

fun help() {
    println("todo: create help")
}