package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.DebtEntity
import com.example.data.model.KhatEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity

@Database(
    entities = [
        UserEntity::class,
        TransactionEntity::class,
        AccountEntity::class,
        DebtEntity::class,
        SavingsGoalEntity::class,
        KhatEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun debtDao(): DebtDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun khatDao(): KhatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE savings_goals ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE savings_goals ADD COLUMN historyJson TEXT NOT NULL DEFAULT '[]'")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN khatId INTEGER DEFAULT NULL")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN khatName TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `khats` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `syncId` TEXT NOT NULL,
                            `userId` INTEGER NOT NULL,
                            `name` TEXT NOT NULL,
                            `allocatedAmount` REAL NOT NULL,
                            `colorHex` TEXT NOT NULL DEFAULT '#4F46E5',
                            `iconName` TEXT NOT NULL DEFAULT 'folder',
                            `timestamp` INTEGER NOT NULL DEFAULT 0,
                            `updatedAt` INTEGER NOT NULL DEFAULT 0,
                            `isDeleted` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE debts ADD COLUMN khatId INTEGER DEFAULT NULL")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE debts ADD COLUMN khatName TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE debts ADD COLUMN deductedFromMain INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE debts ADD COLUMN paidAmount REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE debts ADD COLUMN paymentHistoryJson TEXT NOT NULL DEFAULT '[]'")
                } catch (_: Exception) {}
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daily_expense_tracker.db"
                ).addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
