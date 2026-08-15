package com.inkleaf.app.domain.parser

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily

object SyntaxHighlighter {

    private val commentPattern = "(?://.*|/\\*[\\s\\S]*?\\*/|#.*)"
    private val stringPattern = "(?:\"\"\"[\\s\\S]*?\"\"\"|\\\"(?:\\\\\\\"|[^\\\"])*\\\"|'(?:\\\\'|[^'])*')"
    private val numberPattern = "\\b(?:\\d+(?:\\.\\d+)?|0x[0-9a-fA-F]+)\\b"
    private val keywordPattern = "\\b(?:package|import|class|interface|object|fun|val|var|return|if|else|for|while|do|when|try|catch|finally|throw|as|in|is|super|this|typeof|function|let|const|def|elif|lambda|pass|break|continue|public|private|protected|internal|static|final|void|new|instanceof|select|from|where|insert|into|update|delete|join|on|group|by|order|having|and|or|not|null|true|false|echo|mkdir|cd|rm|git|npm|gradlew|gradle|build)\\b"
    private val annotationPattern = "@[a-zA-Z_][a-zA-Z0-9_]*"
    private val typePattern = "\\b[A-Z][a-zA-Z0-9_]*\\b"

    private val regex = Regex(
        "(?<COMMENT>$commentPattern)" +
        "|(?<STRING>$stringPattern)" +
        "|(?<KEYWORD>$keywordPattern)" +
        "|(?<ANNOTATION>$annotationPattern)" +
        "|(?<TYPE>$typePattern)" +
        "|(?<NUMBER>$numberPattern)"
    )

    // Colors tailored for a dark terminal card
    private val commentStyle = SpanStyle(color = Color(0xFF64748B)) // slate-500
    private val stringStyle = SpanStyle(color = Color(0xFFFBBF24)) // amber-400
    private val keywordStyle = SpanStyle(color = Color(0xFF38BDF8)) // sky-400
    private val annotationStyle = SpanStyle(color = Color(0xFFA78BFA)) // violet-400
    private val typeStyle = SpanStyle(color = Color(0xFF34D399)) // emerald-400
    private val numberStyle = SpanStyle(color = Color(0xFFF472B6)) // pink-400

    fun highlight(code: String, language: String?, searchQuery: String): AnnotatedString {
        val builder = AnnotatedString.Builder(code)

        // Only run highlighter for known/supported code blocks
        val normalizedLang = normalizeCodeLanguage(language)
        if (normalizedLang != null && normalizedLang.lowercase() != "text" && normalizedLang.lowercase() != "plain") {
            try {
                val matches = regex.findAll(code)
                for (match in matches) {
                    val groups = match.groups
                    val style = when {
                        groups["COMMENT"] != null -> commentStyle
                        groups["STRING"] != null -> stringStyle
                        groups["KEYWORD"] != null -> keywordStyle
                        groups["ANNOTATION"] != null -> annotationStyle
                        groups["TYPE"] != null -> typeStyle
                        groups["NUMBER"] != null -> numberStyle
                        else -> null
                    }
                    if (style != null) {
                        builder.addStyle(style, match.range.first, match.range.last + 1)
                    }
                }
            } catch (e: Exception) {
                // Fallback gracefully on parsing/regex timeouts
            }
        }

        // Highlight search queries on top of syntax highlighting
        if (searchQuery.isNotEmpty()) {
            var startIndex = code.indexOf(searchQuery, ignoreCase = true)
            while (startIndex != -1) {
                val endIndex = startIndex + searchQuery.length
                builder.addStyle(
                    style = SpanStyle(
                        background = Color.Yellow.copy(alpha = 0.6f),
                        color = Color.Black
                    ),
                    start = startIndex,
                    end = endIndex
                )
                startIndex = code.indexOf(searchQuery, startIndex + 1, ignoreCase = true)
            }
        }

        return builder.toAnnotatedString()
    }
}
