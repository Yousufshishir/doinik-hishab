package com.example.data.database

import androidx.room.*
import com.example.data.model.KhatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KhatDao {
    @Query("SELECT * FROM khats WHERE userId = :userId AND isDeleted = 0 ORDER BY id ASC")
    fun getKhatsByUser(userId: Long): Flow<List<KhatEntity>>

    @Query("SELECT * FROM khats WHERE userId = :userId ORDER BY updatedAt DESC")
    suspend fun getAllKhatsIncludingDeleted(userId: Long): List<KhatEntity>

    @Query("SELECT * FROM khats WHERE id = :id LIMIT 1")
    suspend fun getKhatById(id: Long): KhatEntity?

    @Query("SELECT * FROM khats WHERE syncId = :syncId LIMIT 1")
    suspend fun getKhatBySyncId(syncId: String): KhatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhat(khat: KhatEntity): Long

    @Update
    suspend fun updateKhat(khat: KhatEntity)

    @Query("UPDATE khats SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markDeleted(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM khats WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Delete
    suspend fun deleteKhat(khat: KhatEntity)

    @Query("DELETE FROM khats WHERE userId = :userId")
    suspend fun clearAllForUser(userId: Long)

    @Query("DELETE FROM khats")
    suspend fun clearAll()
}
