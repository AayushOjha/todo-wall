package com.mohanic.todo

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt

private const val FALLBACK_WIDTH = 1080
private const val FALLBACK_HEIGHT = 1920
private const val HORIZONTAL_PADDING = 80f
private const val TOP_PADDING = 240f
private const val TITLE_SIZE = 96f
private const val ITEM_SIZE = 64f
private const val LINE_SPACING = 96f

fun renderTodosToBitmap(context: Context, todos: List<Todo>): Bitmap {
    val wallpaperManager = WallpaperManager.getInstance(context)
    val width = wallpaperManager.desiredMinimumWidth.takeIf { it > 0 } ?: FALLBACK_WIDTH
    val height = wallpaperManager.desiredMinimumHeight.takeIf { it > 0 } ?: FALLBACK_HEIGHT

    val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor("#101418".toColorInt())

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = TITLE_SIZE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val itemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = ITEM_SIZE
    }
    val donePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.STRIKE_THRU_TEXT_FLAG).apply {
        color = "#888888".toColorInt()
        textSize = ITEM_SIZE
    }

    var y = TOP_PADDING
    canvas.drawText("To-Do", HORIZONTAL_PADDING, y, titlePaint)
    y += LINE_SPACING * 1.5f

    if (todos.isEmpty()) {
        canvas.drawText("Nothing to do.", HORIZONTAL_PADDING, y, itemPaint)
    } else {
        todos.forEach { todo ->
            val marker = if (todo.isDone) "☑" else "☐"
            val paint = if (todo.isDone) donePaint else itemPaint
            canvas.drawText("$marker  ${todo.text}", HORIZONTAL_PADDING, y, paint)
            y += LINE_SPACING
        }
    }

    return bitmap
}
