package com.mohannic.taskarma

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Todo::class, TodoGroup::class], version = 3, exportSchema = false)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun todoGroupDao(): TodoGroupDao

    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase? = null

        /** Migration v1→v2: adds the isArchived column with default 0 (false). */
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Migration v2→v3: adds groups and a groupId foreign key on todos. */
        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS todo_groups (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_todo_groups_name
                    ON todo_groups(name)
                    """.trimIndent()
                )
                db.execSQL(
                    "INSERT INTO todo_groups (name, sortOrder, createdAt) VALUES ('My Tasks', 0, ${System.currentTimeMillis()})"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS todos_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        text TEXT NOT NULL,
                        isDone INTEGER NOT NULL DEFAULT 0,
                        isArchived INTEGER NOT NULL DEFAULT 0,
                        groupId INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(groupId) REFERENCES todo_groups(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO todos_new (id, text, isDone, isArchived, groupId)
                    SELECT id, text, isDone, isArchived, last_insert_rowid() FROM todos
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE todos")
                db.execSQL("ALTER TABLE todos_new RENAME TO todos")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_todos_groupId ON todos(groupId)")
            }
        }

        /**
         * Callback that seeds a default "My Tasks" group when the database
         * is created from scratch (fresh install). Migration 2→3 handles
         * the seed for existing users who upgrade.
         */
        private val seedCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL(
                    "INSERT OR IGNORE INTO todo_groups (name, sortOrder, createdAt) " +
                    "VALUES ('My Tasks', 0, ${System.currentTimeMillis()})"
                )
            }
        }

        fun getInstance(context: Context): TodoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "todo.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(seedCallback)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
