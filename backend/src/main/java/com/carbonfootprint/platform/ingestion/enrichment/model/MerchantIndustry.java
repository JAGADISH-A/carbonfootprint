package com.carbonfootprint.platform.ingestion.enrichment.model;

/**
 * Merchant industry segment inferred for {@link CarbonActivityType#SHOPPING} activities.
 *
 * <p>Different retail industries have vastly different carbon footprints per unit
 * of spend. Classifying the merchant industry enables more accurate emission factors.
 *
 * <p>Serialization: {@link #name()} is stored in {@code metadata.carbonHints.merchantIndustry}.
 */
public enum MerchantIndustry {

    /** Grocery stores, supermarkets, quick-commerce (BigBasket, DMart, Blinkit). */
    GROCERY,

    /** Restaurants, cafes, food delivery (Zomato, Swiggy, McDonald's). */
    RESTAURANT,

    /** Clothing and fashion retail (Myntra, Zara, H&amp;M). */
    CLOTHING,

    /** Consumer electronics retail (Croma, Vijay Sales, Reliance Digital). */
    ELECTRONICS,

    /** Pharmacies, chemists, medical stores (Apollo, MedPlus, 1mg). */
    PHARMACY,

    /** Furniture and home decor (IKEA, Urban Ladder, Pepperfry). */
    FURNITURE
}
