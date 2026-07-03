package com.example

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    indices = [
        Index(value = ["packageName"]),
        Index(value = ["startTimeUtc"]),
        Index(value = ["syncStatus"]),
        Index(value = ["startTimeUtc", "syncStatus"])
    ]
)
data class SessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "packageName")
    val packageName: String,

    @ColumnInfo(name = "appName")
    val appName: String?,

    @ColumnInfo(name = "startTimeUtc")
    val startTimeUtc: Long,

    @ColumnInfo(name = "endTimeUtc")
    val endTimeUtc: Long,

    @ColumnInfo(name = "durationSeconds")
    val durationSeconds: Long,

    @ColumnInfo(name = "syncStatus")
    val syncStatus: String,

    @ColumnInfo(name = "createdAtUtc")
    val createdAtUtc: Long,

    @ColumnInfo(name = "errorMessage")
    val errorMessage: String?
)
