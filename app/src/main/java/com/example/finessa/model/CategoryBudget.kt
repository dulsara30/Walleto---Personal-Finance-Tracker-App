package com.example.finessa.model

import java.io.Serializable

data class CategoryBudget(
    val category: String,
    var budget: Double,
    var spent: Double = 0.0
) : Serializable 