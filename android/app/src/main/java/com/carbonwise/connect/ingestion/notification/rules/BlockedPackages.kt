package com.carbonwise.connect.ingestion.notification.rules

/**
 * Centralized blocklist of package names that should be ignored.
 *
 * Notifications from these packages will always be rejected during filtering,
 * regardless of other rules or allowlist settings.
 *
 * # Adding a new blocked package
 *
 * 1. Add the Android package name to [values].
 * 2. Include a short rationale in the comment explaining why it's blocked.
 *
 * # Notes
 *
 * - Package names are case-sensitive.
 * - Blocklist is checked BEFORE allowlist; a blocked package is always rejected.
 * - Subpackages are not matched; "com.android" does NOT match "com.android.chrome".
 */
object BlockedPackages {

    /**
     * Immutable set of blocked package names.
     *
     * Sorted alphabetically for readability.
     */
    val values: Set<String> = setOf(
        // System UI
        "android.systemui",             // Android System UI
        "com.android.systemui",         // AOSP System UI

        // Messaging / chat (no carbon signal)
        "com.android.mms",              // SMS app
        "com.google.android.apps.messaging", // Google Messages

        // Social media (no carbon signal)
        "com.facebook.katana",          // Facebook
        "com.instagram.android",        // Instagram
        "com.twitter.android",          // Twitter/X
        "com.zhiliaoapp.musically",     // TikTok

        // Media / entertainment (no carbon signal)
        "com.spotify.music",            // Spotify
        "com.netflix.mediaclient",      // Netflix
        "com.google.android.youtube",   // YouTube

        // Productivity (no carbon signal)
        "com.google.android.gm",        // Gmail
        "com.google.android.calendar",  // Google Calendar
        "com.microsoft.office.outlook", // Outlook

        // Gaming
        "com.supercell.clashofclans",   // Clash of Clans
        "com.king.candycrushsaga",      // Candy Crush
    )
}
