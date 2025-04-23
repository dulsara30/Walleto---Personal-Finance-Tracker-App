package com.example.finessa.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finessa.databinding.ItemCategoryBudgetBinding
import com.example.finessa.model.CategoryBudget
import com.example.finessa.utils.CurrencyManager

class CategoryBudgetAdapter(
    private val context: android.content.Context
) : ListAdapter<CategoryBudget, CategoryBudgetAdapter.BudgetViewHolder>(BudgetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val binding = ItemCategoryBudgetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BudgetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BudgetViewHolder(
        private val binding: ItemCategoryBudgetBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(budget: CategoryBudget) {
            binding.apply {
                tvCategory.text = budget.category
                etBudget.setText(CurrencyManager.formatAmount(context, budget.budget))
                
                val progress = if (budget.budget > 0) {
                    (budget.spent / budget.budget * 100).toInt()
                } else {
                    0
                }
                progressBar.progress = progress
                tvProgress.text = "$progress%"
            }
        }
    }

    private class BudgetDiffCallback : DiffUtil.ItemCallback<CategoryBudget>() {
        override fun areItemsTheSame(oldItem: CategoryBudget, newItem: CategoryBudget): Boolean {
            return oldItem.category == newItem.category
        }

        override fun areContentsTheSame(oldItem: CategoryBudget, newItem: CategoryBudget): Boolean {
            return oldItem == newItem
        }
    }
} 