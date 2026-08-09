package com.gatheredthoughts.voicenotes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val transcript: String,
    val title: String,
    val category: String,
    val createdAt: Long
)
