package com.example.data.database

import androidx.room.*
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getTransactionsByUser(userId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND timestamp >= :startTime AND timestamp <= :endTime AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getTransactionsBetween(userId: Long, startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE syncId = :syncId LIMIT 1")
    suspend fun getTransactionBySyncId(syncId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllTransactionsIncludingDeleted(userId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND isDeleted = 0 ORDER BY timestamp DESC")
    suspend fun getActiveTransactionsByUser(userId: Long): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markDeleted(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE syncId = :syncId")
    suspend fun markDeletedBySyncId(syncId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM transactions WHERE transactionRef = :ref AND isDeleted = 0")
    suspend fun getTransactionsByRef(ref: String): List<TransactionEntity>

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE transactionRef = :ref")
    suspend fun markDeletedByRef(ref: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM transactions WHERE transactionRef LIKE :pattern AND isDeleted = 0")
    suspend fun getTransactionsByRefPattern(pattern: String): List<TransactionEntity>

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE transactionRef LIKE :pattern")
    suspend fun markDeletedByRefPattern(pattern: String, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun clearAllForUser(userId: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}
