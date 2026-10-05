package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val syncId: String = UUID.randomUUID().toString(),
    val userId: Long = 0,
    val personName: String,
    val amount: Double,
    val type: String, // "RECEIVE" (আমি পাব) or "PAY" (আমি দেব)
    val dueDate: String = "",
    val note: String = "",
    val isSettled: Boolean = false,
    val khatId: Long? = null,
    val khatName: String = "",
    val deductedFromMain: Boolean = false,
    val paidAmount: Double = 0.0,
    val paymentHistoryJson: String = "[]",
    val timestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    val remainingAmount: Double
        get() = if (isSettled) 0.0 else maxOf(0.0, amount - paidAmount)

    val effectivePaidAmount: Double
        get() = if (isSettled && paidAmount <= 0.0) amount else paidAmount

    fun parsePaymentHistory(): List<DebtPaymentRecord> {
        if (paymentHistoryJson.isBlank() || paymentHistoryJson == "[]") return emptyList()
        return try {
            val arr = org.json.JSONArray(paymentHistoryJson)
            val list = mutableListOf<DebtPaymentRecord>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DebtPaymentRecord(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        amount = obj.optDouble("amount", 0.0),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        note = obj.optString("note", ""),
                        remainingAfter = obj.optDouble("remainingAfter", 0.0)
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addPaymentRecord(record: DebtPaymentRecord): String {
        return try {
            val existing = parsePaymentHistory().toMutableList()
            existing.add(0, record)
            val arr = org.json.JSONArray()
            for (item in existing) {
                val obj = org.json.JSONObject()
                obj.put("id", item.id)
                obj.put("amount", item.amount)
                obj.put("timestamp", item.timestamp)
                obj.put("note", item.note)
                obj.put("remainingAfter", item.remainingAfter)
                arr.put(obj)
            }
            arr.toString()
        } catch (_: Exception) {
            paymentHistoryJson
        }
    }
}

data class DebtPaymentRecord(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val remainingAfter: Double = 0.0
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val syncId: String = UUID.randomUUID().toString(),
    val userId: Long = 0,
    val title: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val note: String = "",
    val isCompleted: Boolean = false,
    val historyJson: String = "[]",
    val timestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    fun parseHistory(): List<SavingsTransaction> {
        if (historyJson.isBlank() || historyJson == "[]") return emptyList()
        return try {
            val arr = org.json.JSONArray(historyJson)
            val list = mutableListOf<SavingsTransaction>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SavingsTransaction(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        amount = obj.optDouble("amount", 0.0),
                        type = obj.optString("type", "DEPOSIT"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        note = obj.optString("note", ""),
                        balanceAfter = obj.optDouble("balanceAfter", 0.0)
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addHistoryRecord(record: SavingsTransaction): String {
        return try {
            val existing = parseHistory().toMutableList()
            existing.add(0, record)
            val arr = org.json.JSONArray()
            for (item in existing) {
                val obj = org.json.JSONObject()
                obj.put("id", item.id)
                obj.put("amount", item.amount)
                obj.put("type", item.type)
                obj.put("timestamp", item.timestamp)
                obj.put("note", item.note)
                obj.put("balanceAfter", item.balanceAfter)
                arr.put(obj)
            }
            arr.toString()
        } catch (e: Exception) {
            historyJson
        }
    }
}

data class SavingsTransaction(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val type: String, // "DEPOSIT" or "WITHDRAWAL"
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val balanceAfter: Double = 0.0
)
