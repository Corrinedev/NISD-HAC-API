package com.cdv.hac.api

data class WeightedValue(val value: Double, val weight: Double)

fun List<WeightedValue>.weightedAverage(): Double {
    val totalWeight = sumOf { it.weight }
    return sumOf { it.value * it.weight } / totalWeight
}
