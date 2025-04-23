package com.example.finessa.ui.main

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finessa.R
import com.example.finessa.adapter.TransactionAdapter
import com.example.finessa.databinding.FragmentHomeBinding
import com.example.finessa.model.Transaction
import com.example.finessa.ui.dialog.TransactionDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.util.Date
import androidx.navigation.fragment.findNavController
import com.example.finessa.utils.CurrencyManager

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TransactionAdapter
    private lateinit var tvBalance: TextView
    private lateinit var tvBudgetStatus: TextView
    private lateinit var progressBudget: LinearProgressIndicator
    private lateinit var cardBudgetStatus: MaterialCardView
    private lateinit var rvTransactions: RecyclerView
    private lateinit var btnAddIncome: MaterialButton
    private lateinit var btnAddExpense: MaterialButton
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private val transactionType = object : TypeToken<List<Transaction>>() {}.type
    private val CHANNEL_ID = "budget_notification_channel"
    private val NOTIFICATION_ID = 1

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            showSnackbar("Notifications are disabled. You won't receive budget alerts.")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createNotificationChannel()
        checkNotificationPermission()

        // Initialize views
        tvBalance = binding.tvBalance
        tvBudgetStatus = binding.tvBudgetStatus
        progressBudget = binding.progressBudget
        cardBudgetStatus = binding.cardBudgetStatus
        rvTransactions = binding.rvTransactions
        btnAddIncome = binding.btnAddIncome
        btnAddExpense = binding.btnAddExpense

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("finance_tracker", 0)

        // Setup RecyclerView
        adapter = TransactionAdapter(
            context = requireContext(),
            onEditClick = { transaction ->
                showTransactionDialog(isIncome = transaction.isIncome, existingTransaction = transaction)
            },
            onDeleteClick = { transaction ->
                showDeleteConfirmationDialog(transaction)
            }
        )
        rvTransactions.layoutManager = LinearLayoutManager(context)
        rvTransactions.adapter = adapter

        // Setup buttons
        btnAddIncome.setOnClickListener { showTransactionDialog(isIncome = true) }
        btnAddExpense.setOnClickListener { showTransactionDialog(isIncome = false) }

        // Load saved data
        loadSavedData()
        updateBudgetStatus()
        observeCurrencyChanges()
    }

    private fun showTransactionDialog(isIncome: Boolean, existingTransaction: Transaction? = null) {
        TransactionDialogFragment(
            isIncome = isIncome,
            onSave = { transaction ->
                val transactions = adapter.currentList.toMutableList()
                
                if (!isIncome) {
                    if (!validateBudgets(transaction, transactions)) {
                        return@TransactionDialogFragment
                    }
                }

                if (existingTransaction != null) {
                    val index = transactions.indexOfFirst { it.id == existingTransaction.id }
                    if (index != -1) {
                        transactions[index] = transaction
                        showSnackbar("Transaction updated successfully")
                    }
                } else {
                    transactions.add(0, transaction)
                    showSnackbar("Transaction added successfully")
                }
                adapter.submitList(transactions)
                updateBalance(transactions)
                updateBudgetStatus()
                saveData(transactions)
            },
            existingTransaction = existingTransaction
        ).show(childFragmentManager, "transaction_dialog")
    }

    private fun validateBudgets(newTransaction: Transaction, transactions: List<Transaction>): Boolean {
        // Monthly Budget Validation
        val monthlyBudget = sharedPreferences.getFloat("monthly_budget", 0f)
        if (monthlyBudget > 0) {
            val currentMonthExpenses = getCurrentMonthExpenses(transactions)
            val newTotal = currentMonthExpenses + newTransaction.amount
            if (newTotal > monthlyBudget) {
                showBudgetLimitBlockedNotification(newTotal, monthlyBudget.toDouble(), "Monthly")
                return false
            } else if (newTotal > monthlyBudget * 0.8) {
                showApproachingBudgetNotification(newTotal, monthlyBudget.toDouble(), "Monthly", ((newTotal / monthlyBudget) * 100).toInt())
            }
        }

        // Weekly Budget Validation
        val weeklyBudget = sharedPreferences.getFloat("weekly_budget", 0f)
        if (weeklyBudget > 0) {
            val currentWeekExpenses = getCurrentWeekExpenses(transactions)
            val newWeeklyTotal = currentWeekExpenses + newTransaction.amount
            if (newWeeklyTotal > weeklyBudget) {
                showBudgetLimitBlockedNotification(newWeeklyTotal, weeklyBudget.toDouble(), "Weekly")
                return false
            } else if (newWeeklyTotal > weeklyBudget * 0.8) {
                showApproachingBudgetNotification(newWeeklyTotal, weeklyBudget.toDouble(), "Weekly", ((newWeeklyTotal / weeklyBudget) * 100).toInt())
            }
        }

        // Category Budget Validation
        val categoryBudgetJson = sharedPreferences.getString("category_budgets", null)
        if (categoryBudgetJson != null) {
            val categoryBudgets = gson.fromJson<Map<String, Double>>(categoryBudgetJson, object : TypeToken<Map<String, Double>>() {}.type)
            val categoryBudget = categoryBudgets[newTransaction.category]
            if (categoryBudget != null) {
                val currentCategoryExpenses = getCurrentMonthCategoryExpenses(transactions, newTransaction.category)
                val newCategoryTotal = currentCategoryExpenses + newTransaction.amount
                if (newCategoryTotal > categoryBudget) {
                    showBudgetLimitBlockedNotification(newCategoryTotal, categoryBudget, newTransaction.category)
                    return false
                } else if (newCategoryTotal > categoryBudget * 0.8) {
                    showApproachingBudgetNotification(newCategoryTotal, categoryBudget, newTransaction.category, ((newCategoryTotal / categoryBudget) * 100).toInt())
                }
            }
        }

        return true
    }

    private fun getCurrentWeekExpenses(transactions: List<Transaction>): Double {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val weekStart = calendar.time

        return transactions
            .filter { !it.isIncome && it.date >= weekStart }
            .sumOf { it.amount }
    }

    private fun getCurrentMonthExpenses(transactions: List<Transaction>): Double {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return transactions
            .filter { !it.isIncome } // Only expenses
            .filter {
                val transactionCalendar = Calendar.getInstance().apply { time = it.date }
                transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                transactionCalendar.get(Calendar.YEAR) == currentYear
            }
            .sumOf { it.amount }
    }

    private fun getCurrentMonthCategoryExpenses(transactions: List<Transaction>, category: String): Double {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return transactions
            .filter { !it.isIncome && it.category == category }
            .filter {
                val transactionCalendar = Calendar.getInstance().apply { time = it.date }
                transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                transactionCalendar.get(Calendar.YEAR) == currentYear
            }
            .sumOf { it.amount }
    }

    private fun updateBudgetStatus() {
        val monthlyBudget = sharedPreferences.getFloat("monthly_budget", 0f)
        if (monthlyBudget > 0) {
            val currentExpenses = getCurrentMonthExpenses(adapter.currentList)
            val percentage = (currentExpenses / monthlyBudget.toDouble() * 100).toInt()
            
            progressBudget.progress = percentage
            tvBudgetStatus.text = "Monthly Budget: $${String.format("%.2f", currentExpenses)} / $${String.format("%.2f", monthlyBudget)} (${percentage}%)"
            
            cardBudgetStatus.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), when {
                    percentage >= 100 -> R.color.error_light
                    percentage >= 80 -> R.color.warning_light
                    else -> R.color.success_light
                })
            )
        }
    }

    private fun showBudgetLimitBlockedNotification(currentAmount: Double, budgetLimit: Double, budgetType: String) {
        // Check if we have notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showBudgetLimitBlockedDialog(currentAmount, budgetLimit, budgetType)
            return
        }

        // Intent to open the Budget Fragment
        val budgetIntent = Intent(requireContext(), MainActivity::class.java).apply {
            action = "NAVIGATE_TO_BUDGET"
        }
        val budgetPendingIntent = PendingIntent.getActivity(
            requireContext(),
            1, // Unique request code
            budgetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to open the app
        val appIntent = requireActivity().packageManager.getLaunchIntentForPackage(requireActivity().packageName)
        val appPendingIntent = PendingIntent.getActivity(
            requireContext(),
            0, 
            appIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val percentage = (currentAmount / budgetLimit * 100).toInt()
        val overAmount = currentAmount - budgetLimit

        val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_warning_24)
            .setContentTitle("Transaction Blocked: $budgetType Budget Exceeded")
            .setContentText("Exceeded by $${String.format("%.2f", overAmount)} (${percentage}% used). Limit: $${String.format("%.2f", budgetLimit)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setAutoCancel(true)
            .setContentIntent(appPendingIntent) // Opens app on tap
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_baseline_settings_24, "View Budget", budgetPendingIntent) // Add action button
            .build()

        val notificationManager = ContextCompat.getSystemService(
            requireContext(),
            NotificationManager::class.java
        ) as NotificationManager

        notificationManager.notify(NOTIFICATION_ID + budgetType.hashCode() + 2000, notification)
        showBudgetLimitBlockedDialog(currentAmount, budgetLimit, budgetType)
    }

    private fun showBudgetLimitBlockedDialog(currentAmount: Double, budgetLimit: Double, budgetType: String) {
        val overAmount = currentAmount - budgetLimit
        val percentage = (currentAmount / budgetLimit * 100).toInt()

        MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Rounded)
            .setTitle("Budget Limit Exceeded!")
            .setMessage("You have exceeded your $budgetType budget by $${String.format("%.2f", overAmount)}.\n\n" +
                    "Current spending: $${String.format("%.2f", currentAmount)}\n" +
                    "Budget limit: $${String.format("%.2f", budgetLimit)}\n" +
                    "Percentage used: ${percentage}%\n\n" +
                    "Please adjust your spending or increase your budget limit.")
            .setIcon(R.drawable.ic_baseline_warning_24)
            .setPositiveButton("View Budget") { _, _ ->
                findNavController().navigate(R.id.nav_budget)
            }
            .setNegativeButton("Dismiss", null)
            .show()
    }

    private fun showApproachingBudgetNotification(currentAmount: Double, budgetLimit: Double, budgetType: String, percentage: Int) {
        // Check if we have notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Intent to open the Budget Fragment
        val budgetIntent = Intent(requireContext(), MainActivity::class.java).apply {
            action = "NAVIGATE_TO_BUDGET"
        }
        val budgetPendingIntent = PendingIntent.getActivity(
            requireContext(),
            1, // Unique request code
            budgetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to open the app
        val appIntent = requireActivity().packageManager.getLaunchIntentForPackage(requireActivity().packageName)
        val appPendingIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            appIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_warning_24)
            .setContentTitle("Approaching $budgetType Budget Limit")
            .setContentText("You've used ${percentage}% of your $budgetType budget ($${String.format("%.2f", currentAmount)} / $${String.format("%.2f", budgetLimit)})")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(appPendingIntent) // Opens app on tap
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_baseline_settings_24, "View Budget", budgetPendingIntent) // Add action button
            .build()

        val notificationManager = ContextCompat.getSystemService(
            requireContext(),
            NotificationManager::class.java
        ) as NotificationManager

        notificationManager.notify(NOTIFICATION_ID + budgetType.hashCode() + 1000, notification)
    }

    private fun showDeleteConfirmationDialog(transaction: Transaction) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTransaction(transaction)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTransaction(transaction: Transaction) {
        val transactions = adapter.currentList.toMutableList()
        transactions.remove(transaction)
        adapter.submitList(transactions)
        updateBalance(transactions)
        saveData(transactions)
        showSnackbar("Transaction deleted")
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }

    private fun loadSavedData() {
        val json = sharedPreferences.getString("transactions", null)
        val transactions = if (json != null) {
            gson.fromJson<List<Transaction>>(json, transactionType)
        } else {
            emptyList()
        }
        adapter.submitList(transactions)
        updateBalance(transactions)
    }

    private fun saveData(transactions: List<Transaction>) {
        val json = gson.toJson(transactions)
        sharedPreferences.edit().putString("transactions", json).apply()
    }

    private fun updateBalance(transactions: List<Transaction>) {
        val balance = transactions.sumOf { transaction ->
            if (transaction.isIncome) transaction.amount else -transaction.amount
        }
        tvBalance.text = CurrencyManager.formatAmount(requireContext(), balance)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Budget Notifications"
            val descriptionText = "Notifications for budget limits"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Notifications are enabled
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show educational UI
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Notification Permission Required")
                        .setMessage("Budget notifications help you stay on track with your spending. Please enable notifications to receive budget alerts.")
                        .setPositiveButton("Enable") { _, _ ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Not Now", null)
                        .show()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun observeCurrencyChanges() {
        CurrencyManager.currencyChanged.observe(viewLifecycleOwner, Observer { _ ->
            updateAmounts()
            adapter.notifyDataSetChanged()
        })
    }

    private fun updateAmounts() {
        val transactionsJson = sharedPreferences.getString("transactions", null)
        val transactions = if (transactionsJson != null) {
            gson.fromJson<List<Transaction>>(transactionsJson, transactionType)
        } else {
            emptyList()
        }

        val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
        val totalExpenses = transactions.filter { !it.isIncome }.sumOf { it.amount }
        val balance = totalIncome - totalExpenses

        tvBalance.text = CurrencyManager.formatAmount(requireContext(), balance)
        updateBudgetStatus()
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