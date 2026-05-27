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
private const val HORIZONTAL_PADDING_DP = 40f
private const val TOP_PADDING_DP = 150f
private const val TITLE_SIZE_SP = 32f
private const val ITEM_SIZE_SP = 20f
private const val LINE_SPACING_DP = 44f
private const val BULLET_SPACING_DP = 30f

fun renderTodosToBitmap(context: Context, todos: List<Todo>): Bitmap {
    val displayMetrics = context.resources.displayMetrics
    val density = displayMetrics.density
    
    // Scale dimensions based on density
    val horizontalPadding = HORIZONTAL_PADDING_DP * density
    val topPadding = TOP_PADDING_DP * density
    val titleSize = TITLE_SIZE_SP * density
    val itemSize = ITEM_SIZE_SP * density
    val lineSpacing = LINE_SPACING_DP * density
    val bulletSpacing = BULLET_SPACING_DP * density

    // Use exact screen dimensions to prevent scrolling on multi-page home screens
    val width = displayMetrics.widthPixels.takeIf { it > 0 } ?: FALLBACK_WIDTH
    val height = displayMetrics.heightPixels.takeIf { it > 0 } ?: FALLBACK_HEIGHT

    val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Clean Dark Background (Google Dark Style)
    canvas.drawColor("#121212".toColorInt())

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1A73E8".toColorInt() // Google Blue
        textSize = titleSize
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    
    val itemPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = itemSize
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    
    val donePaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.STRIKE_THRU_TEXT_FLAG).apply {
        color = "#80FFFFFF".toColorInt() // Semi-transparent white
        textSize = itemSize
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#20FFFFFF".toColorInt()
        strokeWidth = 2f * density
    }

    var y = topPadding
    canvas.drawText("TASKS", horizontalPadding, y, titlePaint)
    y += lineSpacing * 0.8f
    canvas.drawLine(horizontalPadding, y, width - horizontalPadding, y, dividerPaint)
    y += lineSpacing * 1.2f

    val availableWidth = (width - 2 * horizontalPadding - bulletSpacing).coerceAtLeast(100f)
    val maxRenderHeight = height - (100f * density) // Safe bottom margin

    if (todos.isEmpty()) {
        canvas.drawText("No pending tasks", horizontalPadding, y, itemPaint)
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
                staticLayout.getLineBaseline(lineCount - 1) - staticLayout.getLineBaseline(0) + lineSpacing
            } else {
                lineSpacing
            }

            // Check if this item and potential overflow indicator would exceed the safe render height
            val remainingTasks = todos.size - i
            val overflowIndicatorHeight = if (remainingTasks > 1) lineSpacing else 0f
            if (y + itemHeight + overflowIndicatorHeight > maxRenderHeight) {
                canvas.drawText("... and $remainingTasks more", horizontalPadding, y, donePaint)
                break
            }

            // Draw bullet
            canvas.drawText(bullet, horizontalPadding, y, paint)

            // Draw text layout aligned to the bullet's baseline
            canvas.save()
            canvas.translate(horizontalPadding + bulletSpacing, y - staticLayout.getLineBaseline(0))
            staticLayout.draw(canvas)
            canvas.restore()

            y += itemHeight
        }
    }

    return bitmap
}
