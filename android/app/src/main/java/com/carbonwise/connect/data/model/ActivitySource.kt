package com.carbonwise.connect.data.model

enum class ActivitySource {
    SMS,
    NOTIFICATION,
    EMAIL,
    RECEIPT,
    MANUAL;

    companion object {
        fun fromString(value: String): ActivitySource {
            return try {
                ActivitySource.valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                SMS // Default fallback
            }
        }
    }
}
