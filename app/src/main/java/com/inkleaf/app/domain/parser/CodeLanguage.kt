package com.inkleaf.app.domain.parser

private val recognizedCodeLanguages = setOf(
    "bash", "sh", "shell", "zsh", "fish", "powershell", "ps1",
    "kotlin", "java", "javascript", "js", "typescript", "ts",
    "json", "xml", "yaml", "yml", "python", "py", "rust", "go",
    "c", "cpp", "c++", "csharp", "cs", "swift", "sql", "html",
    "css", "scss", "gradle", "groovy", "dart", "ruby", "php", "regex",
    "markdown", "md"
)

fun normalizeCodeLanguage(info: String?): String? {
    val token = info
        ?.trim()
        ?.split(Regex("\\s+"))
        ?.firstOrNull()
        ?.trim('`', '{', '}', '[', ']', '(', ')')
        ?.lowercase()
        ?.takeIf { it.isNotEmpty() }

    return token?.takeIf { it in recognizedCodeLanguages }
}

fun normalizedFenceInfo(info: String?): String? = info
    ?.trim()
    ?.split(Regex("\\s+"))
    ?.firstOrNull()
    ?.lowercase()
    ?.takeIf { it.isNotEmpty() }
