package com.example.finessa.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.example.finessa.R
import com.example.finessa.databinding.FragmentSettingsBinding
import com.example.finessa.utils.CurrencyManager

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val currencies = listOf(
        "USD",
        "LKR",
        "EUR",
        "GBP",
        "INR"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCurrencySpinner()
    }

    private fun setupCurrencySpinner() {
        // Create adapter with currency symbols
        val currencyItems = currencies.map { currency ->
            when (currency) {
                "USD" -> getString(R.string.usd)
                "LKR" -> getString(R.string.lkr)
                "EUR" -> getString(R.string.eur)
                "GBP" -> getString(R.string.gbp)
                "INR" -> getString(R.string.inr)
                else -> currency
            }
        }

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_menu_item,
            currencyItems
        )
        binding.spinnerCurrency.adapter = adapter

        // Load saved currency
        val savedCurrency = CurrencyManager.getSelectedCurrency(requireContext())
        val position = currencies.indexOf(savedCurrency)
        if (position != -1) {
            binding.spinnerCurrency.setSelection(position)
        }

        binding.spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCurrency = currencies[position]
                // Save selected currency and notify observers
                CurrencyManager.setSelectedCurrency(requireContext(), selectedCurrency)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 