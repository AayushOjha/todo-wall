package com.mohannic.taskarma

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt

private const val FALLBACK_WIDTH         = 1080
private const val FALLBACK_HEIGHT        = 1920
private const val HORIZONTAL_PADDING_DP  = 48f
private const val TOP_PADDING_DP         = 160f
private const val TITLE_SIZE_SP          = 28f
private const val SUBTITLE_SIZE_SP       = 14f
private const val ITEM_SIZE_SP           = 19f
private const val LINE_SPACING_DP        = 52f
private const val BULLET_SPACING_DP      = 32f
private const val CARD_CORNER_DP         = 18f
private const val CARD_H_PADDING_DP      = 18f
private const val CARD_V_PADDING_DP      = 14f

fun renderTodosToBitmap(context: Context, todos: List<Todo>, isDark: Boolean = true): Bitmap {
    val dm      = context.resources.displayMetrics
    val density = dm.density

    val hPad        = HORIZONTAL_PADDING_DP * density
    val topPad      = TOP_PADDING_DP        * density
    val titleSize   = TITLE_SIZE_SP         * density
    val subtitleSize= SUBTITLE_SIZE_SP      * density
    val itemSize    = ITEM_SIZE_SP          * density
    val lineSpacing = LINE_SPACING_DP       * density
    val bulletSpacing= BULLET_SPACING_DP   * density
    val cardCorner  = CARD_CORNER_DP        * density
    val cardHPad    = CARD_H_PADDING_DP     * density
    val cardVPad    = CARD_V_PADDING_DP     * density

    val width  = dm.widthPixels.takeIf  { it > 0 } ?: FALLBACK_WIDTH
    val height = dm.heightPixels.takeIf { it > 0 } ?: FALLBACK_HEIGHT

    val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // ── Background ───────────────────────────────────────────────────────────
    if (isDark) {
        // Deep navy gradient
        val bgGrad = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(
                "#0D0F14".toColorInt(),
                "#0D0F14".toColorInt(),
                "#161924".toColorInt()
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPaint(Paint().apply { shader = bgGrad })
    } else {
        // Soft light gradient
        val bgGrad = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(
                "#F5F6FF".toColorInt(),
                "#EEEEFF".toColorInt(),
                "#E8E6FF".toColorInt()
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPaint(Paint().apply { shader = bgGrad })
    }

    // ── Decorative accent blob (top-right) ───────────────────────────────────
    val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        val blobColor = if (isDark) Color.argb(40, 108, 99, 255)
                        else        Color.argb(30, 108, 99, 255)
        shader = android.graphics.RadialGradient(
            width * 0.85f, height * 0.08f,
            width * 0.45f,
            intArrayOf(blobColor, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(width * 0.85f, height * 0.08f, width * 0.45f, blobPaint)

    // ── Colour tokens ─────────────────────────────────────────────────────────
    val colorPrimary   = if (isDark) "#8B84FF".toColorInt() else "#6C63FF".toColorInt()
    val colorOnBg      = if (isDark) "#F0F2FF".toColorInt() else "#0D0F14".toColorInt()
    val colorMuted     = if (isDark) "#8892B0".toColorInt() else "#64678A".toColorInt()
    val colorCard      = if (isDark) Color.argb(200, 30, 34, 51)
                         else        Color.argb(220, 255, 255, 255)
    val colorCardDone  = if (isDark) Color.argb(100, 22, 25, 36)
                         else        Color.argb(140, 238, 238, 248)
    val colorTextDone  = if (isDark) Color.argb(120, 240, 242, 255)
                         else        Color.argb(130, 13, 15, 20)
    val colorDivider   = if (isDark) Color.argb(30, 255, 255, 255)
                         else        Color.argb(40, 0, 0, 0)
    val colorCheck     = if (isDark) "#00D9A3".toColorInt() else "#00A37A".toColorInt()

    // ── Paints ────────────────────────────────────────────────────────────────
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = colorPrimary
        textSize = titleSize
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = colorMuted
        textSize = subtitleSize
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        letterSpacing = 0.08f
    }

    val itemPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = colorOnBg
        textSize = itemSize
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    val donePaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.STRIKE_THRU_TEXT_FLAG).apply {
        color    = colorTextDone
        textSize = itemSize
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style     = Paint.Style.FILL
        color     = colorCard
    }

    val cardDonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colorCardDone
    }

    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = colorDivider
        strokeWidth = 1.5f * density
    }

    val bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorMuted
        textSize = itemSize
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorCheck
        textSize = itemSize
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    // ── Header ────────────────────────────────────────────────────────────────
    var y = topPad
    canvas.drawText("TASKARMA", hPad, y, subtitlePaint)
    y += titleSize * 1.1f
    canvas.drawText("My Tasks", hPad, y, titlePaint)
    y += lineSpacing * 0.4f
    canvas.drawLine(hPad, y, width - hPad, y, dividerPaint)
    y += lineSpacing * 1.1f

    // ── Items ────────────────────────────────────────────────────────────────
    val cardWidth      = width - 2 * hPad
    val textStartX     = hPad + cardHPad + bulletSpacing
    val textAvailWidth = (cardWidth - cardHPad * 2 - bulletSpacing - cardHPad).toInt()
    val maxRenderY     = height - 120f * density

    if (todos.isEmpty()) {
        canvas.drawText("No pending tasks ✓", hPad, y, donePaint)
    } else {
        for (i in todos.indices) {
            val todo  = todos[i]
            val isDone = todo.isDone
            val paint  = if (isDone) donePaint else itemPaint
            val bullet = if (isDone) "✓" else "·"
            val bulletPt = if (isDone) checkPaint else bulletPaint

            val layout = StaticLayout.Builder
                .obtain(todo.text, 0, todo.text.length, paint, textAvailWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(2)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setLineSpacing(0f, 1.15f)
                .setIncludePad(false)
                .build()

            val lineCount  = layout.lineCount
            val textHeight = layout.height.toFloat()
            val cardHeight = textHeight + cardVPad * 2

            val remaining = todos.size - i
            val overflowH  = if (remaining > 1) lineSpacing * 1.1f else 0f
            if (y + cardHeight + overflowH > maxRenderY) {
                canvas.drawText("+ $remaining more tasks", hPad, y + cardVPad, donePaint)
                break
            }

            // Draw card background
            val cardRect = RectF(hPad, y, hPad + cardWidth, y + cardHeight)
            canvas.drawRoundRect(cardRect, cardCorner, cardCorner, if (isDone) cardDonePaint else cardPaint)

            // Accent left bar for pending tasks
            if (!isDone) {
                val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colorPrimary
                }
                val accentRect = RectF(hPad, y, hPad + 4f * density, y + cardHeight)
                canvas.drawRoundRect(accentRect, cardCorner, cardCorner, accentPaint)
            }

            // Bullet / check
            val bulletY = y + cardVPad + layout.getLineBaseline(0)
            canvas.drawText(bullet, hPad + cardHPad, bulletY, bulletPt)

            // Text
            canvas.save()
            canvas.translate(textStartX, y + cardVPad)
            layout.draw(canvas)
            canvas.restore()

            y += cardHeight + 10f * density
        }
    }

    return bitmap
}
