package com.jschaofan.ragagent.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 轻量 Markdown 展示器，覆盖聊天回复需要的标题、列表、引用、代码块和表格。
 *
 * 这里不引入 WebView，确保流式文本更新时仍由 Compose 原生渲染。
 */
@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val blocks = parseMarkdown(markdown)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> MarkdownHeading(block)
                is MarkdownBlock.Paragraph -> Text(
                    text = inlineMarkdown(block.text),
                    style = MaterialTheme.typography.bodyMedium,
                )

                is MarkdownBlock.ListItem -> MarkdownListItem(block)
                is MarkdownBlock.Quote -> Text(
                    text = inlineMarkdown(block.text),
                    modifier = Modifier
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        .padding(10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                is MarkdownBlock.Code -> Text(
                    text = block.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small,
                        )
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )

                is MarkdownBlock.Table -> MarkdownTable(block)
            }
        }
    }
}

@Composable
private fun MarkdownHeading(block: MarkdownBlock.Heading) {
    val style = when (block.level) {
        1 -> MaterialTheme.typography.titleLarge
        2 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    Text(
        text = inlineMarkdown(block.text),
        style = style,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun MarkdownListItem(block: MarkdownBlock.ListItem) {
    Row {
        Text(
            text = block.marker,
            modifier = Modifier.width(24.dp),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = inlineMarkdown(block.text),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun MarkdownTable(table: MarkdownBlock.Table) {
    val scrollState = rememberScrollState()
    val rows = listOf(table.headers) + table.rows

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row {
                row.forEach { cell ->
                    Text(
                        text = inlineMarkdown(cell),
                        modifier = Modifier
                            .width(TABLE_CELL_WIDTH)
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            .background(
                                if (rowIndex == 0) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    Color.Transparent
                                },
                            )
                            .padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

/**
 * 支持粗体和行内代码。流式回复可能暂时缺少闭合标记，此时按普通文本展示。
 */
private fun inlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var index = 0
    while (index < text.length) {
        when {
            text.startsWith("**", index) -> {
                val end = text.indexOf("**", startIndex = index + 2)
                if (end > index) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(text.substring(index + 2, end))
                    pop()
                    index = end + 2
                } else {
                    append(text[index++])
                }
            }

            text[index] == '`' -> {
                val end = text.indexOf('`', startIndex = index + 1)
                if (end > index) {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x1A000000),
                        ),
                    )
                    append(text.substring(index + 1, end))
                    pop()
                    index = end + 1
                } else {
                    append(text[index++])
                }
            }

            else -> append(text[index++])
        }
    }
}

private fun parseMarkdown(markdown: String): List<MarkdownBlock> {
    val lines = markdown.replace("\r\n", "\n").split('\n')
    val blocks = mutableListOf<MarkdownBlock>()
    var index = 0

    while (index < lines.size) {
        val line = lines[index]
        when {
            line.isBlank() -> index++

            line.trimStart().startsWith("```") -> {
                val codeLines = mutableListOf<String>()
                index++
                while (index < lines.size && !lines[index].trimStart().startsWith("```")) {
                    codeLines += lines[index]
                    index++
                }
                if (index < lines.size) index++
                blocks += MarkdownBlock.Code(codeLines.joinToString("\n"))
            }

            index + 1 < lines.size &&
                isTableRow(line) &&
                isTableSeparator(lines[index + 1]) -> {
                val headers = splitTableRow(line)
                val rows = mutableListOf<List<String>>()
                index += 2
                while (index < lines.size && isTableRow(lines[index])) {
                    rows += splitTableRow(lines[index])
                    index++
                }
                blocks += MarkdownBlock.Table(headers = headers, rows = rows)
            }

            HEADING_REGEX.matches(line) -> {
                val match = HEADING_REGEX.matchEntire(line)!!
                blocks += MarkdownBlock.Heading(
                    level = match.groupValues[1].length,
                    text = match.groupValues[2],
                )
                index++
            }

            UNORDERED_LIST_REGEX.matches(line) -> {
                val match = UNORDERED_LIST_REGEX.matchEntire(line)!!
                blocks += MarkdownBlock.ListItem(marker = "•", text = match.groupValues[1])
                index++
            }

            ORDERED_LIST_REGEX.matches(line) -> {
                val match = ORDERED_LIST_REGEX.matchEntire(line)!!
                blocks += MarkdownBlock.ListItem(
                    marker = "${match.groupValues[1]}.",
                    text = match.groupValues[2],
                )
                index++
            }

            line.trimStart().startsWith(">") -> {
                blocks += MarkdownBlock.Quote(line.trimStart().removePrefix(">").trim())
                index++
            }

            else -> {
                val paragraph = mutableListOf(line.trim())
                index++
                while (
                    index < lines.size &&
                    lines[index].isNotBlank() &&
                    !startsNewBlock(lines, index)
                ) {
                    paragraph += lines[index].trim()
                    index++
                }
                blocks += MarkdownBlock.Paragraph(paragraph.joinToString("\n"))
            }
        }
    }
    return blocks
}

private fun startsNewBlock(lines: List<String>, index: Int): Boolean {
    val line = lines[index]
    return line.trimStart().startsWith("```") ||
        HEADING_REGEX.matches(line) ||
        UNORDERED_LIST_REGEX.matches(line) ||
        ORDERED_LIST_REGEX.matches(line) ||
        line.trimStart().startsWith(">") ||
        (
            index + 1 < lines.size &&
                isTableRow(line) &&
                isTableSeparator(lines[index + 1])
            )
}

private fun isTableRow(line: String): Boolean = line.count { it == '|' } >= 2

private fun isTableSeparator(line: String): Boolean {
    val cells = splitTableRow(line)
    return cells.isNotEmpty() && cells.all { cell ->
        cell.replace(":", "").trim().let { value ->
            value.length >= 3 && value.all { it == '-' }
        }
    }
}

private fun splitTableRow(line: String): List<String> =
    line.trim().trim('|').split('|').map { cell -> cell.trim() }

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class ListItem(val marker: String, val text: String) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data class Code(val text: String) : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
}

private val HEADING_REGEX = Regex("""^\s*(#{1,3})\s+(.+)$""")
private val UNORDERED_LIST_REGEX = Regex("""^\s*[-*+]\s+(.+)$""")
private val ORDERED_LIST_REGEX = Regex("""^\s*(\d+)\.\s+(.+)$""")
private val TABLE_CELL_WIDTH = 144.dp
