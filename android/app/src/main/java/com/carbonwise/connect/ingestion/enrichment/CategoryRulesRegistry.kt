package com.carbonwise.connect.ingestion.enrichment

import com.carbonwise.connect.ingestion.model.TransactionCandidate

/**
 * Configurable registry for Category inference rules.
 * Maps deterministic patterns (keywords, aliases, merchant types, and packages) to categories.
 */
class CategoryRulesRegistry {

    private val explicitKeywords = mutableListOf<Rule>()
    private val merchantAliases = mutableListOf<Rule>()
    private val merchantTypeMappings = mutableMapOf<String, String>()
    private val packageNameMappings = mutableListOf<Rule>()

    init {
        // Priority 1: Explicit keywords
        registerKeyword("amazon fresh", "GROCERY")
        registerKeyword("swiggy instamart", "GROCERY")

        // Priority 2: Merchant aliases
        registerAlias("uber eats", "FOOD")
        
        // Priority 3: Merchant Types
        registerMerchantTypeMapping("RESTAURANT", "FOOD")
        registerMerchantTypeMapping("CAFE", "FOOD")
        registerMerchantTypeMapping("GROCERY", "GROCERY")
        registerMerchantTypeMapping("ECOMMERCE", "SHOPPING")
        registerMerchantTypeMapping("TRANSPORT", "TRANSPORT")
        registerMerchantTypeMapping("FUEL", "FUEL")
        registerMerchantTypeMapping("AIRLINE", "FLIGHT")
        registerMerchantTypeMapping("RAILWAY", "TRAIN")
        registerMerchantTypeMapping("ELECTRICITY", "ELECTRICITY")
    }

    fun registerKeyword(keyword: String, category: String) {
        explicitKeywords.add(Rule(keyword.lowercase(), category))
    }

    fun registerAlias(alias: String, category: String) {
        merchantAliases.add(Rule(alias.lowercase(), category))
    }

    fun registerMerchantTypeMapping(merchantType: String, category: String) {
        merchantTypeMappings[merchantType.uppercase()] = category
    }

    fun registerPackageMapping(packageName: String, category: String) {
        packageNameMappings.add(Rule(packageName.lowercase(), category))
    }

    /**
     * Resolves the CategoryResult from candidate and merchantTypeResult following strict priority rules.
     */
    fun resolve(candidate: TransactionCandidate, merchantTypeResult: MerchantTypeResult): CategoryResult {
        val rawText = candidate.rawNotification.lowercase()
        val merchantText = candidate.merchant?.lowercase() ?: ""
        val packageName = candidate.sourceApp?.lowercase() ?: ""

        // Priority 1: Explicit notification keywords
        for (rule in explicitKeywords) {
            if (rawText.contains(rule.pattern) || merchantText.contains(rule.pattern)) {
                return CategoryResult(
                    category = rule.category,
                    confidence = 0.95,
                    matchedRule = "EXPLICIT_KEYWORD",
                    reasoning = "Notification text matched explicit keyword '${rule.pattern}'"
                )
            }
        }

        // Priority 2: Merchant aliases
        for (rule in merchantAliases) {
            if (rawText.contains(rule.pattern) || merchantText.contains(rule.pattern)) {
                return CategoryResult(
                    category = rule.category,
                    confidence = 0.94,
                    matchedRule = "MERCHANT_ALIAS",
                    reasoning = "Merchant alias '${rule.pattern}' matched ${rule.category} rule"
                )
            }
        }

        // Priority 3: Merchant Type
        val mType = merchantTypeResult.merchantType.uppercase()
        val catFromType = merchantTypeMappings[mType]
        if (catFromType != null) {
            return CategoryResult(
                category = catFromType,
                confidence = 0.85,
                matchedRule = "MERCHANT_TYPE",
                reasoning = "Mapped from merchant type '$mType'"
            )
        }

        // Priority 4: Package name
        for (rule in packageNameMappings) {
            if (packageName.contains(rule.pattern)) {
                return CategoryResult(
                    category = rule.category,
                    confidence = 0.80,
                    matchedRule = "PACKAGE_NAME",
                    reasoning = "Mapped from package name '${rule.pattern}'"
                )
            }
        }

        // Priority 5: Default UNKNOWN
        return CategoryResult(
            category = "UNKNOWN",
            confidence = 0.0,
            matchedRule = "DEFAULT_FALLBACK",
            reasoning = "No rules matched"
        )
    }

    private data class Rule(val pattern: String, val category: String)
}
