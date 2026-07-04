package com.mohannic.taskarma

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todo_groups",
    indices = [Index(value = ["name"], unique = true)]
)
data class TodoGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
