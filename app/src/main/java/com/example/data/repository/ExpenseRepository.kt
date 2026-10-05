package com.example.data.repository

import com.example.data.database.AccountDao
import com.example.data.database.TransactionDao
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao? = null
) {

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsByUser(userId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByUser(userId)

    suspend fun addTransaction(
        userId: Long,
        title: String,
        amount: Double,
        type: String,
        category: String,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        val entity = TransactionEntity(
            userId = userId,
            title = title,
            amount = amount,
            type = type,
            category = category,
            paymentMethod = "ক্যাশ",
            timestamp = timestamp,
            note = note
        )
        transactionDao.insertTransaction(entity)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.markDeleted(transaction.id, System.currentTimeMillis())
    }

    suspend fun clearAllForUser(userId: Long) = withContext(Dispatchers.IO) {
        transactionDao.clearAllForUser(userId)
    }

    suspend fun clearAllTransactions() = withContext(Dispatchers.IO) {
        transactionDao.clearAll()
    }
}
