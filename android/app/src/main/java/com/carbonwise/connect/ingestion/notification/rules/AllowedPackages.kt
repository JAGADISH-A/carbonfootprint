package com.carbonwise.connect.ingestion.notification.rules

/**
 * Centralized allowlist of package names considered relevant for carbon tracking.
 *
 * Notifications from these packages will pass the package filter stage.
 * If this list is non-empty, ONLY packages in this list are accepted (allowlist mode).
 * If this list is empty, allowlist mode is disabled and blocklist mode applies instead.
 *
 * # Adding a new allowed package
 *
 * 1. Add the Android package name (e.g., "com.ubercab") to [values].
 * 2. Include a short rationale in the comment for traceability.
 * 3. Verify the package actually produces carbon-relevant notifications.
 *
 * # Notes
 *
 * - Package names are case-sensitive.
 * - Use the exact package name from `StatusBarNotification.packageName`.
 * - Subpackages are not matched; "com.uber" does NOT match "com.uber.cab".
 */
object AllowedPackages {

    /**
     * Immutable set of allowed package names.
     *
     * Sorted alphabetically for readability. To add a new entry, append to this list
     * and maintain alphabetical order.
     */
    val values: Set<String> = setOf(
        // Ride-hailing
        "com.ubercab",                  // Uber rides
        "com.lyft.android",             // Lyft rides
        "com.grabtaxi.driver",          // Grab (Southeast Asia)

        // Food delivery
        "com.doordash.driverapp",       // DoorDash
        "com.grubhub.driver",           // Grubhub
        "com.ubercab.eats",             // Uber Eats
        "com.postmates.android",        // Postmates

        // Grocery delivery
        "com.instacart.client",         // Instacart

        // E-commerce / shipping
        "com.amazon.mShop.android.shopping", // Amazon Shopping
        "com.amazon.delivery",               // Amazon Delivery
        "com FedEx.mobile",                  // FedEx
        "com.ups.android",                   // UPS

        // Energy / utilities
        // Add utility app packages here as identified

        // Travel
        "com.airbnb.android",           // Airbnb
    )
}
