package com.example

import com.example.data.model.SimpleCategoryData
import com.example.data.model.TransactionEntity
import com.example.data.model.formatTakaSafe
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.max

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun test_formatTakaSafe() {
        val formatted = formatTakaSafe(1250.50)
        assertEquals("৳ 1,250.50", formatted)
    }

    @Test
    fun test_transactionEntityCreation() {
        val transaction = TransactionEntity(
            title = "বাজার খরচ",
            amount = 450.0,
            type = "EXPENSE",
            category = "খাবার ও বাজার"
        )
        assertEquals("বাজার খরচ", transaction.title)
        assertEquals(450.0, transaction.amount, 0.001)
        assertEquals("EXPENSE", transaction.type)
        assertEquals("খাবার ও বাজার", transaction.category)
    }

    @Test
    fun test_categoryIconsExist() {
        val categories = SimpleCategoryData.expenseCategories
        assertTrue(categories.isNotEmpty())
        val foodCat = categories.find { it.id == "food" }
        assertNotNull(foodCat)
        assertEquals("খাবার ও বাজার", foodCat?.name)
    }

    @Test
    fun test_netBalanceCalculation() {
        val income = 5000.0
        val expense = 1500.0
        val net = income - expense
        assertEquals(3500.0, net, 0.001)
    }

    @Test
    fun test_monthlySavingsRateCalculation() {
        val income = 40000.0
        val expense = 25000.0
        val savings = income - expense
        val rate = (savings / income) * 100.0
        assertEquals(37.5, rate, 0.01)
    }

    @Test
    fun test_safeDailySpendingAllowance() {
        val monthlyBudget = 30000.0
        val currentExpense = 12000.0
        val remainingDays = 15
        val remainingBudget = monthlyBudget - currentExpense
        val safeDaily = remainingBudget / max(1, remainingDays)
        assertEquals(1200.0, safeDaily, 0.01)
    }
}
