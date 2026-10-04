package com.flashito.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

/**
 * Cliente de la API de IA (formato compatible con OpenAI) y parser de
 * opciones "A) .. B) ..". Solo HTTP + JSON: sin base de datos ni comandos.
 */
object Ai {

    private const val SYSTEM_PROMPT: String =
        "Sos un resolutor de quizzes. El usuario te manda un JSON con \"pregunta\" y \"opciones\". " +
            "Respondé ÚNICAMENTE con la respuesta correcta en UNA sola línea muy corta, sin explicaciones ni markdown. " +
            "Si hay opciones, empezá con la letra correcta y el texto de la opción (ej: \"D) 4\"). " +
            "Si no hay opciones, respondé brevemente. Respondé en el idioma de la pregunta."

    // Detecta marcadores tipo "A)", "A.", "A:", "(A)", "Opción A:", en una o varias líneas.
    private val MARKER: Pattern = Pattern.compile(
        "(?:\\bopci[oó]n(?:es)?|\\balternativas?|\\balt)?\\s*\\(?\\s*([a-j])\\s*[)\\].:\\-–—]\\s*",
        Pattern.CASE_INSENSITIVE
    )

    data class Question(val pregunta: String, val opciones: List<String>)

    private data class Mark(val letter: Char, val start: Int, val end: Int)

    /** Separa la pregunta de sus opciones dentro del texto subrayado. */
    fun parse(rawText: String): Question {
        val raw = rawText.trim()
        if (raw.isEmpty()) return Question("", emptyList())

        val marks = ArrayList<Mark>()
        val matcher = MARKER.matcher(raw)
        while (matcher.find()) {
            marks.add(Mark(matcher.group(1)!!.lowercase()[0], matcher.start(), matcher.end()))
        }

        // Se queda con la secuencia más larga de letras consecutivas empezando en "a".
        var best: List<Mark>? = null
        var cur: MutableList<Mark>? = null
        for (mk in marks) {
            val expected: Char = if (cur == null) 'a' else ('a' + cur.size)
            if (mk.letter == expected) {
                if (cur == null) cur = ArrayList()
                cur.add(mk)
            } else if (mk.letter == 'a') {
                if (cur != null && cur.size >= 2 && (best == null || cur.size > best.size)) best = cur
                cur = arrayListOf(mk)
            } else {
                if (cur != null && cur.size >= 2 && (best == null || cur.size > best.size)) best = cur
                cur = null
            }
        }
        if (cur != null && cur.size >= 2 && (best == null || cur.size > best.size)) best = cur

        val run = best ?: return Question(raw, emptyList())

        var question = raw.substring(0, run[0].start).trim().trimEnd(',', ';').trim()
        val options = ArrayList<String>()
        for (i in run.indices) {
            val from = run[i].end
            val to = if (i + 1 < run.size) run[i + 1].start else raw.length
            val opt = raw.substring(from, to).trim().trimEnd(',', ';').trim()
            if (opt.isNotEmpty()) options.add(opt)
        }
        if (options.size < 2) return Question(raw, emptyList())
        if (question.isEmpty()) question = raw.substring(0, run[0].start).trim()
        return Question(question, options)
    }

    /** Mensaje para el modelo: JSON con pregunta y opciones. */
    fun userContent(q: Question): String {
        val obj = JSONObject()
        obj.put("pregunta", q.pregunta)
        obj.put("opciones", JSONArray(q.opciones))
        return obj.toString()
    }

    /** Llama al endpoint y devuelve la primera línea de la respuesta. */
    fun ask(endpoint: String, apiKey: String, model: String, userContent: String): String {
        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
        messages.put(JSONObject().put("role", "user").put("content", userContent))
        val payload = JSONObject()
            .put("model", model)
            .put("temperature", 0)
            .put("messages", messages)

        val conn = URL(endpoint).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 60000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", listOf("Bearer", apiKey).joinToString(" "))
            conn.outputStream.use { out ->
                out.write(payload.toString().toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw Exception("Error $code: ${body.take(140)}")
            val content = JSONObject(body)
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
            return firstLine(content)
        } finally {
            conn.disconnect()
        }
    }

    private fun firstLine(text: String): String {
        for (line in text.trim().split('\n')) {
            val l = line.trim().trim('"', '\'', '`').trim()
            if (l.isNotEmpty()) return l.take(200)
        }
        return "Sin respuesta"
    }
}
