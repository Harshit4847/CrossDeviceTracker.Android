package com.example

data class SessionUploadDto(
    val id: String,
    val packageName: String,
    val appName: String?,
    val startTimeUtc: String,
    val endTimeUtc: String,
    val durationSeconds: Long,
    val createdAtUtc: String
)
