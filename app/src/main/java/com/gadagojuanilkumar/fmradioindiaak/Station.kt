package com.gadagojuanilkumar.fmradioindiaak

data class Station(
    val id: String,
    val name: String,
    val state: String,
    val streamUrl: String,
    val logoUrl: String = ""
)