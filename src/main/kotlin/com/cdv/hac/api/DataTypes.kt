package com.cdv.hac.api

import java.util.Date

data class Class(
    val name: String,
    val assignments: List<Assignment>,
    val categories: List<Category>,
    val classPeriod: Int,
    val displayedAverage: Double
) {

}

data class Assignment(
    val name: String,
    val dateDue: Date,
    val dateAssigned: Date,
    val category: Category,
    val score: Double,
    val totalPoints: Double,
    val weightedScore: Double,
    val weight: Double,
    val weightedTotalPoints: Double,
    val averageScore: Double
)

data class Category(val name: String, val points: Double)
