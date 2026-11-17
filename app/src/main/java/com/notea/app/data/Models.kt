package com.notea.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Tipos de pregunta disponibles en un cuestionario. */
enum class QType(val label: String, val hasOptions: Boolean = false) {
    TEXT("Texto corto"),
    PARAGRAPH("Párrafo"),
    NUMBER("Número"),
    SINGLE("Opción única", true),
    MULTI("Casillas (varias opciones)", true),
    DROPDOWN("Desplegable", true),
    SCALE("Escala lineal"),
    RATING("Valoración con estrellas"),
    YESNO("Sí / No"),
    TIME("Hora"),
    DATE("Fecha"),
}

data class Question(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val type: QType = QType.TEXT,
    val required: Boolean = false,
    val options: List<String> = emptyList(),
    val min: Int = 1,
    val max: Int = 5,
    val unit: String = "",
)

/**
 * Respuesta a una pregunta. [text] guarda texto, número, escala, sí/no, hora o fecha;
 * [choices] las opciones marcadas; [other] el texto libre de la opción «Otro».
 */
data class Answer(
    val text: String = "",
    val choices: List<String> = emptyList(),
    val otherChecked: Boolean = false,
    val other: String = "",
) {
    val isEmpty: Boolean
        get() = text.isBlank() && choices.isEmpty() && !(otherChecked && other.isNotBlank())

    /** Opciones elegidas incluyendo el texto de «Otro» si procede. */
    val allChoices: List<String>
        get() = if (otherChecked && other.isNotBlank()) choices + other.trim() else choices

    fun display(q: Question): String = when (q.type) {
        QType.SINGLE, QType.MULTI, QType.DROPDOWN -> allChoices.joinToString(", ")
        QType.YESNO -> when (text) {
            "yes" -> "Sí"
            "no" -> "No"
            else -> ""
        }
        QType.RATING -> text.toIntOrNull()?.let { "★".repeat(it) + "☆".repeat((5 - it).coerceAtLeast(0)) } ?: ""
        QType.NUMBER -> if (text.isBlank()) "" else if (q.unit.isBlank()) text else "$text ${q.unit}"
        QType.SCALE -> if (text.isBlank()) "" else "$text / ${q.max}"
        else -> text
    }
}

fun questionsToJson(list: List<Question>): String {
    val arr = JSONArray()
    for (q in list) {
        val o = JSONObject()
        o.put("id", q.id)
        o.put("title", q.title)
        o.put("type", q.type.name)
        o.put("required", q.required)
        val opts = JSONArray()
        q.options.forEach { opts.put(it) }
        o.put("options", opts)
        o.put("min", q.min)
        o.put("max", q.max)
        o.put("unit", q.unit)
        arr.put(o)
    }
    return arr.toString()
}

fun questionsFromJson(s: String): List<Question> = try {
    val arr = JSONArray(s)
    val out = ArrayList<Question>()
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val opts = o.optJSONArray("options")
        val options = ArrayList<String>()
        if (opts != null) for (j in 0 until opts.length()) options.add(opts.getString(j))
        val type = try {
            QType.valueOf(o.optString("type", "TEXT"))
        } catch (e: Exception) {
            QType.TEXT
        }
        out.add(
            Question(
                id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                title = o.optString("title"),
                type = type,
                required = o.optBoolean("required", false),
                options = options,
                min = o.optInt("min", 1),
                max = o.optInt("max", 5),
                unit = o.optString("unit"),
            )
        )
    }
    out
} catch (e: Exception) {
    emptyList()
}

fun answersToJson(map: Map<String, Answer>): String {
    val root = JSONObject()
    for ((id, a) in map) {
        if (a.isEmpty) continue
        val o = JSONObject()
        o.put("text", a.text)
        val ch = JSONArray()
        a.choices.forEach { ch.put(it) }
        o.put("choices", ch)
        o.put("otherChecked", a.otherChecked)
        o.put("other", a.other)
        root.put(id, o)
    }
    return root.toString()
}

fun answersFromJson(s: String): Map<String, Answer> = try {
    val root = JSONObject(s)
    val out = LinkedHashMap<String, Answer>()
    val keys = root.keys()
    while (keys.hasNext()) {
        val k = keys.next()
        val o = root.getJSONObject(k)
        val ch = o.optJSONArray("choices")
        val choices = ArrayList<String>()
        if (ch != null) for (j in 0 until ch.length()) choices.add(ch.getString(j))
        out[k] = Answer(
            text = o.optString("text"),
            choices = choices,
            otherChecked = o.optBoolean("otherChecked", false),
            other = o.optString("other"),
        )
    }
    out
} catch (e: Exception) {
    emptyMap()
}
