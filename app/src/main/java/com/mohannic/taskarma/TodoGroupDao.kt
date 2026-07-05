package com.mohannic.taskarma

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoGroupDao {
    @Query("SELECT * FROM todo_groups ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<TodoGroup>>

    @Query("SELECT * FROM todo_groups WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): TodoGroup?

    @Query("SELECT * FROM todo_groups ORDER BY sortOrder ASC, createdAt ASC LIMIT 1")
    suspend fun getDefaultGroup(): TodoGroup?

    @Insert
    suspend fun insert(group: TodoGroup): Long

    @Update
    suspend fun update(group: TodoGroup)

    @Delete
    suspend fun delete(group: TodoGroup)

    @Query("SELECT COUNT(*) FROM todo_groups")
    suspend fun getCount(): Int

    /**
     * Runtime safety net: ensures at least one group exists.
     * Returns the id of the default group.
     */
    suspend fun ensureDefaultGroup(): Long {
        val existing = getDefaultGroup()
        if (existing != null) return existing.id
        return insert(TodoGroup(name = "My Tasks", sortOrder = 0))
    }
}
