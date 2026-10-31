package com.hubidubicash.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One spending: what kind, how much (in hundredths of the currency), on which
 * day. A spending a regular cost added names it in [regularId]. Deleting only
 * marks it [deleted], so a restore from a backup made before the delete
 * leaves it deleted, and a regular cost never adds the same month back.
 */
@Entity(tableName = "spending", indices = [Index("day"), Index("regularId")])
data class SpendingRow(
    @PrimaryKey val id: String,
    val kind: String,
    val amount: Long,
    /** The day it was spent, as days since 1970-01-01 ([java.time.LocalDate.toEpochDay]). */
    val day: Long,
    val note: String,
    val regularId: String?,
    /** When it was made and last changed, milliseconds since 1970 (UTC). */
    val created: Long,
    val updated: Long,
    val deleted: Boolean = false,
)

/**
 * A regular income or spending: the same amount every month on [dayOfMonth]
 * (a day past a short month's end falls on its last day). A regular spending
 * is added to the spendings on its day; [lastAdded] is the last month it was,
 * "yyyy-MM". Incomes are never added anywhere; they are for Bills.
 */
@Entity(tableName = "regular")
data class RegularRow(
    @PrimaryKey val id: String,
    val income: Boolean,
    /** A [com.hubidubicash.core.Kind] id; "income" for an income. */
    val kind: String,
    val amount: Long,
    val note: String,
    val dayOfMonth: Int,
    /** The day it was made, as an epoch day: its first month is the first whose day is on or after it. */
    val since: Long,
    val lastAdded: String?,
    val created: Long,
    val updated: Long,
    val deleted: Boolean = false,
)

/** Whether a regular income was received, or a regular spending paid, in one month ("yyyy-MM"). */
@Entity(tableName = "bill_mark", primaryKeys = ["regularId", "month"])
data class BillMarkRow(
    val regularId: String,
    val month: String,
    val paid: Boolean,
    val updated: Long,
)

data class DayTotal(val day: Long, val total: Long)

data class KindTotal(val kind: String, val total: Long)
