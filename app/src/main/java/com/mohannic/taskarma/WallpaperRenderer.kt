package com.mohannic.taskarma

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt

// ── Layout constants (all in dp, scaled by density at runtime) ──────────────
private const val FALLBACK_WIDTH       = 1080
private const val FALLBACK_HEIGHT      = 1920

private const val TOP_PAD_DP          = 90f   // wallpaper sits behind status bar
private const val H_PAD_DP            = 52f
private const val HEADER_SP           = 22f   // compact header "My Tasks"
private const val ITEM_SP             = 17f   // todo text
private const val ROW_GAP_DP          = 8f    // gap between rows
private const val SECTION_GAP_DP      = 18f   // gap before done section
private const val BULLET_X_DP         = 16f   // bullet offset from H_PAD
private const val TEXT_OFFSET_DP      = 26f   // text start after bullet
private const val DIVIDER_GAP_DP      = 10f   // space after header divider
private const val BOTTOM_SAFE_DP      = 90f   // keep clear of gesture area

/** When total tasks (pending + completed) reach this count, the wallpaper groups pending first. */
private const val BUSY_THRESHOLD = 7

fun renderTodosToBitmap(context: Context, todos: List<Todo>, isDark: Boolean = true): Bitmap {
    val dm      = context.resources.displayMetrics
    val density = dm.density

    val width   = dm.widthPixels.takeIf  { it > 0 } ?: FALLBACK_WIDTH
    val height  = dm.heightPixels.takeIf { it > 0 } ?: FALLBACK_HEIGHT

    val topPad      = TOP_PAD_DP      * density
    val hPad        = H_PAD_DP        * density
    val headerSp    = HEADER_SP       * density
    val itemSp      = ITEM_SP         * density
    val rowGap      = ROW_GAP_DP      * density
    val sectionGap  = SECTION_GAP_DP  * density
    val bulletX     = hPad - BULLET_X_DP * density
    val textOffset  = TEXT_OFFSET_DP  * density
    val dividerGap  = DIVIDER_GAP_DP  * density
    val bottomSafe  = height - BOTTOM_SAFE_DP * density

    val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // ── Background ────────────────────────────────────────────────────────────
    val bgColors = if (isDark) {
        intArrayOf("#0D0F14".toColorInt(), "#0D0F14".toColorInt(), "#13172B".toColorInt())
    } else {
        intArrayOf("#F5F6FF".toColorInt(), "#F0F1FF".toColorInt(), "#E8E6FF".toColorInt())
    }
    canvas.drawPaint(Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            bgColors, floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
        )
    })

    // ── Colour tokens ─────────────────────────────────────────────────────────
    val colorPrimary   = if (isDark) "#8B84FF".toColorInt() else "#5A52D5".toColorInt()
    val colorOnBg      = if (isDark) "#E8EAFF".toColorInt() else "#0D0F14".toColorInt()
    val colorMuted     = if (isDark) "#6B7399".toColorInt() else "#7577A0".toColorInt()
    val colorDone      = if (isDark) Color.argb(100, 180, 185, 220)
                         else        Color.argb(120, 80, 85, 130)
    val colorDivider   = if (isDark) Color.argb(35, 255, 255, 255)
                         else        Color.argb(40, 0, 0, 0)
    val colorBullet    = colorPrimary
    val colorDoneMark  = colorMuted

    // ── Paints ────────────────────────────────────────────────────────────────
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = colorOnBg
        textSize = headerSp
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = colorDivider
        strokeWidth = 1f * density
        style       = Paint.Style.STROKE
    }
    val itemPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = colorOnBg
        textSize = itemSp
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    val donePaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.STRIKE_THRU_TEXT_FLAG).apply {
        color    = colorDone
        textSize = itemSp
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    val bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = colorBullet
        textSize = itemSp * 0.9f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    val doneBulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = colorDoneMark
        textSize = itemSp * 0.85f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    val overflowPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = colorMuted
        textSize = itemSp * 0.85f
        typeface = Typeface.create("sans-serif", Typeface.ITALIC)
    }

    // ── Header ────────────────────────────────────────────────────────────────
    var y = topPad

    // "My Tasks" title
    canvas.drawText("My Tasks", hPad, y, headerPaint)
    y += headerSp * 0.55f

    // divider line
    canvas.drawLine(hPad, y, width - hPad, y, dividerPaint)
    y += dividerGap + itemSp * 0.8f   // compact gap after divider

    // ── Items ─────────────────────────────────────────────────────────────────
    val textWidth    = (width - hPad - textOffset - hPad * 0.4f).toInt().coerceAtLeast(100)
    val pending      = todos.filter { !it.isDone }
    val done         = todos.filter { it.isDone }

    fun drawItem(todo: Todo, isDone: Boolean): Boolean {
        val paint  = if (isDone) donePaint else itemPaint
        val bPaint = if (isDone) doneBulletPaint else bulletPaint
        val bullet = if (isDone) "✓" else "·"

        val layout = StaticLayout.Builder
            .obtain(todo.text, 0, todo.text.length, paint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(2)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setLineSpacing(0f, 1.1f)
            .setIncludePad(false)
            .build()

        val blockHeight = layout.height.toFloat() + rowGap

        if (y + blockHeight > bottomSafe) return false  // no space, stop

        // bullet baseline aligns with first text baseline
        val baseline0 = y + layout.getLineBaseline(0)
        canvas.drawText(bullet, bulletX, baseline0, bPaint)

        canvas.save()
        canvas.translate(hPad + textOffset - TEXT_OFFSET_DP * density, y)
        layout.draw(canvas)
        canvas.restore()

        y += blockHeight
        return true
    }

    if (todos.isEmpty()) {
        canvas.drawText("No tasks — enjoy your day  ✓", hPad, y, donePaint)
    } else if (todos.size >= BUSY_THRESHOLD) {
        // ── Busy mode: 7+ total tasks → show pending first, then completed ──
        for (todo in pending) {
            if (!drawItem(todo, isDone = false)) {
                val remaining = pending.size - pending.indexOf(todo)
                canvas.drawText("+ $remaining more pending", hPad, y, overflowPaint)
                return bitmap
            }
        }
        // Completed tasks below pending
        if (done.isNotEmpty() && y < bottomSafe) {
            y += sectionGap * 0.3f
            canvas.drawLine(hPad, y, width - hPad, y, dividerPaint)
            y += dividerGap + itemSp * 0.5f
            for (todo in done) {
                if (!drawItem(todo, isDone = true)) {
                    val remaining = done.size - done.indexOf(todo)
                    canvas.drawText("+ $remaining more done", hPad, y, overflowPaint)
                    return bitmap
                }
            }
        }
    } else {
        // ── Relaxed mode: <7 total → preserve original task order ─────────────
        // Render ALL todos in their natural order (pending and done interleaved
        // exactly as the user created them), then append a done-summary footer.
        for (todo in todos) {
            if (!drawItem(todo, isDone = todo.isDone)) {
                val remaining = todos.size - todos.indexOf(todo)
                canvas.drawText("+ $remaining more", hPad, y, overflowPaint)
                return bitmap
            }
        }
        // If there were done tasks and we still have room, show a tally footer
        if (done.isNotEmpty() && y < bottomSafe) {
            y += sectionGap * 0.3f
            canvas.drawLine(hPad, y, width - hPad, y, dividerPaint)
            y += dividerGap + itemSp * 0.5f
            canvas.drawText("${done.size} of ${todos.size} tasks done", hPad, y, overflowPaint)
        }
    }

    return bitmap
}
