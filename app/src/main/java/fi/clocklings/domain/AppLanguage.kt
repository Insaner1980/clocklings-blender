package fi.clocklings.domain

enum class AppLanguage(
    val tag: String,
) {
    ENGLISH("en"),
    FINNISH("fi"),
    SWEDISH("sv"),
    GERMAN("de"),
    FRENCH("fr"),
    SPANISH("es-ES"),
    PORTUGUESE_BRAZIL("pt-BR"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage? {
            val primary = tag?.substringBefore('-')?.lowercase()
            return entries.firstOrNull { it.tag.substringBefore('-') == primary }
        }
    }
}

fun resolveLanguage(tags: List<String>): AppLanguage =
    tags.firstNotNullOfOrNull(AppLanguage::fromTag) ?: AppLanguage.ENGLISH
