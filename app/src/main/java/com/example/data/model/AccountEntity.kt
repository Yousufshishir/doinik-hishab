package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String, // "bkash", "nagad", "rocket", "dbbl", "brac", "cash"
    val name: String,
    val type: String, // "MFS", "BANK", "CASH"
    val balance: Double,
    val accountIdentifier: String = "", // e.g. "017•• •••55" or "A/C ••4321"
    val colorHex: Long = 0xFF0D6E4A
)
