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
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter

class AnalysisFragment : Fragment() {
    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!

    private lateinit var categoryColors: List<Int>

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
        initializeColors()
        setupPieChart()
    }

    private fun initializeColors() {
        categoryColors = listOf(
            ContextCompat.getColor(requireContext(), R.color.colorPrimary),
            ContextCompat.getColor(requireContext(), R.color.colorAccent),
            ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark),
            ContextCompat.getColor(requireContext(), R.color.iconColor),
            ContextCompat.getColor(requireContext(), R.color.buttonColor)
        )
    }

    private fun setupPieChart() {
        try {
            // Clear any existing data
            binding.pieChart.clear()
            
            // Sample data - replace with your actual transaction data
            val entries = ArrayList<PieEntry>()
            entries.add(PieEntry(30f, "Food"))
            entries.add(PieEntry(20f, "Transport"))
            entries.add(PieEntry(15f, "Entertainment"))
            entries.add(PieEntry(25f, "Bills"))
            entries.add(PieEntry(10f, "Other"))

            if (entries.isEmpty()) {
                binding.pieChart.setNoDataText("No transaction data available")
                binding.pieChart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
                binding.tvTotalSpending.text = "Total Spending: $0.00"
                binding.tvLargestCategory.text = "Largest Category: -"
                return
            }

            val dataSet = PieDataSet(entries, "Spending Categories")
            dataSet.colors = categoryColors
            dataSet.valueTextSize = 12f
            dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.buttonText)

            val data = PieData(dataSet)
            data.setValueFormatter(PercentFormatter(binding.pieChart))
            binding.pieChart.data = data

            // Basic chart customization
            binding.pieChart.apply {
                description.isEnabled = false
                setHoleColor(Color.TRANSPARENT)
                setTransparentCircleAlpha(0)
                legend.isEnabled = true
                setEntryLabelColor(ContextCompat.getColor(requireContext(), R.color.textPrimary))
                setEntryLabelTextSize(12f)
                animateY(1000)
                setUsePercentValues(true)
                legend.textSize = 12f
                legend.formSize = 12f
                legend.textColor = ContextCompat.getColor(requireContext(), R.color.textPrimary)
                setHoleColor(ContextCompat.getColor(requireContext(), R.color.backgroundLight))
            }

            // Update text views with analysis data
            val totalSpending = entries.sumOf { it.value.toDouble() }
            binding.tvTotalSpending.text = "Total Spending: $${String.format("%.2f", totalSpending)}"
            
            val largestCategory = entries.maxByOrNull { it.value }
            binding.tvLargestCategory.text = "Largest Category: ${largestCategory?.label ?: "-"}"

            // Important: Refresh the chart
            binding.pieChart.invalidate()

        } catch (e: Exception) {
            e.printStackTrace()
            binding.pieChart.setNoDataText("Error loading chart data")
            binding.pieChart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}