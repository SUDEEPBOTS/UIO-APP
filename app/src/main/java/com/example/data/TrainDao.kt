package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainDao {
    @Query("SELECT * FROM saved_trains ORDER BY lastSearched DESC")
    fun getSavedTrains(): Flow<List<SavedTrain>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrain(train: SavedTrain)

    @Update
    suspend fun updateTrain(train: SavedTrain)

    @Delete
    suspend fun deleteTrain(train: SavedTrain)

    @Query("DELETE FROM saved_trains WHERE trainNo = :trainNo")
    suspend fun deleteByNo(trainNo: String)
}
