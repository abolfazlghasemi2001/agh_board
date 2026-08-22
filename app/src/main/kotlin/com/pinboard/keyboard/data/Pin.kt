package com.pinboard.keyboard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pins")
data class Pin(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val text: String,
    val category: String = "عمومی",
    val isFavorite: Boolean = false,
    val useCount: Int = 0,
    val lastUsed: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
