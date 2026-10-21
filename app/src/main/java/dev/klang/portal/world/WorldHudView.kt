package dev.klang.portal.world

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/** Painted perspective HUD planes for the first milestone; coordinates use a virtual 1000x1600 surface. */
class WorldHudView(context: Context) : View(context) {
    var onPlayPause: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var onPrevious: (() -> Unit)? = null
    var onVolumeChanged: ((Float) -> Unit)? = null

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var trackTitle = "The portal is quiet"
    private var artist = "Allow access to scan local music"
    private var librarySummary = "YOUR LOCAL LIBRARY"
    private var status = "A little world for your music"
    private var playing = false
    private var volume = 0.72f
    private var cover: Bitmap? = null
    private var draggingKnob = false
    private var lastHapticStep = -1
    private var hudScale = 1f
    private var hudOffsetX = 0f
    private var hudOffsetY = 0f

    init {
        isFocusable = true
        contentDescription = "Fantasy music player. Tap the central record to play or pause. Drag the brass potion dial to change volume."
    }
    fun setTrack(title: String, artistName: String) { trackTitle = title; artist = artistName; invalidate() }
    fun setPlaying(isPlaying: Boolean) { playing = isPlaying; invalidate() }
    fun setStatus(message: String) { status = message; invalidate() }
    fun setLibrarySummary(count: Int) {
        librarySummary = if (count == 1) "1 SONG FOUND" else count.toString() + " SONGS FOUND"
        invalidate()
    }
    fun setAlbumArt(bitmap: Bitmap?) {
        val old = cover
        cover = bitmap
        if (old != null && old !== bitmap && !old.isRecycled) old.recycle()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        hudScale = min(w / 1000f, h / 1600f).coerceAtLeast(0.01f)
        hudOffsetX = (w - 1000f * hudScale) * 0.5f
        hudOffsetY = (h - 1600f * hudScale) * 0.5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val save = canvas.save()
        canvas.translate(hudOffsetX, hudOffsetY)
        canvas.scale(hudScale, hudScale)
        drawFloatingWorldPanel(canvas)
        drawRecord(canvas)
        drawControls(canvas)
        drawPotionDial(canvas)
        drawFooter(canvas)
        canvas.restoreToCount(save)
    }

    private fun drawFloatingWorldPanel(canvas: Canvas) {
        val panel = Path().apply {
            moveTo(65f, 390f); lineTo(900f, 390f); lineTo(945f, 955f); lineTo(38f, 955f); close()
        }
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(60f, 390f, 900f, 950f,
            intArrayOf(Color.argb(230, 49, 43, 34), Color.argb(225, 27, 39, 33), Color.argb(232, 61, 47, 33)),
            null, Shader.TileMode.CLAMP)
        canvas.drawPath(panel, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 5f
        p.color = Color.argb(230, 184, 137, 81)
        canvas.drawPath(panel, p)
        p.strokeWidth = 1.5f
        p.color = Color.argb(125, 243, 210, 154)
        canvas.drawLine(88f, 425f, 877f, 425f, p)
        p.style = Paint.Style.FILL
        drawText(canvas, "KLANG  /  PORTAL MUSIC", 88f, 115f, 30f, Color.rgb(245, 221, 171), true)
        drawText(canvas, "A little world for your music", 88f, 163f, 23f, Color.rgb(225, 226, 201), false)
        drawText(canvas, status.take(52), 88f, 218f, 19f, Color.rgb(190, 203, 174), false)
        drawText(canvas, "NOW PLAYING", 405f, 495f, 20f, Color.rgb(211, 169, 105), true)
        drawText(canvas, shorten(trackTitle, 24), 405f, 560f, 34f, Color.rgb(249, 235, 205), true)
        drawText(canvas, shorten(artist, 26), 405f, 610f, 23f, Color.rgb(202, 205, 177), false)
        drawText(canvas, librarySummary, 405f, 682f, 17f, Color.rgb(211, 169, 105), true)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        p.color = Color.argb(170, 208, 166, 99)
        canvas.drawRoundRect(RectF(395f, 720f, 855f, 790f), 15f, 15f, p)
        p.style = Paint.Style.FILL
        drawText(canvas, "THE LOCAL ARCHIVE  ·  TAP RECORD TO PLAY", 415f, 762f, 15f, Color.rgb(231, 211, 168), true)
    }

    private fun drawRecord(canvas: Canvas) {
        val cx = 245f; val cy = 645f; val radius = 146f
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(cx - radius, cy - radius, cx + radius, cy + radius,
            intArrayOf(Color.rgb(105, 76, 48), Color.rgb(36, 43, 34), Color.rgb(20, 25, 22)), null, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radius, p)
        p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = Color.rgb(202, 155, 92)
        canvas.drawCircle(cx, cy, radius, p)
        p.strokeWidth = 1.6f
        for (i in 1..7) {
            p.color = Color.argb(75 + i * 8, 215, 183, 127)
            canvas.drawCircle(cx, cy, radius - i * 12f, p)
        }
        p.style = Paint.Style.FILL
        val art = cover
        if (art != null && !art.isRecycled) {
            val clip = Path().apply { addCircle(cx, cy, 83f, Path.Direction.CW) }
            val save = canvas.save()
            canvas.clipPath(clip)
            canvas.drawBitmap(art, null, RectF(cx - 83f, cy - 83f, cx + 83f, cy + 83f), p)
            canvas.restoreToCount(save)
        } else {
            p.shader = LinearGradient(cx, cy - 80f, cx, cy + 80f,
                intArrayOf(Color.rgb(139, 159, 106), Color.rgb(44, 83, 69), Color.rgb(26, 42, 41)), null, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, 83f, p)
            p.shader = null
            p.color = Color.argb(140, 236, 215, 165)
            canvas.drawCircle(cx - 20f, cy - 17f, 13f, p)
            canvas.drawCircle(cx + 29f, cy + 21f, 8f, p)
        }
        p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = Color.rgb(215, 178, 115)
        canvas.drawCircle(cx, cy, 85f, p)
        p.style = Paint.Style.FILL; p.color = Color.rgb(231, 196, 126)
        canvas.drawCircle(cx, cy, 9f, p)
        p.color = Color.rgb(61, 49, 34); canvas.drawCircle(cx, cy, 3.5f, p)
    }

    private fun drawControls(canvas: Canvas) {
        val centers = floatArrayOf(325f, 500f, 675f)
        val cy = 1242f
        for (center in centers) {
            p.style = Paint.Style.FILL
            p.shader = LinearGradient(center - 50f, cy - 50f, center + 50f, cy + 50f,
                intArrayOf(Color.rgb(184, 137, 79), Color.rgb(82, 67, 47), Color.rgb(45, 48, 37)), null, Shader.TileMode.CLAMP)
            canvas.drawCircle(center, cy, if (center == 500f) 61f else 47f, p)
            p.shader = null; p.style = Paint.Style.STROKE
            p.strokeWidth = if (center == 500f) 4f else 2.5f
            p.color = Color.rgb(222, 179, 108)
            canvas.drawCircle(center, cy, if (center == 500f) 61f else 47f, p)
            p.style = Paint.Style.FILL
        }
        p.color = Color.rgb(250, 231, 188)
        if (playing) {
            canvas.drawRoundRect(RectF(482f, 1217f, 495f, 1267f), 4f, 4f, p)
            canvas.drawRoundRect(RectF(505f, 1217f, 518f, 1267f), 4f, 4f, p)
        } else {
            canvas.drawPath(Path().apply { moveTo(486f, 1209f); lineTo(486f, 1275f); lineTo(529f, 1242f); close() }, p)
        }
        canvas.drawPath(Path().apply {
            moveTo(310f, 1220f); lineTo(310f, 1264f); lineTo(284f, 1242f); close()
            moveTo(342f, 1220f); lineTo(342f, 1264f); lineTo(316f, 1242f); close()
        }, p)
        canvas.drawPath(Path().apply {
            moveTo(660f, 1220f); lineTo(660f, 1264f); lineTo(686f, 1242f); close()
            moveTo(692f, 1220f); lineTo(692f, 1264f); lineTo(718f, 1242f); close()
        }, p)
        drawText(canvas, "PREVIOUS", 277f, 1312f, 15f, Color.rgb(220, 206, 171), true)
        drawText(canvas, if (playing) "PAUSE" else "PLAY", 461f, 1320f, 16f, Color.rgb(245, 220, 168), true)
        drawText(canvas, "NEXT", 651f, 1312f, 15f, Color.rgb(220, 206, 171), true)
    }

    private fun drawPotionDial(canvas: Canvas) {
        val cx = 820f; val cy = 1435f; val r = 87f
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(cx - r, cy - r, cx + r, cy + r,
            intArrayOf(Color.rgb(189, 145, 86), Color.rgb(83, 67, 47), Color.rgb(36, 43, 34)), null, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r, p)
        p.shader = null; p.style = Paint.Style.STROKE; p.strokeWidth = 5f; p.color = Color.rgb(220, 178, 108)
        canvas.drawCircle(cx, cy, r, p)
        val arc = RectF(cx - 72f, cy - 72f, cx + 72f, cy + 72f)
        p.strokeWidth = 7f; p.strokeCap = Paint.Cap.ROUND
        p.color = Color.argb(75, 244, 214, 156); canvas.drawArc(arc, 130f, 280f, false, p)
        p.color = Color.rgb(237, 193, 109); canvas.drawArc(arc, 130f, 280f * volume, false, p)
        p.strokeCap = Paint.Cap.BUTT; p.style = Paint.Style.FILL
        val liquidTop = cy + 23f - 47f * volume
        val liquid = Path().apply {
            moveTo(cx - 26f, cy - 25f); lineTo(cx - 22f, cy + 25f)
            quadTo(cx, cy + 33f, cx + 22f, cy + 25f); lineTo(cx + 26f, cy - 25f); close()
        }
        val save = canvas.save()
        canvas.clipPath(liquid)
        p.shader = LinearGradient(cx, liquidTop - 12f, cx, cy + 30f,
            intArrayOf(Color.rgb(177, 241, 172), Color.rgb(62, 192, 140), Color.rgb(24, 105, 94)), null, Shader.TileMode.CLAMP)
        canvas.drawRect(cx - 31f, liquidTop, cx + 31f, cy + 35f, p)
        p.shader = null; canvas.restoreToCount(save)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = Color.argb(210, 235, 225, 186)
        canvas.drawPath(liquid, p)
        p.style = Paint.Style.FILL
        drawText(canvas, (volume * 100).toInt().toString() + "%", cx - 22f, cy + 59f, 19f, Color.rgb(255, 229, 175), true)
        drawText(canvas, "POTION", cx - 31f, cy + 119f, 16f, Color.rgb(220, 206, 171), true)
    }

    private fun drawFooter(canvas: Canvas) {
        drawText(canvas, "LOCAL AUDIO  ·  OFFLINE FIRST", 68f, 1537f, 17f, Color.rgb(218, 210, 174), true)
        drawText(canvas, "Tilt gently to look around", 68f, 1570f, 16f, Color.rgb(171, 195, 166), false)
    }

    private fun drawText(canvas: Canvas, text: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean) {
        p.shader = null; p.style = Paint.Style.FILL; p.color = color; p.textSize = size
        p.typeface = if (bold) android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
        else android.graphics.Typeface.create("serif", android.graphics.Typeface.NORMAL)
        canvas.drawText(text, x, y, p)
    }
    private fun shorten(value: String, maxLength: Int): String =
        if (value.length <= maxLength) value else value.take(maxLength - 1) + "…"

    private fun updateVolumeFromTouch(x: Float, y: Float) {
        val dx = x - 820f; val dy = y - 1435f
        val angle = Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())).toFloat()
        val next = ((angle + 140f) / 280f).coerceIn(0f, 1f)
        val step = (next * 20f).toInt()
        if (step != lastHapticStep) {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            lastHapticStep = step
        }
        volume = next; onVolumeChanged?.invoke(volume); invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = (event.x - hudOffsetX) / hudScale
        val y = (event.y - hudOffsetY) / hudScale
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (hypot((x - 820f).toDouble(), (y - 1435f).toDouble()) <= 112.0) {
                    draggingKnob = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    updateVolumeFromTouch(x, y)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> { if (draggingKnob) updateVolumeFromTouch(x, y); return true }
            MotionEvent.ACTION_UP -> {
                if (draggingKnob) { draggingKnob = false; performClick(); return true }
                when {
                    hypot((x - 500f).toDouble(), (y - 1242f).toDouble()) < 80.0 -> onPlayPause?.invoke()
                    hypot((x - 325f).toDouble(), (y - 1242f).toDouble()) < 60.0 -> onPrevious?.invoke()
                    hypot((x - 675f).toDouble(), (y - 1242f).toDouble()) < 60.0 -> onNext?.invoke()
                    x in 75f..375f && y in 480f..820f -> onPlayPause?.invoke()
                    x in 390f..870f && y in 700f..810f -> onNext?.invoke()
                }
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> { draggingKnob = false; return true }
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}
