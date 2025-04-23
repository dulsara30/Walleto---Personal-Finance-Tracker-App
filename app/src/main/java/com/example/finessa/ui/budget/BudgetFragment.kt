package com.example.finessa.ui.budget

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finessa.R
import com.example.finessa.adapter.CategoryBudgetAdapter
import com.example.finessa.databinding.FragmentBudgetBinding
import com.example.finessa.model.CategoryBudget
import com.example.finessa.utils.CurrencyManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BudgetFragment : Fragment() {
    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private lateinit var etBudget: TextInputEditText
    private lateinit var btnSaveBudget: MaterialButton
    private lateinit var rvCategoryBudgets: RecyclerView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var budgetAdapter: CategoryBudgetAdapter
    private val gson = Gson()
    private val budgetType = object : TypeToken<List<CategoryBudget>>() {}.type

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        etBudget = binding.etBudget
        btnSaveBudget = binding.btnSaveBudget
        rvCategoryBudgets = binding.rvCategoryBudgets

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("finance_tracker", 0)

        // Setup RecyclerView
        budgetAdapter = CategoryBudgetAdapter(requireContext())
        rvCategoryBudgets.layoutManager = LinearLayoutManager(context)
        rvCategoryBudgets.adapter = budgetAdapter

        // Load saved data
        loadSavedData()

        // Setup save button
        btnSaveBudget.setOnClickListener {
            val budget = etBudget.text.toString().toDoubleOrNull()
            if (budget == null) {
                Toast.makeText(context, "Please enter a valid budget amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveMonthlyBudget(budget)
            Toast.makeText(context, "Budget saved successfully", Toast.LENGTH_SHORT).show()
        }

        observeCurrencyChanges()
    }

    private fun loadSavedData() {
        // Load monthly budget
        val monthlyBudget = sharedPreferences.getFloat("monthly_budget", 0f)
        if (monthlyBudget > 0) {
            etBudget.setText(monthlyBudget.toString())
        }

        // Load category budgets
        val json = sharedPreferences.getString("category_budgets", null)
        val budgets = if (json != null) {
            gson.fromJson<List<CategoryBudget>>(json, budgetType)
        } else {
            emptyList()
        }
        budgetAdapter.submitList(budgets)
        updateAmounts()
    }

    private fun saveMonthlyBudget(budget: Double) {
        sharedPreferences.edit().putFloat("monthly_budget", budget.toFloat()).apply()
    }

    private fun saveCategoryBudget(category: String, budget: Double) {
        val json = sharedPreferences.getString("category_budgets", null)
        val budgets = if (json != null) {
            gson.fromJson<MutableList<CategoryBudget>>(json, budgetType)
        } else {
            mutableListOf()
        }

        val existingBudget = budgets.find { it.category == category }
        if (existingBudget != null) {
            existingBudget.budget = budget
        } else {
            budgets.add(CategoryBudget(category, budget))
        }

        sharedPreferences.edit()
            .putString("category_budgets", gson.toJson(budgets))
            .apply()

        budgetAdapter.submitList(budgets)
        updateAmounts()
    }

    private fun observeCurrencyChanges() {
        CurrencyManager.currencyChanged.observe(viewLifecycleOwner, Observer { _ ->
            updateAmounts()
            budgetAdapter.notifyDataSetChanged()
        })
    }

    private fun updateAmounts() {
        val budgetsJson = sharedPreferences.getString("category_budgets", null)
        val budgets = if (budgetsJson != null) {
            gson.fromJson<List<CategoryBudget>>(budgetsJson, budgetType)
        } else {
            emptyList()
        }

        val totalBudget = budgets.sumOf { it.budget }
        binding.tvTotalBudget.text = CurrencyManager.formatAmount(requireContext(), totalBudget)
    }

    override fun onResume() {
        super.onResume()
        loadSavedData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}