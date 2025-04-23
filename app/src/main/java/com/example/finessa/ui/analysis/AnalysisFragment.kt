package com.example.finessa.ui.analysis

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.finessa.R
import com.example.finessa.databinding.FragmentAnalysisBinding
import com.example.finessa.model.Transaction
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AnalysisFragment : Fragment() {
    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!

    // Enhanced color palette that matches the app theme
    private val categoryColors = listOf(
        Color.parseColor("#6B1D28"),  // Primary Maroon
        Color.parseColor("#F4B400"),  // Primary Gold
        Color.parseColor("#4A1019"),  // Dark Maroon
        Color.parseColor("#FFD700"),  // Bright Gold
        Color.parseColor("#8B0000"),  // Dark Red
        Color.parseColor("#DAA520"),  // Goldenrod
        Color.parseColor("#800000"),  // Maroon
        Color.parseColor("#FFC107"),  // Amber
        Color.parseColor("#A52A2A"),  // Brown
        Color.parseColor("#FFB700")   // Warm Gold
    )

    private val gson = Gson()
    private val transactionType = object : TypeToken<List<Transaction>>() {}.type

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPieCharts()
    }

    override fun onResume() {
        super.onResume()
        updateCharts()
    }

    private fun setupPieCharts() {
        try {
            // Clear any existing data
            binding.pieChartExpenses.clear()
            binding.pieChartIncome.clear()
            
            // Get actual transaction data from SharedPreferences
            val sharedPreferences = requireContext().getSharedPreferences("finance_tracker", 0)
            val transactionsJson = sharedPreferences.getString("transactions", null)
            val transactions = if (transactionsJson != null) {
                gson.fromJson<List<Transaction>>(transactionsJson, transactionType)
            } else {
                emptyList()
            }

            // Process transactions into pie chart entries
            val expenseEntries = ArrayList<PieEntry>()
            val incomeEntries = ArrayList<PieEntry>()

            // Group transactions by category and sum their amounts
            val expenseMap = mutableMapOf<String, Float>()
            val incomeMap = mutableMapOf<String, Float>()

            transactions.forEach { transaction ->
                if (transaction.isIncome) {
                    incomeMap[transaction.category] = (incomeMap[transaction.category] ?: 0f) + transaction.amount.toFloat()
                } else {
                    expenseMap[transaction.category] = (expenseMap[transaction.category] ?: 0f) + transaction.amount.toFloat()
                }
            }

            // Convert maps to pie entries
            expenseMap.forEach { (category, amount) ->
                expenseEntries.add(PieEntry(amount, category))
            }

            incomeMap.forEach { (category, amount) ->
                incomeEntries.add(PieEntry(amount, category))
            }

            // Setup Expenses Chart
            if (expenseEntries.isEmpty()) {
                binding.pieChartExpenses.setNoDataText("No expense data available")
                binding.pieChartExpenses.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
            } else {
                setupPieChart(
                    binding.pieChartExpenses,
                    expenseEntries,
                    "Expenses by Category"
                )
            }

            // Setup Income Chart
            if (incomeEntries.isEmpty()) {
                binding.pieChartIncome.setNoDataText("No income data available")
                binding.pieChartIncome.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
            } else {
                setupPieChart(
                    binding.pieChartIncome,
                    incomeEntries,
                    "Income by Category"
                )
            }

            // Update text views with analysis data
            updateSummaryTexts(expenseEntries, incomeEntries)

        } catch (e: Exception) {
            e.printStackTrace()
            binding.pieChartExpenses.setNoDataText("Error loading chart data")
            binding.pieChartExpenses.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
            binding.pieChartIncome.setNoDataText("Error loading chart data")
            binding.pieChartIncome.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
        }
    }

    private fun updateCharts() {
        setupPieCharts()
    }

    private fun updateSummaryTexts(expenseEntries: ArrayList<PieEntry>, incomeEntries: ArrayList<PieEntry>) {
        val totalExpenses = expenseEntries.sumOf { it.value.toDouble() }
        val totalIncome = incomeEntries.sumOf { it.value.toDouble() }
        
        binding.tvTotalSpending.text = "Total Expenses: $${String.format("%.2f", totalExpenses)}"
        binding.tvTotalIncome.text = "Total Income: $${String.format("%.2f", totalIncome)}"
        
        val largestExpenseCategory = expenseEntries.maxByOrNull { it.value }
        binding.tvLargestCategory.text = "Largest Category: ${largestExpenseCategory?.label ?: "-"}"
    }

    private fun setupPieChart(chart: com.github.mikephil.charting.charts.PieChart, entries: ArrayList<PieEntry>, label: String) {
        val dataSet = PieDataSet(entries, label)
        dataSet.colors = categoryColors.take(entries.size)
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.buttonText)
        dataSet.valueLineColor = ContextCompat.getColor(requireContext(), R.color.textSecondary)
        dataSet.valueLinePart1Length = 0.4f
        dataSet.valueLinePart2Length = 0.4f
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(chart))
        chart.data = data

        // Enhanced chart customization
        chart.apply {
            description.isEnabled = false
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleAlpha(0)
            legend.isEnabled = true
            setEntryLabelColor(ContextCompat.getColor(requireContext(), R.color.textPrimary))
            setEntryLabelTextSize(14f)
            animateY(1200, Easing.EaseInOutQuad)
            setUsePercentValues(true)
            legend.textSize = 14f
            legend.formSize = 14f
            legend.textColor = ContextCompat.getColor(requireContext(), R.color.textPrimary)
            setHoleColor(ContextCompat.getColor(requireContext(), R.color.backgroundLight))
            
            // Improve legend appearance
            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)
            legend.xEntrySpace = 15f
            legend.yEntrySpace = 8f
            
            // Add rotation
            rotationAngle = 0f
            isRotationEnabled = true
            
            // Add highlight
            setDrawEntryLabels(true)
            setUsePercentValues(true)
            setDrawHoleEnabled(true)
            setHoleColor(Color.WHITE)
            setTransparentCircleRadius(55f)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
        }

        chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}