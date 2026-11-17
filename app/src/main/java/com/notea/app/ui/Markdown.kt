package com.notea.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notea.app.md.Edit
import com.notea.app.md.LineKind
import com.notea.app.md.MdBlock
import com.notea.app.md.continueList
import com.notea.app.md.cycleHeading
import com.notea.app.md.insertBlock
import com.notea.app.md.parseMarkdown
import com.notea.app.md.toggleLinePrefix
import com.notea.app.md.wrapSelection

// ---------------------------------------------------------------------------
// Formato en línea: **negrita**, *cursiva*, ~~tachado~~, `código`, [enlaces](url)
// ---------------------------------------------------------------------------

private class InlineStyle(val link: Color, val codeBg: Color)

private fun AnnotatedString.Builder.appendInline(s: String, st: InlineStyle, depth: Int = 0) {
    if (depth > 6) {
        append(s)
        return
    }
    val plain = StringBuilder()
    fun flush() {
        if (plain.isNotEmpty()) {
            append(plain.toString())
            plain.setLength(0)
        }
    }

    var i = 0
    val n = s.length
    while (i < n) {
        val c = s[i]
        // Carácter escapado
        if (c == '\\' && i + 1 < n && "*_`~[]#\\".indexOf(s[i + 1]) >= 0) {
            plain.append(s[i + 1])
            i += 2
            continue
        }
        // Código en línea
        if (c == '`') {
            val end = s.indexOf('`', i + 1)
            if (end > i + 1) {
                flush()
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = st.codeBg)) {
                    append(s.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }
        // Negrita + cursiva
        if (s.startsWith("***", i)) {
            val end = s.indexOf("***", i + 3)
            if (end > i + 3) {
                flush()
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                    appendInline(s.substring(i + 3, end), st, depth + 1)
                }
                i = end + 3
                continue
            }
        }
        // Negrita
        if (s.startsWith("**", i) || s.startsWith("__", i)) {
            val d = s.substring(i, i + 2)
            val end = s.indexOf(d, i + 2)
            if (end > i + 2) {
                flush()
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendInline(s.substring(i + 2, end), st, depth + 1)
                }
                i = end + 2
                continue
            }
        }
        // Tachado
        if (s.startsWith("~~", i)) {
            val end = s.indexOf("~~", i + 2)
            if (end > i + 2) {
                flush()
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    appendInline(s.substring(i + 2, end), st, depth + 1)
                }
                i = end + 2
                continue
            }
        }
        // Cursiva
        if (c == '*' || (c == '_' && (i == 0 || !s[i - 1].isLetterOrDigit()))) {
            val end = s.indexOf(c, i + 1)
            if (end > i + 1 && !s[i + 1].isWhitespace() && !s[end - 1].isWhitespace()) {
                flush()
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendInline(s.substring(i + 1, end), st, depth + 1)
                }
                i = end + 1
                continue
            }
        }
        // Enlace [texto](url)
        if (c == '[') {
            val close = s.indexOf(']', i + 1)
            if (close > i && close + 1 < n && s[close + 1] == '(') {
                val endUrl = s.indexOf(')', close + 2)
                if (endUrl > close + 1) {
                    flush()
                    val url = s.substring(close + 2, endUrl).trim()
                    val label = s.substring(i + 1, close)
                    appendLink(url, label.ifBlank { url }, st)
                    i = endUrl + 1
                    continue
                }
            }
        }
        // URL suelta
        if (c == 'h' && (s.startsWith("http://", i) || s.startsWith("https://", i))) {
            var end = i
            while (end < n && !s[end].isWhitespace()) end++
            while (end > i && ".,;:!?)".indexOf(s[end - 1]) >= 0) end--
            flush()
            val url = s.substring(i, end)
            appendLink(url, url, st)
            i = end
            continue
        }
        plain.append(c)
        i++
    }
    flush()
}

private fun AnnotatedString.Builder.appendLink(url: String, label: String, st: InlineStyle) {
    val styles = TextLinkStyles(style = SpanStyle(color = st.link, textDecoration = TextDecoration.Underline))
    withLink(LinkAnnotation.Url(url, styles)) {
        append(label)
    }
}

// ---------------------------------------------------------------------------
// Vista renderizada
// ---------------------------------------------------------------------------

/**
 * Muestra Markdown renderizado. Si se pasa [onToggleTask], las casillas son pulsables
 * y devuelven el índice de línea del texto fuente que hay que alternar.
 */
@Composable
fun MarkdownView(
    text: String,
    modifier: Modifier = Modifier,
    onToggleTask: ((Int) -> Unit)? = null,
) {
    val blocks = remember(text) { parseMarkdown(text) }
    val cs = MaterialTheme.colorScheme
    val st = remember(cs.primary, cs.surfaceContainerHighest) { InlineStyle(cs.primary, cs.surfaceContainerHighest) }
    val base = MaterialTheme.typography.bodyLarge

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (b in blocks) {
            when (b) {
                is MdBlock.Heading -> {
                    val style = when (b.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.headlineSmall
                        3 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    }
                    Text(
                        buildAnnotatedString { appendInline(b.text, st) },
                        style = style,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                is MdBlock.Paragraph -> Text(buildAnnotatedString { appendInline(b.text, st) }, style = base)

                is MdBlock.Bullet -> Row(Modifier.padding(start = (b.indent * 18).dp)) {
                    Text("•", style = base, textAlign = TextAlign.Center, modifier = Modifier.width(22.dp))
                    Text(buildAnnotatedString { appendInline(b.text, st) }, style = base)
                }

                is MdBlock.Ordered -> Row(Modifier.padding(start = (b.indent * 18).dp)) {
                    Text(
                        b.number + ".",
                        style = base,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.widthIn(min = 22.dp).padding(end = 6.dp),
                    )
                    Text(buildAnnotatedString { appendInline(b.text, st) }, style = base)
                }

                is MdBlock.Task -> {
                    val rowMod = Modifier
                        .fillMaxWidth()
                        .padding(start = (b.indent * 18).dp)
                        .clip(RoundedCornerShape(8.dp))
                    Row(
                        if (onToggleTask != null) rowMod.clickable { onToggleTask(b.line) } else rowMod,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = b.checked,
                            onCheckedChange = null,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp).size(22.dp),
                        )
                        Text(
                            buildAnnotatedString { appendInline(b.text, st) },
                            style = base,
                            color = if (b.checked) cs.onSurfaceVariant else cs.onSurface,
                            textDecoration = if (b.checked) TextDecoration.LineThrough else null,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }

                is MdBlock.Quote -> Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(cs.primary, RoundedCornerShape(2.dp))
                    )
                    Text(
                        buildAnnotatedString { appendInline(b.text, st) },
                        style = base,
                        fontStyle = FontStyle.Italic,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
                    )
                }

                is MdBlock.Code -> Surface(
                    color = cs.surfaceContainerHighest,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        b.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(12.dp),
                    )
                }

                is MdBlock.Table -> MarkdownTable(b, st)

                MdBlock.Rule -> HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun MarkdownTable(table: MdBlock.Table, st: InlineStyle) {
    val cs = MaterialTheme.colorScheme
    val cols = maxOf(table.header.size, table.rows.maxOfOrNull { it.size } ?: 0).coerceAtLeast(1)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, cs.outlineVariant, RoundedCornerShape(10.dp))
    ) {
        Row(Modifier.fillMaxWidth().background(cs.surfaceContainerHigh)) {
            for (c in 0 until cols) {
                Text(
                    buildAnnotatedString { appendInline(table.header.getOrElse(c) { "" }, st) },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
        for (row in table.rows) {
            HorizontalDivider(color = cs.outlineVariant)
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until cols) {
                    Text(
                        buildAnnotatedString { appendInline(row.getOrElse(c) { "" }, st) },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Editor: resaltado de sintaxis en vivo + barra de formato
// ---------------------------------------------------------------------------

private val hlHeading = Regex("^(#{1,6})\\s+.*$")
private val hlListPrefix = Regex("^(\\s*)(- \\[[ xX]\\] |[-*+] |\\d+[.)] )")
private val hlCheckedTask = Regex("^\\s*[-*+] \\[[xX]\\] ")
private val hlBold = Regex("(\\*\\*|__)(?=\\S)(.+?)(?<=\\S)\\1")
private val hlItalic = Regex("(?<![*\\w])\\*(?=[^\\s*])([^*\\n]+?)(?<=[^\\s*])\\*(?![*\\w])")
private val hlStrike = Regex("~~(?=\\S)(.+?)(?<=\\S)~~")
private val hlCode = Regex("`([^`\\n]+)`")
private val hlLink = Regex("\\[([^\\]\\n]*)\\]\\(([^)\\n]*)\\)")

/**
 * Resalta la sintaxis Markdown mientras se escribe sin alterar el texto: los marcadores se
 * atenúan y el contenido se muestra ya en negrita, cursiva, como título, etc.
 */
class MarkdownHighlighter(
    private val marker: Color,
    private val accent: Color,
    private val codeBg: Color,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(highlight(text.text), OffsetMapping.Identity)

    private fun highlight(src: String): AnnotatedString = buildAnnotatedString {
        append(src)
        val dim = SpanStyle(color = marker)
        var offset = 0
        var inFence = false
        for (line in src.split('\n')) {
            val start = offset
            val end = offset + line.length
            offset = end + 1
            if (line.isEmpty()) continue

            if (line.trimStart().startsWith("```")) {
                inFence = !inFence
                addStyle(dim.copy(fontFamily = FontFamily.Monospace), start, end)
                continue
            }
            if (inFence) {
                addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg), start, end)
                continue
            }

            val heading = hlHeading.find(line)
            if (heading != null) {
                val level = heading.groupValues[1].length
                val size = when (level) {
                    1 -> 26.sp
                    2 -> 22.sp
                    3 -> 19.sp
                    else -> 17.sp
                }
                addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = size), start, end)
                addStyle(dim, start, start + level)
            } else if (line.startsWith(">")) {
                addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = marker), start, end)
            } else {
                val prefix = hlListPrefix.find(line)
                if (prefix != null) {
                    val ps = start + prefix.groupValues[1].length
                    val pe = start + prefix.value.length
                    addStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold), ps, pe)
                    if (hlCheckedTask.containsMatchIn(line) && pe < end) {
                        addStyle(SpanStyle(color = marker, textDecoration = TextDecoration.LineThrough), pe, end)
                    }
                }
            }

            for (m in hlBold.findAll(line)) {
                val a = start + m.range.first
                val b = start + m.range.last + 1
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), a, b)
                addStyle(dim, a, a + 2)
                addStyle(dim, b - 2, b)
            }
            for (m in hlItalic.findAll(line)) {
                val a = start + m.range.first
                val b = start + m.range.last + 1
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), a, b)
                addStyle(dim, a, a + 1)
                addStyle(dim, b - 1, b)
            }
            for (m in hlStrike.findAll(line)) {
                val a = start + m.range.first
                val b = start + m.range.last + 1
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), a + 2, b - 2)
                addStyle(dim, a, a + 2)
                addStyle(dim, b - 2, b)
            }
            for (m in hlCode.findAll(line)) {
                val a = start + m.range.first
                val b = start + m.range.last + 1
                addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg), a, b)
            }
            for (m in hlLink.findAll(line)) {
                val a = start + m.range.first
                val b = start + m.range.last + 1
                val labelEnd = a + 1 + m.groupValues[1].length
                addStyle(SpanStyle(color = accent, textDecoration = TextDecoration.Underline), a + 1, labelEnd)
                addStyle(dim, a, a + 1)
                addStyle(dim, labelEnd, b)
            }
        }
    }
}

/** Campo de texto Markdown con resaltado en vivo y continuación automática de listas. */
@Composable
fun MarkdownEditorField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Escribe aquí…",
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val cs = MaterialTheme.colorScheme
    val highlighter = remember(cs.outline, cs.primary, cs.surfaceContainerHighest) {
        MarkdownHighlighter(cs.outline, cs.primary, cs.surfaceContainerHighest)
    }
    BasicTextField(
        value = value,
        onValueChange = { nv ->
            val fixed = if (nv.selection.collapsed) continueList(value.text, nv.text, nv.selection.start) else null
            if (fixed != null) {
                onValueChange(TextFieldValue(fixed.text, TextRange(fixed.start, fixed.end)))
            } else {
                onValueChange(nv)
            }
        },
        modifier = modifier,
        textStyle = textStyle.copy(color = cs.onSurface),
        cursorBrush = SolidColor(cs.primary),
        visualTransformation = highlighter,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        decorationBox = { inner ->
            Box {
                if (value.text.isEmpty()) {
                    Text(placeholder, style = textStyle, color = cs.onSurfaceVariant.copy(alpha = 0.6f))
                }
                inner()
            }
        },
    )
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = label)
    }
}

/** Barra de formato: cada botón inserta o alterna la sintaxis Markdown en la selección. */
@Composable
fun MarkdownToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = value.text
    val s = value.selection.start
    val e = value.selection.end
    fun commit(edit: Edit) {
        onValueChange(TextFieldValue(edit.text, TextRange(edit.start, edit.end)))
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolButton(Icons.Filled.CheckBox, "Lista de casillas") { commit(toggleLinePrefix(t, s, e, LineKind.TASK)) }
            ToolButton(Icons.AutoMirrored.Filled.FormatListBulleted, "Lista") { commit(toggleLinePrefix(t, s, e, LineKind.BULLET)) }
            ToolButton(Icons.Filled.FormatListNumbered, "Lista numerada") { commit(toggleLinePrefix(t, s, e, LineKind.ORDERED)) }
            ToolButton(Icons.Filled.Title, "Título") { commit(cycleHeading(t, s, e)) }
            ToolButton(Icons.Filled.FormatBold, "Negrita") { commit(wrapSelection(t, s, e, "**")) }
            ToolButton(Icons.Filled.FormatItalic, "Cursiva") { commit(wrapSelection(t, s, e, "*")) }
            ToolButton(Icons.Filled.FormatStrikethrough, "Tachado") { commit(wrapSelection(t, s, e, "~~")) }
            ToolButton(Icons.Filled.FormatQuote, "Cita") { commit(toggleLinePrefix(t, s, e, LineKind.QUOTE)) }
            ToolButton(Icons.Filled.Code, "Código") { commit(wrapSelection(t, s, e, "`")) }
            ToolButton(Icons.Filled.DataObject, "Bloque de código") { commit(insertBlock(t, s, e, "```\n\n```", 4)) }
            ToolButton(Icons.Filled.Link, "Enlace") { commit(wrapSelection(t, s, e, "[", "](https://)")) }
            ToolButton(Icons.Filled.HorizontalRule, "Separador") { commit(insertBlock(t, s, e, "---\n")) }
        }
    }
}
