package com.mohannic.taskarma

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class TodoGroupDaoTest {
    private lateinit var db: TodoDatabase
    private lateinit var todoDao: TodoDao
    private lateinit var groupDao: TodoGroupDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TodoDatabase::class.java).build()
        todoDao = db.todoDao()
        groupDao = db.todoGroupDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun observeByGroup_returnsOnlyMatchingTodos() = runBlocking {
        val groupId = groupDao.insert(TodoGroup(name = "Work"))
        todoDao.insert(Todo(text = "Work task", groupId = groupId))
        todoDao.insert(Todo(text = "Personal task", groupId = 999L))

        val todos = todoDao.observeByGroup(groupId).first()
        assert(todos.size == 1)
        assert(todos.first().text == "Work task")
    }

    @Test
    fun deleteGroup_deletesAssociatedTodos() = runBlocking {
        val groupId = groupDao.insert(TodoGroup(name = "Temp"))
        todoDao.insert(Todo(text = "Temp task", groupId = groupId))

        groupDao.delete(TodoGroup(id = groupId, name = "Temp"))

        val todos = todoDao.observeByGroup(groupId).first()
        assert(todos.isEmpty())
    }
}
