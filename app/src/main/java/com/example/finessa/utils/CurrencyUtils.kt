package com.example.finessa.utils

import android.content.Context
import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private const val PREF_NAME = "finance_tracker"
    private const val KEY_CURRENCY = "selected_currency"

    // Currency symbols and locales
    private val currencyMap = mapOf(
        "USD" to Pair("$", Locale.US),
        "LKR" to Pair("Rs", Locale("si", "LK")),
        "EUR" to Pair("€", Locale.GERMANY),
        "GBP" to Pair("£", Locale.UK),
        "INR" to Pair("₹", Locale("en", "IN"))
    )

    fun getSelectedCurrency(context: Context): String {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_CURRENCY, "USD") ?: "USD"
    }

    fun formatAmount(context: Context, amount: Double): String {
        val currency = getSelectedCurrency(context)
        val (symbol, locale) = currencyMap[currency] ?: currencyMap["USD"]!!
        
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = java.util.Currency.getInstance(currency)
        
        return formatter.format(amount)
    }

    fun getCurrencySymbol(context: Context): String {
        val currency = getSelectedCurrency(context)
        return currencyMap[currency]?.first ?: "$"
    }

    fun convertAmount(amount: Double, fromCurrency: String, toCurrency: String): Double {
        // This is a simplified conversion. In a real app, you would use an API to get real-time exchange rates
        val exchangeRates = mapOf(
            "USD" to 1.0,
            "LKR" to 322.0,
            "EUR" to 0.92,
            "GBP" to 0.79,
            "INR" to 83.0
        )

        val fromRate = exchangeRates[fromCurrency] ?: 1.0
        val toRate = exchangeRates[toCurrency] ?: 1.0

        // Convert to USD first, then to target currency
        val amountInUSD = amount / fromRate
        return amountInUSD * toRate
    }

    fun formatAmountWithSymbol(context: Context, amount: Double): String {
        val currency = getSelectedCurrency(context)
        val (symbol, locale) = currencyMap[currency] ?: currencyMap["USD"]!!
        
        val formatter = NumberFormat.getNumberInstance(locale)
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 2
        
        return "$symbol${formatter.format(amount)}"
    }
} 