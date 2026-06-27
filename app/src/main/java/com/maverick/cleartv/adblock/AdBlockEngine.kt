package com.maverick.cleartv.adblock

import android.util.Log

class AdBlockEngine {

    private val blockedDomains = HashSet<String>(4096)
    private val whitelistDomains = HashSet<String>(256)
    private val blockedPatterns = mutableListOf<Regex>()
    private val whitelistPatterns = mutableListOf<Regex>()
    private val cosmeticFilters = HashMap<String, MutableList<String>>()

    @Volatile var ruleCount = 0
        private set

    fun loadRules(lines: List<String>) {
        var loaded = 0
        for (line in lines) {
            val rule = FilterParser.parse(line) ?: continue
            when {
                rule.isCosmeticFilter -> {
                    val selector = rule.cosmeticSelector ?: continue
                    val key = rule.cosmeticDomain ?: "*"
                    cosmeticFilters.getOrPut(key) { mutableListOf() }.add(selector)
                }
                rule.isDomainRule -> {
                    val domain = rule.domain ?: continue
                    if (rule.isException) whitelistDomains.add(domain)
                    else blockedDomains.add(domain)
                }
                rule.pattern != null -> {
                    // Cap regex patterns — domain rules cover 95% of blocks, patterns are slow
                    if (blockedPatterns.size < 2000) {
                        if (rule.isException) whitelistPatterns.add(rule.pattern)
                        else blockedPatterns.add(rule.pattern)
                    }
                }
            }
            loaded++
        }
        ruleCount = blockedDomains.size + blockedPatterns.size
        Log.d("AdBlockEngine", "Loaded $loaded rules (${blockedDomains.size} domains, ${blockedPatterns.size} patterns)")
    }

    fun shouldBlock(url: String, pageHost: String = ""): Boolean {
        if (url.isBlank()) return false

        val host = extractHost(url)

        // Check whitelist first
        if (host != null && isWhitelisted(host)) return false
        if (whitelistPatterns.any { it.containsMatchIn(url) }) return false

        // Check domain blocklist (includes subdomains)
        if (host != null && isDomainBlocked(host)) return true

        // Check pattern blocklist
        return blockedPatterns.any { it.containsMatchIn(url) }
    }

    fun getCosmeticSelectors(pageHost: String): List<String> {
        val selectors = mutableListOf<String>()
        selectors.addAll(cosmeticFilters["*"] ?: emptyList())
        // Add host-specific selectors
        var host = pageHost
        while (host.isNotEmpty()) {
            cosmeticFilters[host]?.let { selectors.addAll(it) }
            val dot = host.indexOf('.')
            if (dot == -1) break
            host = host.substring(dot + 1)
        }
        return selectors
    }

    private fun isDomainBlocked(host: String): Boolean {
        if (blockedDomains.contains(host)) return true
        var h = host
        while (true) {
            val dot = h.indexOf('.')
            if (dot == -1) break
            h = h.substring(dot + 1)
            if (blockedDomains.contains(h)) return true
        }
        return false
    }

    private fun isWhitelisted(host: String): Boolean {
        if (whitelistDomains.contains(host)) return true
        var h = host
        while (true) {
            val dot = h.indexOf('.')
            if (dot == -1) break
            h = h.substring(dot + 1)
            if (whitelistDomains.contains(h)) return true
        }
        return false
    }

    private fun extractHost(url: String): String? {
        return try {
            val withoutScheme = url.substringAfter("://")
            withoutScheme.substringBefore("/").substringBefore(":").lowercase()
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        blockedDomains.clear()
        whitelistDomains.clear()
        blockedPatterns.clear()
        whitelistPatterns.clear()
        cosmeticFilters.clear()
        ruleCount = 0
    }
}
