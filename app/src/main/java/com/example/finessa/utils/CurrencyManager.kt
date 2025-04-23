package com.example.finessa.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object CurrencyManager {
    private const val PREF_NAME = "finance_tracker"
    private const val KEY_CURRENCY = "selected_currency"
    
    private val _currencyChanged = MutableLiveData<String>()
    val currencyChanged: LiveData<String> = _currencyChanged

    fun getSelectedCurrency(context: Context): String {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_CURRENCY, "USD") ?: "USD"
    }

    fun setSelectedCurrency(context: Context, currency: String) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(KEY_CURRENCY, currency).apply()
        _currencyChanged.postValue(currency)
    }

    fun formatAmount(context: Context, amount: Double): String {
        return CurrencyUtils.formatAmount(context, amount)
    }

    fun formatAmountWithSymbol(context: Context, amount: Double): String {
        return CurrencyUtils.formatAmountWithSymbol(context, amount)
    }
} 