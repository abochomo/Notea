package com.notea.app.md

/**
 * Núcleo de Markdown sin dependencias de Android: análisis por bloques y operaciones
 * de edición sobre texto + selección. Al ser Kotlin puro se puede probar en la JVM.
 */
sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data class Bullet(val indent: Int, val text: String) : MdBlock()
    data class Ordered(val indent: Int, val number: String, val text: String) : MdBlock()
    data class Task(val indent: Int, val checked: Boolean, val text: String, val line: Int) : MdBlock()
    data class Quote(val text: String) : MdBlock()
    data class Code(val text: String) : MdBlock()
    data class Table(val header: List<String>, val rows: List<List<String>>) : MdBlock()
    object Rule : MdBlock()
}

private val headingRe = Regex("^(#{1,6})\\s+(.*)$")
private val taskRe = Regex("^(\\s*)[-*+]\\s+\\[([ xX])\\]\\s?(.*)$")
private val bulletRe = Regex("^(\\s*)[-*+]\\s+(.*)$")
private val orderedRe = Regex("^(\\s*)(\\d+)[.)]\\s+(.*)$")
private val quoteRe = Regex("^>\\s?(.*)$")
private val ruleRe = Regex("^\\s*([-*_])(\\s*\\1){2,}\\s*$")
private val tableSepRe = Regex("^\\s*\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?\\s*$")

private fun tableCells(line: String): List<String> =
    line.trim().removePrefix("|").removeSuffix("|").split('|').map { it.trim() }

fun parseMarkdown(src: String): List<MdBlock> {
    val lines = src.split('\n')
    val out = ArrayList<MdBlock>()
    val para = ArrayList<String>()
    fun flush() {
        if (para.isNotEmpty()) {
            out.add(MdBlock.Paragraph(para.joinToString("\n")))
            para.clear()
        }
    }

    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            flush()
            val code = ArrayList<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                code.add(lines[i])
                i++
            }
            out.add(MdBlock.Code(code.joinToString("\n")))
            i++
            continue
        }
        if (trimmed.isEmpty()) {
            flush()
            i++
            continue
        }
        if (ruleRe.matches(line)) {
            flush()
            out.add(MdBlock.Rule)
            i++
            continue
        }
        val heading = headingRe.find(line)
        if (heading != null) {
            flush()
            out.add(MdBlock.Heading(heading.groupValues[1].length, heading.groupValues[2].trim()))
            i++
            continue
        }
        val task = taskRe.find(line)
        if (task != null) {
            flush()
            out.add(
                MdBlock.Task(
                    indent = task.groupValues[1].length / 2,
                    checked = task.groupValues[2] != " ",
                    text = task.groupValues[3],
                    line = i,
                )
            )
            i++
            continue
        }
        val bullet = bulletRe.find(line)
        if (bullet != null) {
            flush()
            out.add(MdBlock.Bullet(bullet.groupValues[1].length / 2, bullet.groupValues[2]))
            i++
            continue
        }
        val ordered = orderedRe.find(line)
        if (ordered != null) {
            flush()
            out.add(MdBlock.Ordered(ordered.groupValues[1].length / 2, ordered.groupValues[2], ordered.groupValues[3]))
            i++
            continue
        }
        if (quoteRe.matches(line)) {
            flush()
            val quote = ArrayList<String>()
            while (i < lines.size) {
                val m = quoteRe.find(lines[i]) ?: break
                quote.add(m.groupValues[1])
                i++
            }
            out.add(MdBlock.Quote(quote.joinToString("\n")))
            continue
        }
        if (trimmed.startsWith("|") && i + 1 < lines.size && tableSepRe.matches(lines[i + 1]) && lines[i + 1].contains('-')) {
            flush()
            val header = tableCells(line)
            val rows = ArrayList<List<String>>()
            i += 2
            while (i < lines.size && lines[i].trim().startsWith("|")) {
                rows.add(tableCells(lines[i]))
                i++
            }
            out.add(MdBlock.Table(header, rows))
            continue
        }
        para.add(line)
        i++
    }
    flush()
    return out
}

/** Marca o desmarca la casilla de la línea [line] (índice dentro del texto fuente). */
fun toggleTask(src: String, line: Int): String {
    val lines = src.split('\n').toMutableList()
    if (line < 0 || line >= lines.size) return src
    val l = lines[line]
    if (taskRe.find(l) == null) return src
    val idx = l.indexOf('[')
    if (idx < 0 || idx + 2 >= l.length) return src
    val mark = if (l[idx + 1] == ' ') 'x' else ' '
    lines[line] = l.substring(0, idx + 1) + mark + l.substring(idx + 2)
    return lines.joinToString("\n")
}

private val linkRe = Regex("!?\\[([^\\]]*)\\]\\([^)]*\\)")
private val emphasisRe = Regex("(\\*\\*\\*|\\*\\*|__|~~|`)")

/** Texto plano sin sintaxis Markdown. */
fun stripInline(s: String): String {
    var r = linkRe.replace(s) { it.groupValues[1] }
    r = emphasisRe.replace(r, "")
    r = Regex("(?<![\\w*])\\*(?=\\S)([^*]+)\\*(?![\\w*])").replace(r) { it.groupValues[1] }
    return r
}

/** Resumen de una nota en texto plano para tarjetas y notificaciones. */
fun plainSnippet(src: String, maxLines: Int): String {
    val out = ArrayList<String>()
    var inCode = false
    for (raw in src.split('\n')) {
        if (out.size >= maxLines) break
        val t = raw.trim()
        if (t.startsWith("```")) {
            inCode = !inCode
            continue
        }
        if (t.isEmpty()) continue
        if (inCode) {
            out.add(t)
            continue
        }
        if (ruleRe.matches(raw) || tableSepRe.matches(raw) && raw.contains('-') && raw.contains('|')) continue
        val heading = headingRe.find(raw)
        val task = taskRe.find(raw)
        val bullet = bulletRe.find(raw)
        val quote = quoteRe.find(raw)
        val line = when {
            heading != null -> stripInline(heading.groupValues[2])
            task != null -> (if (task.groupValues[2] == " ") "☐ " else "☑ ") + stripInline(task.groupValues[3])
            bullet != null -> "• " + stripInline(bullet.groupValues[2])
            quote != null -> stripInline(quote.groupValues[1])
            else -> stripInline(t)
        }
        if (line.isNotBlank()) out.add(line)
    }
    return out.joinToString("\n")
}

/** Cuenta de casillas (hechas, total) de una nota. */
fun taskProgress(src: String): Pair<Int, Int> {
    var done = 0
    var total = 0
    var inCode = false
    for (raw in src.split('\n')) {
        if (raw.trim().startsWith("```")) {
            inCode = !inCode
            continue
        }
        if (inCode) continue
        val m = taskRe.find(raw) ?: continue
        total++
        if (m.groupValues[2] != " ") done++
    }
    return Pair(done, total)
}

// ---------------------------------------------------------------------------
// Edición
// ---------------------------------------------------------------------------

/** Resultado de una operación de edición: texto nuevo y selección resultante. */
data class Edit(val text: String, val start: Int, val end: Int)

enum class LineKind { BULLET, ORDERED, TASK, QUOTE, H1, H2, H3 }

private val linePrefixRe = Regex("^(\\s*)(- \\[[ xX]\\] |[-*+] |\\d+[.)] |> ?|#{1,6} )?")

private fun kindOf(prefix: String): LineKind? = when {
    prefix.isEmpty() -> null
    prefix.startsWith("- [") -> LineKind.TASK
    prefix[0] == '-' || prefix[0] == '*' || prefix[0] == '+' -> LineKind.BULLET
    prefix[0].isDigit() -> LineKind.ORDERED
    prefix[0] == '>' -> LineKind.QUOTE
    prefix == "# " -> LineKind.H1
    prefix == "## " -> LineKind.H2
    prefix[0] == '#' -> LineKind.H3
    else -> null
}

private fun prefixFor(kind: LineKind, n: Int): String = when (kind) {
    LineKind.BULLET -> "- "
    LineKind.ORDERED -> "$n. "
    LineKind.TASK -> "- [ ] "
    LineKind.QUOTE -> "> "
    LineKind.H1 -> "# "
    LineKind.H2 -> "## "
    LineKind.H3 -> "### "
}

/** Rodea la selección con [before]/[after]; si ya lo está, lo quita. */
fun wrapSelection(text: String, selStart: Int, selEnd: Int, before: String, after: String = before): Edit {
    val s = minOf(selStart, selEnd).coerceIn(0, text.length)
    val e = maxOf(selStart, selEnd).coerceIn(0, text.length)
    if (s >= before.length && e + after.length <= text.length &&
        text.startsWith(before, s - before.length) && text.startsWith(after, e)
    ) {
        val nt = text.substring(0, s - before.length) + text.substring(s, e) + text.substring(e + after.length)
        return Edit(nt, s - before.length, e - before.length)
    }
    val nt = text.substring(0, s) + before + text.substring(s, e) + after + text.substring(e)
    return Edit(nt, s + before.length, e + before.length)
}

/** Aplica (o quita, si ya lo tienen todas) un prefijo de línea a las líneas seleccionadas. */
fun toggleLinePrefix(text: String, selStart: Int, selEnd: Int, kind: LineKind): Edit {
    val s = minOf(selStart, selEnd).coerceIn(0, text.length)
    val e = maxOf(selStart, selEnd).coerceIn(0, text.length)
    val ls = if (s == 0) 0 else text.lastIndexOf('\n', s - 1) + 1
    val leIdx = text.indexOf('\n', e)
    val le = if (leIdx < 0) text.length else leIdx
    val lines = text.substring(ls, le).split('\n')
    val prefixes = lines.map { linePrefixRe.find(it) }
    val allSame = lines.indices.all { i -> kindOf(prefixes[i]?.groupValues?.get(2) ?: "") == kind }
    var n = 0
    val newLines = lines.mapIndexed { i, line ->
        val m = prefixes[i]
        val indent = m?.groupValues?.get(1) ?: ""
        val rest = line.substring(m?.value?.length ?: 0)
        if (allSame) {
            indent + rest
        } else {
            n++
            indent + prefixFor(kind, n) + rest
        }
    }
    val block = newLines.joinToString("\n")
    val newText = text.substring(0, ls) + block + text.substring(le)
    val delta = newText.length - text.length
    val ne = (e + delta).coerceIn(ls, newText.length)
    return if (s == e) Edit(newText, ne, ne) else Edit(newText, ls, ls + block.length)
}

/** Alterna el encabezado de la línea: ninguno → H1 → H2 → H3 → ninguno. */
fun cycleHeading(text: String, selStart: Int, selEnd: Int): Edit {
    val s = minOf(selStart, selEnd).coerceIn(0, text.length)
    val ls = if (s == 0) 0 else text.lastIndexOf('\n', s - 1) + 1
    val leIdx = text.indexOf('\n', ls)
    val first = text.substring(ls, if (leIdx < 0) text.length else leIdx)
    val current = kindOf(linePrefixRe.find(first)?.groupValues?.get(2) ?: "")
    val next = when (current) {
        LineKind.H1 -> LineKind.H2
        LineKind.H2 -> LineKind.H3
        LineKind.H3 -> LineKind.H3
        else -> LineKind.H1
    }
    return toggleLinePrefix(text, selStart, selEnd, next)
}

/** Inserta un bloque en su propia línea. [caret] es la posición del cursor dentro del bloque. */
fun insertBlock(text: String, selStart: Int, selEnd: Int, block: String, caret: Int = block.length): Edit {
    val s = minOf(selStart, selEnd).coerceIn(0, text.length)
    val e = maxOf(selStart, selEnd).coerceIn(0, text.length)
    val nlBefore = s > 0 && text[s - 1] != '\n'
    val nlAfter = e < text.length && text[e] != '\n'
    val ins = (if (nlBefore) "\n" else "") + block + (if (nlAfter) "\n" else "")
    val nt = text.substring(0, s) + ins + text.substring(e)
    val c = s + (if (nlBefore) 1 else 0) + caret
    return Edit(nt, c, c)
}

private val listItemRe = Regex("^(\\s*)(- \\[[ xX]\\] |[-*+] |(\\d+)([.)]) |> )(.*)$")

/**
 * Continuación automática de listas: si el usuario acaba de pulsar Intro al final de un
 * elemento de lista, añade el marcador siguiente; si el elemento estaba vacío, cierra la lista.
 * Devuelve null si no hay nada que hacer.
 */
fun continueList(oldText: String, newText: String, cursor: Int): Edit? {
    if (newText.length != oldText.length + 1) return null
    if (cursor <= 0 || cursor > newText.length || newText[cursor - 1] != '\n') return null
    if (!oldText.regionMatches(0, newText, 0, cursor - 1)) return null
    if (!oldText.regionMatches(cursor - 1, newText, cursor, newText.length - cursor)) return null
    val prevStart = if (cursor < 2) 0 else newText.lastIndexOf('\n', cursor - 2) + 1
    val prevLine = newText.substring(prevStart, cursor - 1)
    val m = listItemRe.find(prevLine) ?: return null
    val indent = m.groupValues[1]
    val marker = m.groupValues[2]
    val rest = m.groupValues[5]
    if (rest.isBlank()) {
        // Elemento vacío: se elimina el marcador y se sale de la lista.
        val nt = newText.substring(0, prevStart) + newText.substring(cursor)
        return Edit(nt, prevStart, prevStart)
    }
    val next = when {
        marker.startsWith("- [") -> "- [ ] "
        m.groupValues[3].isNotEmpty() -> ((m.groupValues[3].toIntOrNull() ?: 0) + 1).toString() + m.groupValues[4] + " "
        else -> marker
    }
    val ins = indent + next
    val nt = newText.substring(0, cursor) + ins + newText.substring(cursor)
    return Edit(nt, cursor + ins.length, cursor + ins.length)
}
