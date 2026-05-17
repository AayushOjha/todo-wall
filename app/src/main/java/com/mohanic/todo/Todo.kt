package com.mohanic.todo

data class Todo(
    val id: Long,
    val text: String,
    val isDone: Boolean = false,
)
