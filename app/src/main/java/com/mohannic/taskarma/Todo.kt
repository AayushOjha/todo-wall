package com.mohannic.taskarma

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val text: String,
    val isDone: Boolean = false,
)
