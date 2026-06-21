package com.carbonwise.connect.ingestion.enrichment

import com.carbonwise.connect.ingestion.model.TransactionCandidate

/**
 * Configurable registry mapping deterministic rules to Carbon Hints.
 * Follows strict priority order for resolution.
 */
class CarbonHintRulesRegistry {

    private val merchantAliases = mutableListOf<HintRule>()
    private val notificationKeywords = mutableListOf<HintRule>()
    private val merchantTypeMappings = mutableMapOf<String, HintRule>()
    private val categoryMappings = mutableMapOf<String, HintRule>()
    private val packageMappings = mutableListOf<HintRule>()

    init {
        // Priority 1: Merchant Aliases
        registerAlias("swiggy", "FOOD_DELIVERY")
        registerAlias("zomato", "FOOD_DELIVERY")
        registerAlias("starbucks", "CAFE_VISIT")
        registerAlias("uber", "RIDE_SHARE", transportMode = "CAR")
        registerAlias("ola", "RIDE_SHARE", transportMode = "CAR")
        registerAlias("rapido", "RIDE_SHARE", transportMode = "BIKE")
        registerAlias("irctc", "TRAIN_TRAVEL", transportMode = "TRAIN")
        registerAlias("air india", "AIR_TRAVEL", transportMode = "AIRCRAFT")
        registerAlias("indian oil", "FUEL_PURCHASE", fuelType = "PETROL")
        registerAlias("electricity board", "ELECTRICITY_PAYMENT", energyType = "GRID_ELECTRICITY")
        registerAlias("blinkit", "GROCERY_DELIVERY")
        registerAlias("amazon fresh", "GROCERY_DELIVERY")
        registerAlias("amazon", "ONLINE_SHOPPING")
        
        // Sort aliases to match longer, more specific ones first
        merchantAliases.sortByDescending { it.pattern.length }

        // Priority 2: Notification Keywords
        registerKeyword("hpcl diesel", "FUEL_PURCHASE", fuelType = "DIESEL")
        registerKeyword("uber eats", "FOOD_DELIVERY")

        // Priority 3: Merchant Type Mappings
        registerMerchantTypeMapping("RESTAURANT", "RESTAURANT_DINING")
        registerMerchantTypeMapping("CAFE", "CAFE_VISIT")
        registerMerchantTypeMapping("GROCERY", "GROCERY_PURCHASE")
        registerMerchantTypeMapping("TRANSPORT", "PUBLIC_TRANSPORT")
        registerMerchantTypeMapping("FUEL", "FUEL_PURCHASE", fuelType = "PETROL")
        registerMerchantTypeMapping("AIRLINE", "AIR_TRAVEL", transportMode = "AIRCRAFT")
        registerMerchantTypeMapping("RAILWAY", "TRAIN_TRAVEL", transportMode = "TRAIN")
        registerMerchantTypeMapping("ELECTRICITY", "ELECTRICITY_PAYMENT", energyType = "GRID_ELECTRICITY")
        registerMerchantTypeMapping("ECOMMERCE", "ONLINE_SHOPPING")

        // Priority 4: Category Mappings
        registerCategoryMapping("FOOD", "FOOD_DELIVERY")
        registerCategoryMapping("SHOPPING", "ONLINE_SHOPPING")
        registerCategoryMapping("TRANSPORT", "PUBLIC_TRANSPORT")
        registerCategoryMapping("FUEL", "FUEL_PURCHASE")
        registerCategoryMapping("ELECTRICITY", "ELECTRICITY_PAYMENT", energyType = "GRID_ELECTRICITY")
        registerCategoryMapping("FLIGHT", "AIR_TRAVEL", transportMode = "AIRCRAFT")
        registerCategoryMapping("TRAIN", "TRAIN_TRAVEL", transportMode = "TRAIN")
        registerCategoryMapping("ACCOMMODATION", "HOTEL_STAY")
        registerCategoryMapping("HEALTH", "MEDICAL_PURCHASE")
        registerCategoryMapping("ENTERTAINMENT", "ENTERTAINMENT")
        registerCategoryMapping("UTILITY", "UTILITY_PAYMENT")
        registerCategoryMapping("GROCERY", "GROCERY_PURCHASE")
    }

    fun registerAlias(alias: String, hint: String, transportMode: String? = null, fuelType: String? = null, energyType: String? = null, purchaseType: String? = null) {
        merchantAliases.add(HintRule(alias.lowercase(), hint, transportMode, fuelType, energyType, purchaseType))
        merchantAliases.sortByDescending { it.pattern.length }
    }

    fun registerKeyword(keyword: String, hint: String, transportMode: String? = null, fuelType: String? = null, energyType: String? = null, purchaseType: String? = null) {
        notificationKeywords.add(HintRule(keyword.lowercase(), hint, transportMode, fuelType, energyType, purchaseType))
        notificationKeywords.sortByDescending { it.pattern.length }
    }

    fun registerMerchantTypeMapping(merchantType: String, hint: String, transportMode: String? = null, fuelType: String? = null, energyType: String? = null, purchaseType: String? = null) {
        merchantTypeMappings[merchantType.uppercase()] = HintRule(merchantType.uppercase(), hint, transportMode, fuelType, energyType, purchaseType)
    }

    fun registerCategoryMapping(category: String, hint: String, transportMode: String? = null, fuelType: String? = null, energyType: String? = null, purchaseType: String? = null) {
        categoryMappings[category.uppercase()] = HintRule(category.uppercase(), hint, transportMode, fuelType, energyType, purchaseType)
    }

    fun registerPackageMapping(packageName: String, hint: String, transportMode: String? = null, fuelType: String? = null, energyType: String? = null, purchaseType: String? = null) {
        packageMappings.add(HintRule(packageName.lowercase(), hint, transportMode, fuelType, energyType, purchaseType))
    }

    /**
     * Resolves the CarbonHintResult following strict priority rules.
     */
    fun resolve(
        candidate: TransactionCandidate, 
        merchantTypeResult: MerchantTypeResult, 
        categoryResult: CategoryResult
    ): CarbonHintResult {
        val rawText = candidate.rawNotification.lowercase()
        val merchantText = candidate.merchant?.lowercase() ?: ""
        val packageName = candidate.sourceApp?.lowercase() ?: ""

        // Priority 1: Merchant Aliases
        for (rule in merchantAliases) {
            if (rawText.contains(rule.pattern) || merchantText.contains(rule.pattern)) {
                return CarbonHintResult(
                    carbonHint = rule.hint,
                    transportMode = rule.transportMode,
                    fuelType = rule.fuelType,
                    energyType = rule.energyType,
                    purchaseType = rule.purchaseType,
                    confidence = 0.95,
                    matchedRule = "MERCHANT_ALIAS",
                    reasoning = "Merchant alias '${rule.pattern}' matched ${rule.hint} registry."
                )
            }
        }

        // Priority 2: Notification Keywords
        for (rule in notificationKeywords) {
            if (rawText.contains(rule.pattern) || merchantText.contains(rule.pattern)) {
                return CarbonHintResult(
                    carbonHint = rule.hint,
                    transportMode = rule.transportMode,
                    fuelType = rule.fuelType,
                    energyType = rule.energyType,
                    purchaseType = rule.purchaseType,
                    confidence = 0.90,
                    matchedRule = "NOTIFICATION_KEYWORD",
                    reasoning = "Notification keyword '${rule.pattern}' matched ${rule.hint} registry."
                )
            }
        }

        // Priority 3: Merchant Type
        val mType = merchantTypeResult.merchantType.uppercase()
        val mtRule = merchantTypeMappings[mType]
        if (mtRule != null) {
            return CarbonHintResult(
                carbonHint = mtRule.hint,
                transportMode = mtRule.transportMode,
                fuelType = mtRule.fuelType,
                energyType = mtRule.energyType,
                purchaseType = mtRule.purchaseType,
                confidence = 0.85,
                matchedRule = "MERCHANT_TYPE",
                reasoning = "Merchant type '${mType}' mapped to ${mtRule.hint}."
            )
        }

        // Priority 4: Category
        val cat = categoryResult.category.uppercase()
        val catRule = categoryMappings[cat]
        if (catRule != null && cat != "UNKNOWN") {
            return CarbonHintResult(
                carbonHint = catRule.hint,
                transportMode = catRule.transportMode,
                fuelType = catRule.fuelType,
                energyType = catRule.energyType,
                purchaseType = catRule.purchaseType,
                confidence = 0.80,
                matchedRule = "CATEGORY",
                reasoning = "Category '${cat}' mapped to ${catRule.hint}."
            )
        }

        // Priority 5: Package Name
        for (rule in packageMappings) {
            if (packageName.contains(rule.pattern)) {
                return CarbonHintResult(
                    carbonHint = rule.hint,
                    transportMode = rule.transportMode,
                    fuelType = rule.fuelType,
                    energyType = rule.energyType,
                    purchaseType = rule.purchaseType,
                    confidence = 0.75,
                    matchedRule = "PACKAGE_NAME",
                    reasoning = "Package name '${rule.pattern}' mapped to ${rule.hint}."
                )
            }
        }

        // Priority 6: UNKNOWN
        return CarbonHintResult(
            carbonHint = "UNKNOWN",
            transportMode = null,
            fuelType = null,
            energyType = null,
            purchaseType = null,
            confidence = 0.0,
            matchedRule = "DEFAULT_FALLBACK",
            reasoning = "No rules matched, fallback to UNKNOWN."
        )
    }

    private data class HintRule(
        val pattern: String,
        val hint: String,
        val transportMode: String?,
        val fuelType: String?,
        val energyType: String?,
        val purchaseType: String?
    )
}
