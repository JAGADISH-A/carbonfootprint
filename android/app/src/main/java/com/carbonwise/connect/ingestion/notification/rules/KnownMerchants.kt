package com.carbonwise.connect.ingestion.notification.rules

/**
 * Centralized list of known merchants and brands associated with carbon-relevant transactions.
 *
 * When a notification mentions one of these merchants, the classifier can use this
 * as a strong signal for categorization (e.g., Uber → transport, DoorDash → food delivery).
 *
 * # Adding a new merchant
 *
 * 1. Add the merchant entry to [values].
 * 2. Specify the merchant name (lowercase).
 * 3. Specify the primary [MerchantCategory] the merchant belongs to.
 * 4. Include aliases for common variations (e.g., "uber eats", "ubereats").
 *
 * # Notes
 *
 * - Merchant names are lowercase and matched case-insensitively.
 * - Partial matching is supported; "uber" matches "Uber Ride" and "Uber Eats".
 * - Use the most recognizable form of the merchant name as the primary key.
 */
object KnownMerchants {

    /**
     * Immutable set of known merchant entries.
     *
     * When adding new merchants, append to the appropriate category list.
     */
    val values: Set<MerchantEntry> = setOf(
        // Ride-hailing / transport
        MerchantEntry("uber", MerchantCategory.TRANSPORT_RIDE, setOf("uber ride", "uber driver")),
        MerchantEntry("lyft", MerchantCategory.TRANSPORT_RIDE, setOf("lyft ride")),
        MerchantEntry("grab", MerchantCategory.TRANSPORT_RIDE, setOf("grabcar", "grab ride")),
        MerchantEntry("bolt", MerchantCategory.TRANSPORT_RIDE, setOf("bolt ride")),
        MerchantEntry("via", MerchantCategory.TRANSPORT_RIDE, setOf("via transit")),

        // Food delivery
        MerchantEntry("doordash", MerchantCategory.FOOD_DELIVERY, setOf("door dash", "dasher")),
        MerchantEntry("grubhub", MerchantCategory.FOOD_DELIVERY, setOf("grub hub")),
        MerchantEntry("uber eats", MerchantCategory.FOOD_DELIVERY, setOf("ubereats", "uber eat")),
        MerchantEntry("postmates", MerchantCategory.FOOD_DELIVERY, setOf("post mate")),
        MerchantEntry("seamless", MerchantCategory.FOOD_DELIVERY, setOf()),
        MerchantEntry("deliveroo", MerchantCategory.FOOD_DELIVERY, setOf("deliver roo")),
        MerchantEntry("just eat", MerchantCategory.FOOD_DELIVERY, setOf("justeat", "just eat takeaway")),

        // Grocery
        MerchantEntry("instacart", MerchantCategory.FOOD_GROCERY, setOf("insta cart")),
        MerchantEntry("whole foods", MerchantCategory.FOOD_GROCERY, setOf("wholefoods", "whole food")),
        MerchantEntry("trader joe", MerchantCategory.FOOD_GROCERY, setOf("trader joes")),
        MerchantEntry("aldi", MerchantCategory.FOOD_GROCERY, setOf()),
        MerchantEntry("kroger", MerchantCategory.FOOD_GROCERY, setOf()),
        MerchantEntry("safeway", MerchantCategory.FOOD_GROCERY, setOf()),
        MerchantEntry("walmart grocery", MerchantCategory.FOOD_GROCERY, setOf("walmart pickup", "walmart delivery")),

        // E-commerce / shipping
        MerchantEntry("amazon", MerchantCategory.SHOPPING_ONLINE, setOf("amazon order", "amazon delivery", "amazon fresh")),
        MerchantEntry("fedex", MerchantCategory.TRANSPORT_DELIVERY, setOf("fed ex", "fedex delivery")),
        MerchantEntry("ups", MerchantCategory.TRANSPORT_DELIVERY, setOf("ups delivery", "ups my choice")),
        MerchantEntry("usps", MerchantCategory.TRANSPORT_DELIVERY, setOf("us postal", "postal service")),
        MerchantEntry("dhl", MerchantCategory.TRANSPORT_DELIVERY, setOf("dhl express", "dhl delivery")),

        // Energy / utilities
        MerchantEntry("pg&e", MerchantCategory.ENERGY_BILL, setOf("pacific gas", "pg and e")),
        MerchantEntry("con edison", MerchantCategory.ENERGY_BILL, setOf("coned", "con ed")),
        MerchantEntry("duke energy", MerchantCategory.ENERGY_BILL, setOf("duke")),
        MerchantEntry("southern california edison", MerchantCategory.ENERGY_BILL, setOf("sce", "edison")),

        // Travel
        MerchantEntry("airbnb", MerchantCategory.TRAVEL_BOOKING, setOf("air bnb", "air b&b")),
        MerchantEntry("booking.com", MerchantCategory.TRAVEL_BOOKING, setOf("booking com")),
        MerchantEntry("expedia", MerchantCategory.TRAVEL_BOOKING, setOf()),
        MerchantEntry("hotels.com", MerchantCategory.TRAVEL_BOOKING, setOf("hotels com")),
        MerchantEntry("hilton", MerchantCategory.TRAVEL_BOOKING, setOf("hilton hotels")),
        MerchantEntry("marriott", MerchantCategory.TRAVEL_BOOKING, setOf("marriott bonvoy")),
    )
}

/**
 * Represents a known merchant with its primary category and alternative name variations.
 *
 * @property name Primary merchant name (lowercase, used for display and matching)
 * @property category Primary carbon-relevant category for this merchant
 * @property aliases Alternative names or variations for matching
 */
data class MerchantEntry(
    val name: String,
    val category: MerchantCategory,
    val aliases: Set<String> = emptySet()
) {
    /**
     * All searchable names including the primary name and aliases.
     */
    val allNames: Set<String>
        get() = aliases + name
}

/**
 * Categories that known merchants can belong to.
 *
 * Maps directly to [EventCategory] but is kept separate to avoid coupling
 * merchant configuration to the classifier implementation.
 */
enum class MerchantCategory {
    TRANSPORT_RIDE,
    TRANSPORT_DELIVERY,
    FOOD_DELIVERY,
    FOOD_GROCERY,
    ENERGY_BILL,
    SHOPPING_ONLINE,
    SHOPPING_IN_STORE,
    SUBSCRIPTION,
    TRAVEL_BOOKING,
    UNKNOWN
}
