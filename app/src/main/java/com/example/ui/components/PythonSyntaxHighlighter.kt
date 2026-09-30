package com.example.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.*

object PythonSyntaxHighlighter {

    private val KEYWORDS = setOf(
        "def", "class", "import", "from", "return", "if", "elif", "else",
        "try", "except", "finally", "while", "for", "in", "is", "not",
        "and", "or", "lambda", "with", "as", "yield", "async", "await",
        "pass", "break", "continue", "raise", "None", "True", "False",
        "self", "super", "global", "nonlocal", "assert", "del"
    )

    fun highlight(code: String): AnnotatedString {
        return buildAnnotatedString {
            append(code)

            val lines = code.split("\n")
            var lineStartIndex = 0

            for (line in lines) {
                // Check comments
                val commentIndex = line.indexOf("#")
                if (commentIndex != -1) {
                    val start = lineStartIndex + commentIndex
                    val end = lineStartIndex + line.length
                    addStyle(
                        SpanStyle(color = CodeComment, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        start,
                        end
                    )
                }

                // Check strings (simple regex)
                val codeBeforeComment = if (commentIndex != -1) line.substring(0, commentIndex) else line

                // Match single or double quoted strings
                val stringRegex = Regex("""(["'])(?:(?=(\\?))\2.)*?\1""")
                stringRegex.findAll(codeBeforeComment).forEach { match ->
                    val start = lineStartIndex + match.range.first
                    val end = lineStartIndex + match.range.last + 1
                    addStyle(SpanStyle(color = CodeString), start, end)
                }

                // Match keywords and identifiers
                val wordRegex = Regex("""\b([a-zA-Z_][a-zA-Z0-9_]*)\b""")
                wordRegex.findAll(codeBeforeComment).forEach { match ->
                    val word = match.value
                    val start = lineStartIndex + match.range.first
                    val end = lineStartIndex + match.range.last + 1

                    if (KEYWORDS.contains(word)) {
                        addStyle(SpanStyle(color = CodeKeyword, fontWeight = FontWeight.Bold), start, end)
                    }
                }

                // Match numbers
                val numRegex = Regex("""\b\d+(\.\d+)?\b""")
                numRegex.findAll(codeBeforeComment).forEach { match ->
                    val start = lineStartIndex + match.range.first
                    val end = lineStartIndex + match.range.last + 1
                    addStyle(SpanStyle(color = CodeNumber), start, end)
                }

                // Match decorators
                val decoratorRegex = Regex("""@([a-zA-Z_][a-zA-Z0-9_]*)""")
                decoratorRegex.findAll(codeBeforeComment).forEach { match ->
                    val start = lineStartIndex + match.range.first
                    val end = lineStartIndex + match.range.last + 1
                    addStyle(SpanStyle(color = CodeFunction), start, end)
                }

                lineStartIndex += line.length + 1 // +1 for the newline
            }
        }
    }
}
