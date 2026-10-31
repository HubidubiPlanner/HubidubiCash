package com.hubidubicash.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(entities = [SpendingRow::class, RegularRow::class, BillMarkRow::class], version = 1)
@ConstructedBy(CashDatabaseConstructor::class)
abstract class CashDatabase : RoomDatabase() {
    abstract fun dao(): CashDao
}

// Room writes the actual for each platform.
@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object CashDatabaseConstructor : RoomDatabaseConstructor<CashDatabase> {
    override fun initialize(): CashDatabase
}
