package com.xbjornsen.emftracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val startTime: Long,
    val endTime: Long,
    val peak: Float,
    val average: Float,
    val readingCount: Int
)
