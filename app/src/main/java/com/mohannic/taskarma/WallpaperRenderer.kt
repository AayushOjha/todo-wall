package com.mohannic.taskarma

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt

private const val FALLBACK_WIDTH = 1080
private const val FALLBACK_HEIGHT = 1920
private const val HORIZONTAL_PADDING = 120f
private const val TOP_PADDING = 400f
private const val TITLE_SIZE = 80f
private const val ITEM_SIZE = 50f
private const val LINE_SPACING = 110f
private const val BULLET_SPACING = 80f

fun renderTodosToBitmap(context: Context, todos: List<Todo>): Bitmap {
    val displayMetrics = context.resources.displayMetrics
    val width = displayMetrics.widthPixels.takeIf { it > 0 } ?: FALLBACK_WIDTH
    val height = displayMetrics.heightPixels.takeIf { it > 0 } ?: FALLBACK_HEIGHT

    val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Clean Dark Background (Google Dark Style)
    canvas.drawColor("#121212".toColorInt())

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1A73E8".toColorInt() // Google Blue
        textSize = TITLE_SIZE
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    
    val itemPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = ITEM_SIZE
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    
    val donePaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.STRIKE_THRU_TEXT_FLAG).apply {
        color = "#80FFFFFF".toColorInt() // Semi-transparent white
        textSize = ITEM_SIZE
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
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

    val availableWidth = width - 2 * HORIZONTAL_PADDING - BULLET_SPACING
    val maxRenderHeight = height - 250f // Safe bottom margin above dock/navigation bar

    if (todos.isEmpty()) {
        canvas.drawText("No pending tasks", HORIZONTAL_PADDING, y, itemPaint)
    } else {
        for (i in todos.indices) {
            val todo = todos[i]
            val paint = if (todo.isDone) donePaint else itemPaint
            val bullet = if (todo.isDone) "✓ " else "○ "
            
            // Build StaticLayout for wrapped task text
            val staticLayout = StaticLayout.Builder.obtain(todo.text, 0, todo.text.length, paint, availableWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(2)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setLineSpacing(0f, 1.1f)
                .setIncludePad(false)
                .build()

            val lineCount = staticLayout.lineCount
            val itemHeight = if (lineCount > 1) {
                staticLayout.getLineBaseline(lineCount - 1) - staticLayout.getLineBaseline(0) + LINE_SPACING
            } else {
                LINE_SPACING
            }

            // Check if this item and potential overflow indicator would exceed the safe render height
            val remainingTasks = todos.size - i
            val overflowIndicatorHeight = if (remainingTasks > 1) LINE_SPACING else 0f
            if (y + itemHeight + overflowIndicatorHeight > maxRenderHeight) {
                canvas.drawText("... and $remainingTasks more", HORIZONTAL_PADDING, y, donePaint)
                break
            }

            // Draw bullet
            canvas.drawText(bullet, HORIZONTAL_PADDING, y, paint)

            // Draw text layout aligned to the bullet's baseline
            canvas.save()
            canvas.translate(HORIZONTAL_PADDING + BULLET_SPACING, y - staticLayout.getLineBaseline(0))
            staticLayout.draw(canvas)
            canvas.restore()

            y += itemHeight
        }
    }

    return bitmap
}
