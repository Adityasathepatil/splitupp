package com.techtool.splitup.models

data class Settlement(
    val from: Member,
    val to: Member,
    val amount: Double
)
