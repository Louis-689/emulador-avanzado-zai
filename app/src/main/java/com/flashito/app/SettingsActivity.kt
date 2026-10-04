package com.flashito.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Pantalla de configuración: endpoint, API key y modelo (todo queda en el celu). */
class SettingsActivity : Activity() {

    private lateinit var endpoint: EditText
    private lateinit var apiKey: EditText
    private lateinit var model: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("flashito", MODE_PRIVATE)

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }

        box.addView(TextView(this).apply {
            text = "⚡ Flashito"
            setTextSize(2, 26f)
            setTypeface(typeface, Typeface.BOLD)
        })

        fun label(text: String): TextView = TextView(this).apply {
            this.text = text
            setTextSize(2, 14f)
            setPadding(0, dp(16), 0, dp(4))
        }

        fun field(value: String, hidden: Boolean = false): EditText = EditText(this).apply {
            setText(value)
            setSingleLine(true)
            if (hidden) transformationMethod = PasswordTransformationMethod.getInstance()
        }

        endpoint = field(
            prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions") ?: ""
        )
        apiKey = field(prefs.getString("apiKey", "") ?: "", hidden = true)
        model = field(prefs.getString("model", "gpt-4o-mini") ?: "")

        box.addView(label("Endpoint (compatible con OpenAI):"))
        box.addView(endpoint)
        box.addView(label("API key:"))
        box.addView(apiKey)
        box.addView(label("Modelo:"))
        box.addView(model)

        val status = TextView(this).apply {
            setTextSize(2, 13f)
            setPadding(0, dp(10), 0, 0)
        }
        box.addView(status)

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }

        val probar = Button(this).apply { text = "Probar conexión" }
        probar.setOnClickListener {
            status.setTextColor(Color.GRAY)
            status.text = "Probando…"
            Thread {
                try {
                    val answer = Ai.ask(
                        endpoint.text.toString().trim(),
                        apiKey.text.toString().trim(),
                        model.text.toString().trim(),
                        "Test de conexión. Respondé exactamente: OK"
                    )
                    runOnUiThread {
                        status.setTextColor(-0xff5501) // verde
                        status.text = "OK — el modelo respondió: $answer"
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        status.setTextColor(Color.parseColor("#EF4444"))
                        status.text = "Error: ${e.message ?: "desconocido"}"
                    }
                }
            }.start()
        }

        val guardar = Button(this).apply { text = "Guardar" }
        guardar.setOnClickListener {
            prefs.edit()
                .putString("endpoint", endpoint.text.toString().trim())
                .putString("apiKey", apiKey.text.toString().trim())
                .putString("model", model.text.toString().trim())
                .apply()
            Toast.makeText(this, "Guardado ✓", Toast.LENGTH_SHORT).show()
        }

        buttons.addView(probar, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        buttons.addView(guardar, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(buttons)

        box.addView(TextView(this).apply {
            text = "Cómo usarlo: subrayá la pregunta (con sus opciones, si tiene) en " +
                "cualquier app → tocá “Flashito” en el menú → la respuesta aparece " +
                "abajo a la izquierda, chica y discreta."
            setTextSize(2, 13f)
            setPadding(0, dp(22), 0, 0)
        })

        setContentView(ScrollView(this).apply { addView(box) })
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
