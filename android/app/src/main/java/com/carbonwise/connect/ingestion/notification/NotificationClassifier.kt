package com.carbonwise.connect.ingestion.notification

import com.carbonwise.connect.ingestion.model.ClassificationResult
import com.carbonwise.connect.ingestion.model.EventCategory
import com.carbonwise.connect.ingestion.model.NotificationEvent
import com.carbonwise.connect.ingestion.pipeline.DataClassifier
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Determines the event category and confidence score for a notification.
 * Single responsibility: classify notifications into carbon-relevant categories.
 */
@Singleton
class NotificationClassifier @Inject constructor() : DataClassifier<NotificationEvent> {

    private val categoryKeywords = mapOf(
        EventCategory.TRANSPORT_RIDE to listOf(
            "uber", "lyft", "ride", "taxi", "cab", "trip", "driver",
            "pickup", "dropoff", "destination", "ride share", "carpool"
        ),
        EventCategory.TRANSPORT_DELIVERY to listOf(
            "delivery", "delivered", "shipped", "package", "parcel",
            "fedex", "ups", "usps", "dhl", "amazon logistics",
            "out for delivery", "in transit"
        ),
        EventCategory.FOOD_DELIVERY to listOf(
            "doordash", "grubhub", "uber eats", "postmates", "instacart",
            "food delivery", "order confirmed", "on the way", "arriving soon",
            "your order", "dasher", "courier"
        ),
        EventCategory.FOOD_GROCERY to listOf(
            "grocery", "walmart", "kroger", "safeway", "whole foods",
            "trader joe", "aldi", "costco", "sam's club", "instacart",
            "groceries", "shopping list"
        ),
        EventCategory.ENERGY_BILL to listOf(
            "electric", "energy", "utility", "power", "gas bill",
            "water bill", "electricity", "kwh", "meter reading"
        ),
        EventCategory.SHOPPING_ONLINE to listOf(
            "order", "purchase", "checkout", "confirmation", "receipt",
            "your order has been", "thank you for", "order placed",
            "payment confirmed", "shipping update"
        ),
        EventCategory.SHOPPING_IN_STORE to listOf(
            "receipt", "transaction", "purchase", "total", "change due",
            "loyalty", "reward points", "store"
        ),
        EventCategory.SUBSCRIPTION to listOf(
            "subscription", "renewal", "renewed", "monthly", "annual",
            "your subscription", "plan", "billing", "charged"
        ),
        EventCategory.TRAVEL_BOOKING to listOf(
            "booking", "reservation", "flight", "hotel", "airbnb",
            "confirmation number", "itinerary", "boarding pass",
            "check-in", "departure"
        )
    )

    override fun classify(data: NotificationEvent): ClassificationResult {
        val text = buildString {
            append(data.rawData.title)
            append(" ")
            append(data.rawData.body)
            append(" ")
            append(data.category.orEmpty())
        }.lowercase()

        var bestCategory = EventCategory.UNKNOWN
        var bestScore = 0f

        for ((category, keywords) in categoryKeywords) {
            val matchCount = keywords.count { keyword ->
                text.contains(keyword.lowercase())
            }

            if (matchCount > 0) {
                val score = calculateConfidence(matchCount, keywords.size)
                if (score > bestScore) {
                    bestScore = score
                    bestCategory = category
                }
            }
        }

        // Package-based boosting
        val packageBoost = getPackageBoost(data.packageName)
        if (packageBoost.category != EventCategory.UNKNOWN) {
            val boostedScore = (bestScore + packageBoost.score).coerceAtMost(1f)
            if (boostedScore > bestScore) {
                bestCategory = packageBoost.category
                bestScore = boostedScore
            }
        }

        val labels = mutableListOf<String>()
        if (bestCategory != EventCategory.UNKNOWN) {
            labels.add(bestCategory.name.lowercase())
        }

        return ClassificationResult(
            category = bestCategory,
            confidence = bestScore,
            labels = labels
        )
    }

    private fun calculateConfidence(matchCount: Int, totalKeywords: Int): Float {
        if (totalKeywords == 0) return 0f
        val base = matchCount.toFloat() / totalKeywords
        return (base * 0.7f + 0.3f).coerceIn(0f, 1f)
    }

    private fun getPackageBoost(packageName: String): PackageBoost {
        return when {
            packageName.contains("uber") && !packageName.contains("eats") ->
                PackageBoost(EventCategory.TRANSPORT_RIDE, 0.4f)
            packageName.contains("lyft") ->
                PackageBoost(EventCategory.TRANSPORT_RIDE, 0.4f)
            packageName.contains("doordash") || packageName.contains("grubhub") ->
                PackageBoost(EventCategory.FOOD_DELIVERY, 0.4f)
            packageName.contains("amazon") ->
                PackageBoost(EventCategory.SHOPPING_ONLINE, 0.3f)
            packageName.contains("instacart") ->
                PackageBoost(EventCategory.FOOD_GROCERY, 0.4f)
            packageName.contains("airbnb") ->
                PackageBoost(EventCategory.TRAVEL_BOOKING, 0.4f)
            else -> PackageBoost(EventCategory.UNKNOWN, 0f)
        }
    }

    private data class PackageBoost(
        val category: EventCategory,
        val score: Float
    )
}
