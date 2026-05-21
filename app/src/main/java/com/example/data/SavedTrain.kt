package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_trains")
data class SavedTrain(
    @PrimaryKey val trainNo: String,
    val trainName: String,
    val lastSearched: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
