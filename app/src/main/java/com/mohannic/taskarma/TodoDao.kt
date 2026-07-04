package com.mohannic.taskarma

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos WHERE isArchived = 0 ORDER BY id ASC")
    fun observeAll(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE isArchived = 1 ORDER BY id DESC")
    fun observeArchived(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE isArchived = 0 AND groupId = :groupId ORDER BY id ASC")
    fun observeByGroup(groupId: Long): Flow<List<Todo>>

    @Insert
    suspend fun insert(todo: Todo): Long

    @Update
    suspend fun update(todo: Todo)

    @Delete
    suspend fun delete(todo: Todo)

    @Query("UPDATE todos SET isArchived = 1 WHERE id = :id")
    suspend fun archiveById(id: Long)

    @Query("UPDATE todos SET isArchived = 0 WHERE id = :id")
    suspend fun unarchiveById(id: Long)
}
