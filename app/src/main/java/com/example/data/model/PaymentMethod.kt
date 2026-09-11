package com.example.data.model

data class PaymentMethod(
    val id: String = "",
    val name: String = "",
    val category: String = "eSewa", // Only: "eSewa", "FF Diamond", "Robux", "PUBG"
    val amountType: String = "FIXED", // "FIXED" or "RANGE" (Min/Max)
    val fixedCoins: Int = 1000,       // Fixed coins required (if amountType == FIXED)
    val minCoins: Int = 1000,         // Minimum coins required (if amountType == RANGE)
    val maxCoins: Int = 10000,        // Maximum coins allowed (if amountType == RANGE)
    val minimumAmount: Int = 10,      // Cash / item value (e.g. 100 NPR or 60 UC)
    val maximumAmount: Int = 100,     // Maximum cash / item value
    val isActive: Boolean = true,
    val type: String = "wallet",      // "wallet" or "game"
    val icon: String = "esewa",       // "esewa", "fire", "robux", "pubg"
    val placeholder: String = "Enter account details", // User hint
    val description: String = "",     // Short description
    val badge: String = ""            // "HOT", "POPULAR", "INSTANT", etc.
) {
    fun isFixed(): Boolean = amountType.equals("FIXED", ignoreCase = true) || (fixedCoins > 0 && minCoins <= 0)

    fun getEffectiveCoins(rate: Int): Int {
        val rateVal = if (rate > 0) rate else 100
        return if (isFixed()) {
            if (fixedCoins > 0) fixedCoins else (minimumAmount * rateVal)
        } else {
            if (minCoins > 0) minCoins else (minimumAmount * rateVal)
        }
    }

    fun getMinCoinsAllowed(rate: Int): Int {
        val rateVal = if (rate > 0) rate else 100
        return if (minCoins > 0) minCoins else (if (fixedCoins > 0) fixedCoins else minimumAmount * rateVal)
    }

    fun getMaxCoinsAllowed(rate: Int): Int {
        val min = getMinCoinsAllowed(rate)
        return if (maxCoins > min) maxCoins else (min * 5)
    }
}

