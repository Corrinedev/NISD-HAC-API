package com.cdv.hac.api

import kotlinx.datetime.LocalDate

data class Class(
    val name: String,
    val assignments: List<Assignment>,
    val categories: List<Category>,
    val classPeriod: IntRange,
    val displayedAverage: Double,
    val teacher: Teacher,
    val room: String,
    val classId: Int
)

data class Assignment(
    val name: String,
    val dateDue: LocalDate,
    val dateAssigned: LocalDate,
    val category: Category,
    val score: Score,
    val totalPoints: Double,
    val weightedScore: Double?,
    val weight: Double,
    val weightedTotalPoints: Double,
    val averageScore: Double?,
    val dropped: Boolean = false
)

data class Score(val score: Double?, val missing: Boolean = false, val excused: Boolean = false) {
    override fun toString(): String {
        if(missing) return "M - Missing"
        if(excused) return "EX - Excused"
        if(score == null) return "Ungraded"
        return "$score"
    }
}

data class Category(val name: String, val points: Double)

data class Teacher(val name: String, val email: String) {
    override fun toString(): String {
       return "Name: $name, Email: $email"
    }
}
