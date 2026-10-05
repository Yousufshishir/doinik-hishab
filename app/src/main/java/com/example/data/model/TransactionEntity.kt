package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val syncId: String = UUID.randomUUID().toString(),
    val userId: Long = 0,
    val title: String,
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val category: String,
    val paymentMethod: String = "ক্যাশ",
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val encryptedNote: String = "",
    val isEncrypted: Boolean = false,
    val fee: Double = 0.0,
    val transactionRef: String = "",
    val khatId: Long? = null,
    val khatName: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
