package com.gadagojuanilkumar.fmradioindiaak

data class FmEntry(
    val state: String,
    val city: String,
    val frequency: String,
    val name: String,
    val remarks: String = "",
    val streamUrl: String = "",
    val streamName: String = ""
)