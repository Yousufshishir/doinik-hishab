package com.example.data.database

import androidx.room.*
import com.example.data.model.DebtEntity
import com.example.data.model.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE userId = :userId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getDebtsByUser(userId: Long): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllDebtsIncludingDeleted(userId: Long): List<DebtEntity>

    @Query("SELECT * FROM debts WHERE syncId = :syncId LIMIT 1")
    suspend fun getDebtBySyncId(syncId: String): DebtEntity?

    @Query("SELECT * FROM debts WHERE id = :id LIMIT 1")
    suspend fun getDebtById(id: Long): DebtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(debts: List<DebtEntity>)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Query("UPDATE debts SET isSettled = :settled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setSettled(id: Long, settled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE debts SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markDeleted(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE debts SET isDeleted = 1, updatedAt = :updatedAt WHERE syncId = :syncId")
    suspend fun markDeletedBySyncId(syncId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM debts WHERE userId = :userId")
    suspend fun clearAllForUser(userId: Long)

    @Query("DELETE FROM debts")
    suspend fun clearAll()
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE userId = :userId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getGoalsByUser(userId: Long): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllGoalsIncludingDeleted(userId: Long): List<SavingsGoalEntity>

    @Query("SELECT * FROM savings_goals WHERE syncId = :syncId LIMIT 1")
    suspend fun getGoalBySyncId(syncId: String): SavingsGoalEntity?

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    suspend fun getGoalById(id: Long): SavingsGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<SavingsGoalEntity>)

    @Update
    suspend fun updateGoal(goal: SavingsGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoalEntity)

    @Query("UPDATE savings_goals SET savedAmount = savedAmount + :amount, updatedAt = :updatedAt WHERE id = :id")
    suspend fun addSavings(id: Long, amount: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE savings_goals SET savedAmount = :savedAmount, isCompleted = :isCompleted, historyJson = :historyJson, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSavingsProgress(id: Long, savedAmount: Double, isCompleted: Boolean, historyJson: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE savings_goals SET savedAmount = CASE WHEN (savedAmount - :amount) < 0 THEN 0.0 ELSE (savedAmount - :amount) END, updatedAt = :updatedAt WHERE id = :id")
    suspend fun withdrawSavings(id: Long, amount: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE savings_goals SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markDeleted(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE savings_goals SET isDeleted = 1, updatedAt = :updatedAt WHERE syncId = :syncId")
    suspend fun markDeletedBySyncId(syncId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM savings_goals WHERE userId = :userId")
    suspend fun clearAllForUser(userId: Long)

    @Query("DELETE FROM savings_goals")
    suspend fun clearAll()
}
