package com.mohannic.taskarma

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

class TodoDatabaseMigrationTest {
    private val testDb = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TodoDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate2To3() {
        helper.createDatabase(testDb, 2).apply {
            execSQL("INSERT INTO todos (text, isDone, isArchived) VALUES ('Buy milk', 0, 0)")
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 3, true, TodoDatabase.MIGRATION_2_3)
        db.query("SELECT name FROM todo_groups").use { cursor ->
            assert(cursor.moveToFirst())
            assert(cursor.getString(0) == "My Tasks")
        }
        db.query("SELECT groupId FROM todos").use { cursor ->
            assert(cursor.moveToFirst())
            assert(cursor.getLong(0) > 0)
        }
    }
}
