package com.gtportable

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.hypot

object Psp {
    const val SELECT = 0x1; const val START = 0x8
    const val UP = 0x10; const val RIGHT = 0x20; const val DOWN = 0x40; const val LEFT = 0x80
    const val L = 0x100; const val R = 0x200
    const val TRIANGLE = 0x1000; const val CIRCLE = 0x2000; const val CROSS = 0x4000; const val SQUARE = 0x8000
}

enum class Kind { BUTTON, SLIDER }

/** x,y = centro (0..1 da tela); size = fração da altura da tela. */
class PadButton(val id: String, val label: String, val mask: Int, val kind: Kind,
                var x: Float, var y: Float, var size: Float)

class ControlsView(ctx: Context) : View(ctx) {
    var onState: ((Int, Int, Int) -> Unit)? = null
    var editMode = false
        set(v) { field = v; if (!v) selected = null; invalidate() }

    private val prefs = ctx.getSharedPreferences("gt", Context.MODE_PRIVATE)
    private var buttons = defaultLayout()
    private var selected: PadButton? = null
    private var pressed = HashSet<String>()
    private var lastMask = -1; private var lastAx = -1
    private var sliderPos = 0f

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFFFFF }
    private val fillOn = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xAAFFFFFF.toInt() }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f; color = 0xCCFFFFFF.toInt() }
    private val sel = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 8f; color = 0xFFFFC107.toInt() }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER }

    init { load() }

    private fun defaultLayout() = mutableListOf(
        PadButton("steer", "VOLANTE", 0, Kind.SLIDER, .23f, .84f, .19f),
        PadButton("gas", "ACEL", Psp.CROSS, Kind.BUTTON, .90f, .70f, .20f),
        PadButton("brake", "FREIO", Psp.SQUARE, Kind.BUTTON, .75f, .82f, .16f),
        PadButton("circle", "○", Psp.CIRCLE, Kind.BUTTON, .90f, .42f, .12f),
        PadButton("camera", "CAM", Psp.TRIANGLE, Kind.BUTTON, .76f, .56f, .12f),
        PadButton("shiftdn", "L", Psp.L, Kind.BUTTON, .06f, .14f, .12f),
        PadButton("shiftup", "R", Psp.R, Kind.BUTTON, .94f, .14f, .12f),
        PadButton("up", "▲", Psp.UP, Kind.BUTTON, .09f, .34f, .09f),
        PadButton("left", "◀", Psp.LEFT, Kind.BUTTON, .05f, .47f, .09f),
        PadButton("right", "▶", Psp.RIGHT, Kind.BUTTON, .13f, .47f, .09f),
        PadButton("down", "▼", Psp.DOWN, Kind.BUTTON, .09f, .60f, .09f),
        PadButton("select", "SEL", Psp.SELECT, Kind.BUTTON, .44f, .08f, .08f),
        PadButton("start", "START", Psp.START, Kind.BUTTON, .56f, .08f, .08f),
    )

    fun resetLayout() { buttons = defaultLayout(); selected = null; invalidate() }
    fun setSelectedSize(s: Float) { selected?.size = s; invalidate() }

    fun save() {
        val a = JSONArray()
        buttons.forEach { a.put(JSONObject().put("id", it.id).put("x", it.x.toDouble()).put("y", it.y.toDouble()).put("s", it.size.toDouble())) }
        prefs.edit().putString("layout", a.toString()).apply()
    }

    private fun load() {
        val j = prefs.getString("layout", null) ?: return
        try {
            val a = JSONArray(j)
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                buttons.find { it.id == o.getString("id") }?.apply {
                    x = o.getDouble("x").toFloat(); y = o.getDouble("y").toFloat(); size = o.getDouble("s").toFloat()
                }
            }
        } catch (_: Exception) {}
    }

    private fun rect(b: PadButton): RectF {
        val s = b.size * height
        val w = if (b.kind == Kind.SLIDER) s * 4.5f else s
        val cx = b.x * width; val cy = b.y * height
        return RectF(cx - w / 2, cy - s / 2, cx + w / 2, cy + s / 2)
    }

    private fun hit(b: PadButton, px: Float, py: Float): Boolean {
        val r = rect(b)
        return if (b.kind == Kind.SLIDER) r.contains(px, py)
        else hypot(px - r.centerX(), py - r.centerY()) <= r.width() / 2 * 1.15f
    }

    override fun onDraw(c: Canvas) {
        for (b in buttons) {
            val r = rect(b); val on = b.id in pressed
            text.textSize = r.height() * 0.28f
            if (b.kind == Kind.SLIDER) {
                c.drawRoundRect(r, r.height() / 2, r.height() / 2, fill)
                c.drawRoundRect(r, r.height() / 2, r.height() / 2, stroke)
                val kx = r.centerX() + sliderPos * (r.width() / 2 - r.height() / 2)
                c.drawCircle(kx, r.centerY(), r.height() * 0.42f, if (on) fillOn else fill)
                c.drawText(b.label, r.centerX(), r.centerY() + text.textSize / 3, text)
            } else {
                c.drawCircle(r.centerX(), r.centerY(), r.width() / 2, if (on) fillOn else fill)
                c.drawCircle(r.centerX(), r.centerY(), r.width() / 2, stroke)
                c.drawText(b.label, r.centerX(), r.centerY() + text.textSize / 3, text)
            }
            if (editMode && b === selected) c.drawRect(r, sel)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (editMode) { editTouch(e); return true }
        var mask = 0; var ax = 128
        val now = HashSet<String>(); sliderPos = 0f
        val up = e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL
        for (i in 0 until e.pointerCount) {
            if (up || (e.actionMasked == MotionEvent.ACTION_POINTER_UP && i == e.actionIndex)) continue
            val px = e.getX(i); val py = e.getY(i)
            for (b in buttons) if (hit(b, px, py)) {
                now.add(b.id)
                if (b.kind == Kind.SLIDER) {
                    val r = rect(b); val half = r.width() / 2 - r.height() / 2
                    sliderPos = ((px - r.centerX()) / half).coerceIn(-1f, 1f)
                    ax = (128 + sliderPos * 127).toInt()
                } else mask = mask or b.mask
            }
        }
        pressed = now; invalidate()
        if (mask != lastMask || ax != lastAx) { lastMask = mask; lastAx = ax; onState?.invoke(mask, ax, 128) }
        return true
    }

    private fun editTouch(e: MotionEvent) {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> selected = buttons.lastOrNull { hit(it, e.x, e.y) } ?: selected
            MotionEvent.ACTION_MOVE -> selected?.let { it.x = (e.x / width).coerceIn(.02f, .98f); it.y = (e.y / height).coerceIn(.04f, .96f) }
        }
        invalidate()
    }
}
