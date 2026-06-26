package com.maverick.cleartv.adblock

data class FilterRule(
    val raw: String,
    val isException: Boolean,
    val isDomainRule: Boolean,
    val domain: String?,
    val pattern: Regex?,
    val isCosmeticFilter: Boolean,
    val cosmeticSelector: String?,
    val cosmeticDomain: String?
)

object FilterParser {

    fun parse(line: String): FilterRule? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("!") || trimmed.startsWith("[")) return null

        // Cosmetic filter: example.com##.selector or ##.selector
        if (trimmed.contains("##")) {
            val parts = trimmed.split("##", limit = 2)
            val cosmeticDomain = parts[0].ifEmpty { null }
            return FilterRule(
                raw = trimmed,
                isException = false,
                isDomainRule = false,
                domain = null,
                pattern = null,
                isCosmeticFilter = true,
                cosmeticSelector = parts[1],
                cosmeticDomain = cosmeticDomain
            )
        }

        // Cosmetic exception: #@#
        if (trimmed.contains("#@#")) return null

        val isException = trimmed.startsWith("@@")
        val rule = if (isException) trimmed.substring(2) else trimmed

        // Strip options ($script,image, etc) — we don't filter by type yet
        val ruleWithoutOptions = rule.substringBefore("$")

        // Domain anchor rule: ||domain.com^ or ||domain.com/
        if (ruleWithoutOptions.startsWith("||")) {
            val rest = ruleWithoutOptions.substring(2).trimEnd('^', '/')
            val domainPart = rest.substringBefore("/")
            if (!domainPart.contains("*") && domainPart.isNotEmpty()) {
                return FilterRule(
                    raw = trimmed,
                    isException = isException,
                    isDomainRule = true,
                    domain = domainPart.lowercase(),
                    pattern = null,
                    isCosmeticFilter = false,
                    cosmeticSelector = null,
                    cosmeticDomain = null
                )
            }
        }

        // Generic URL pattern
        return try {
            val regexStr = ruleWithoutOptions
                .replace(".", "\\.")
                .replace("?", "\\?")
                .replace("*", ".*")
                .replace("^", "([/?&#]|$)")
                .let { if (it.startsWith("||")) "https?://([^/]+\\.)?" + it.substring(2) else it }
                .let { if (it.startsWith("|")) "^" + it.substring(1) else it }
                .let { if (it.endsWith("|")) it.dropLast(1) + "$" else it }
            FilterRule(
                raw = trimmed,
                isException = isException,
                isDomainRule = false,
                domain = null,
                pattern = Regex(regexStr, RegexOption.IGNORE_CASE),
                isCosmeticFilter = false,
                cosmeticSelector = null,
                cosmeticDomain = null
            )
        } catch (e: Exception) {
            null
        }
    }
}
