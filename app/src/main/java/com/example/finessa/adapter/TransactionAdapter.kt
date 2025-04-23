package com.example.finessa.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finessa.databinding.TransactionItemBinding
import com.example.finessa.model.Transaction
import com.example.finessa.utils.CurrencyManager
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private val context: android.content.Context,
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = TransactionItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: TransactionItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.apply {
                tvTitle.text = transaction.title
                tvCategory.text = transaction.category
                tvAmount.text = CurrencyManager.formatAmount(context, transaction.amount)
                tvAmount.setTextColor(
                    context.getColor(
                        if (transaction.isIncome) android.R.color.holo_green_dark
                        else android.R.color.holo_red_dark
                    )
                )

                // Set up click listeners
                root.setOnClickListener { onEditClick(transaction) }
                btnDelete.setOnClickListener { onDeleteClick(transaction) }
            }
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}