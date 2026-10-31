package com.gtportable

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class GameActivity : ComponentActivity(), SurfaceHolder.Callback {
    private var pfd: ParcelFileDescriptor? = null
    private lateinit var controls: ControlsView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        pfd = contentResolver.openFileDescriptor(intent.data!!, "r")

        val root = FrameLayout(this)
        val sv = SurfaceView(this).also { it.holder.addCallback(this) }
        controls = ControlsView(this).also { it.onState = { btn, ax, ay -> Native.input(btn, ax, ay) } }

        val edit = TextView(this).apply {
            text = "✎ Editar controles"; setPadding(24, 12, 24, 12); alpha = .6f
            setOnClickListener { setEditing(true) }
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; visibility = View.GONE
            setBackgroundColor(0xCC000000.toInt()); setPadding(24, 8, 24, 8)
        }
        val size = SeekBar(this).apply {
            max = 30; progress = 10
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, p: Int, user: Boolean) { if (user) controls.setSelectedSize(0.05f + p * 0.01f) }
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) {}
            })
        }
        val reset = Button(this).apply { text = "Padrão"; setOnClickListener { controls.resetLayout() } }
        val done = Button(this).apply { text = "Salvar"; setOnClickListener { controls.save(); setEditing(false) } }
        bar.addView(TextView(this).apply { text = "Tamanho  " }); bar.addView(size); bar.addView(reset); bar.addView(done)

        root.addView(sv); root.addView(controls)
        root.addView(edit, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        root.addView(bar, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        setContentView(root)
        barRef = bar; editRef = edit
    }

    private var barRef: View? = null
    private var editRef: View? = null
    private fun setEditing(on: Boolean) {
        controls.editMode = on
        barRef?.visibility = if (on) View.VISIBLE else View.GONE
        editRef?.visibility = if (on) View.GONE else View.VISIBLE
    }

    override fun surfaceCreated(h: SurfaceHolder) { pfd?.let { Native.start(h.surface, it.fd) } }
    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, ht: Int) {}
    override fun surfaceDestroyed(h: SurfaceHolder) { Native.stop() }
    override fun onDestroy() { pfd?.close(); super.onDestroy() }
}
