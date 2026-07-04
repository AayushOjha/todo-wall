package com.mohannic.taskarma

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todos",
    foreignKeys = [
        ForeignKey(
            entity = TodoGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["groupId"])]
)
data class Todo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val text: String,
    val isDone: Boolean = false,
    val isArchived: Boolean = false,
    val groupId: Long,
)
