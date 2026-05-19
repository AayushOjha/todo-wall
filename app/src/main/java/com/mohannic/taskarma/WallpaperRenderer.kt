package com.mohannic.taskarma

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
private const val HORIZONTAL_PADDING = 100f
private const val TOP_PADDING = 300f
private const val TITLE_SIZE = 110f
private const val ITEM_SIZE = 54f
private const val LINE_SPACING = 100f

fun renderTodosToBitmap(context: Context, todos: List<Todo>): Bitmap {
    val wallpaperManager = WallpaperManager.getInstance(context)
    val width = wallpaperManager.desiredMinimumWidth.takeIf { it > 0 } ?: FALLBACK_WIDTH
    val height = wallpaperManager.desiredMinimumHeight.takeIf { it > 0 } ?: FALLBACK_HEIGHT

    val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Brand background: Deep Slate
    canvas.drawColor("#101418".toColorInt())

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#3D8BFF".toColorInt() // Electric Blue
        textSize = TITLE_SIZE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val itemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = ITEM_SIZE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val donePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.STRIKE_THRU_TEXT_FLAG).apply {
        color = "#94A3B8".toColorInt() // Steel
        textSize = ITEM_SIZE
    }

    var y = TOP_PADDING
    canvas.drawText("TASKS", HORIZONTAL_PADDING, y, titlePaint)
    y += LINE_SPACING * 1.8f

    if (todos.isEmpty()) {
        canvas.drawText("All clear for now.", HORIZONTAL_PADDING, y, itemPaint)
    } else {
        todos.forEach { todo ->
            val marker = if (todo.isDone) "✓" else "○"
            val paint = if (todo.isDone) donePaint else itemPaint
            canvas.drawText("$marker   ${todo.text}", HORIZONTAL_PADDING, y, paint)
            y += LINE_SPACING
        }
    }

    return bitmap
}
