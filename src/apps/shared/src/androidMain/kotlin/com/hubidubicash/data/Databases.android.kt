package com.hubidubicash.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Dispatchers

/** The phone's database, through Android's own SQLite: nothing native ships in the app. */
fun androidDatabase(context: Context): CashDatabase {
    val app = context.applicationContext
    return Room.databaseBuilder<CashDatabase>(app, app.getDatabasePath("hubidubicash.db").absolutePath)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
