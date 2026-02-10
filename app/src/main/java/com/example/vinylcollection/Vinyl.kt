package com.example.vinylcollection

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vinyls")
data class Vinyl(
    @field:PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String,
    val year: Int?,
    val genre: String,
    val label: String,
    val rating: Int?,
    val condition: String,
    val notes: String,
    val coverUri: String? = null
)
