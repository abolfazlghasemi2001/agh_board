package com.pinboard.keyboard.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PinDao {

    @Query("SELECT * FROM pins")
    fun getAll(): Flow<List<Pin>>

    @Insert
    suspend fun insert(pin: Pin): Long

    @Update
    suspend fun update(pin: Pin)

    @Delete
    suspend fun delete(pin: Pin)
}
