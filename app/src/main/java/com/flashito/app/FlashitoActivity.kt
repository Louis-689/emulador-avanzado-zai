package com.flashito.app

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Recibe el texto subrayado (ACTION_PROCESS_TEXT) y muestra la respuesta en
 * una tarjeta chica abajo a la izquierda, sin activar pantalla completa:
 * la actividad es translúcida y la tarjeta se ancla abajo a la izquierda.
 */
class FlashitoActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("flashito", MODE_PRIVATE)
        val endpoint = prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions") ?: ""
        val apiKey = prefs.getString("apiKey", "") ?: ""
        val model = prefs.getString("model", "gpt-4o-mini") ?: ""
        val selected = (intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: "")
            .take(1200)

        if (apiKey.isBlank()) {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
            return
        }
        if (selected.isBlank()) {
            finish()
            return
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(11).toFloat()
                setColor(0xF110141A.toInt())
            }
            setPadding(dp(10), dp(6), dp(10), dp(6))
        }

        val answer = TextView(this).apply {
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(0xFFF9FAFB.toInt())
            setTextSize(2, 12f)
            maxWidth = dp(360)
            text = "⏳ Pensando…"
        }
        card.addView(answer)

        val wrap = FrameLayout(this).apply { setPadding(dp(8), 0, dp(8), dp(8)) }
        wrap.addView(
            card,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.START
            )
        )
        setContentView(wrap)

        card.setOnClickListener { finish() }
        handler.postDelayed({ finish() }, 12000)

        Thread {
            try {
                val question = Ai.parse(selected)
                val result = Ai.ask(endpoint, apiKey, model, Ai.userContent(question))
                handler.post {
                    handler.removeCallbacksAndMessages(null)
                    answer.text = "✓ $result"
                    handler.postDelayed({ finish() }, 7000)
                }
            } catch (e: Exception) {
                handler.post {
                    handler.removeCallbacksAndMessages(null)
                    answer.text = "✕ ${e.message ?: "Error desconocido"}"
                    handler.postDelayed({ finish() }, 9000)
                }
            }
        }.start()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
