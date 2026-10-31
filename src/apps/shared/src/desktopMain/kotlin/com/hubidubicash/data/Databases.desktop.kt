package com.hubidubicash.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

/** The computer's database: one SQLite file in the app's data folder. */
fun desktopDatabase(file: File): CashDatabase {
    file.parentFile?.mkdirs()
    return Room.databaseBuilder<CashDatabase>(name = file.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

/** A database that lives only as long as the process: for tests. */
fun inMemoryDatabase(): CashDatabase =
    Room.inMemoryDatabaseBuilder<CashDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
