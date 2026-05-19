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
private const val HORIZONTAL_PADDING = 120f
private const val TOP_PADDING = 400f
private const val TITLE_SIZE = 80f
private const val ITEM_SIZE = 50f
private const val LINE_SPACING = 110f

fun renderTodosToBitmap(context: Context, todos: List<Todo>): Bitmap {
    val wallpaperManager = WallpaperManager.getInstance(context)
    val width = wallpaperManager.desiredMinimumWidth.takeIf { it > 0 } ?: FALLBACK_WIDTH
    val height = wallpaperManager.desiredMinimumHeight.takeIf { it > 0 } ?: FALLBACK_HEIGHT

    val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Clean Dark Background (Google Dark Style)
    canvas.drawColor("#121212".toColorInt())

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1A73E8".toColorInt() // Google Blue
        textSize = TITLE_SIZE
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    val itemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = ITEM_SIZE
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    val donePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.STRIKE_THRU_TEXT_FLAG).apply {
        color = "#80FFFFFF".toColorInt() // Semi-transparent white
        textSize = ITEM_SIZE
    }
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#20FFFFFF".toColorInt()
        strokeWidth = 2f
    }

    var y = TOP_PADDING
    canvas.drawText("TASKS", HORIZONTAL_PADDING, y, titlePaint)
    y += LINE_SPACING * 0.8f
    canvas.drawLine(HORIZONTAL_PADDING, y, width - HORIZONTAL_PADDING, y, dividerPaint)
    y += LINE_SPACING * 1.2f

    if (todos.isEmpty()) {
        canvas.drawText("No pending tasks", HORIZONTAL_PADDING, y, itemPaint)
    } else {
        // Only show first 10-12 tasks to avoid overflow
        todos.take(12).forEach { todo ->
            val paint = if (todo.isDone) donePaint else itemPaint
            val bullet = if (todo.isDone) "✓ " else "○ "
            canvas.drawText("$bullet ${todo.text}", HORIZONTAL_PADDING, y, paint)
            y += LINE_SPACING
        }
        
        if (todos.size > 12) {
            canvas.drawText("... and ${todos.size - 12} more", HORIZONTAL_PADDING, y, donePaint)
        }
    }

    return bitmap
}
