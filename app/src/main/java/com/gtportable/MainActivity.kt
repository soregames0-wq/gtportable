package com.gtportable

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView
    private lateinit var resume: Button
    private val prefs by lazy { getSharedPreferences("gt", MODE_PRIVATE) }

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        if (!looksLikePspImage(uri)) { status.text = "Arquivo inválido: use ISO ou CSO do jogo."; return@registerForActivityResult }
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        prefs.edit().putString("rom", uri.toString()).apply()
        launch(uri)
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(48, 48, 48, 48) }
        status = TextView(this).apply { text = "Escolha a ROM (ISO/CSO) do seu jogo."; textSize = 16f; gravity = Gravity.CENTER }
        val pick = Button(this).apply { text = "Selecionar ROM"; setOnClickListener { picker.launch(arrayOf("*/*")) } }
        resume = Button(this).apply { text = "Continuar com a última ROM"; setOnClickListener { prefs.getString("rom", null)?.let { launch(Uri.parse(it)) } } }
        root.addView(status); root.addView(pick); root.addView(resume)
        setContentView(root)
    }

    override fun onResume() { super.onResume(); resume.isEnabled = prefs.getString("rom", null) != null }

    private fun launch(uri: Uri) = startActivity(Intent(this, GameActivity::class.java).setData(uri))

    /** Confere o cabeçalho: ISO9660 ("CD001" em 0x8001) ou CSO ("CISO"). */
    private fun looksLikePspImage(uri: Uri): Boolean = try {
        contentResolver.openInputStream(uri)!!.use { s ->
            val head = ByteArray(4); s.read(head)
            if (String(head) == "CISO") true else {
                s.skip(0x8001L - 4); val m = ByteArray(5); s.read(m); String(m) == "CD001"
            }
        }
    } catch (e: Exception) { false }
}
