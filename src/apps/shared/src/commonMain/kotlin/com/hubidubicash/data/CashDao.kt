package com.hubidubicash.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

/** Every read leaves deleted rows out; a range of days is first to last, both included. */
@Dao
interface CashDao {
    @Query(
        "SELECT * FROM spending WHERE deleted = 0 AND day BETWEEN :first AND :last " +
            "ORDER BY day DESC, created DESC, id DESC LIMIT :limit OFFSET :offset"
    )
    suspend fun page(first: Long, last: Long, limit: Int, offset: Int): List<SpendingRow>

    @Query("SELECT COUNT(*) FROM spending WHERE deleted = 0 AND day BETWEEN :first AND :last")
    suspend fun count(first: Long, last: Long): Int

    @Query(
        "SELECT day, SUM(amount) AS total FROM spending WHERE deleted = 0 AND day BETWEEN :first AND :last " +
            "GROUP BY day"
    )
    suspend fun dayTotals(first: Long, last: Long): List<DayTotal>

    @Query(
        "SELECT kind, SUM(amount) AS total FROM spending WHERE deleted = 0 AND day BETWEEN :first AND :last " +
            "GROUP BY kind"
    )
    suspend fun kindTotals(first: Long, last: Long): List<KindTotal>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM spending WHERE deleted = 0 AND day BETWEEN :first AND :last")
    suspend fun total(first: Long, last: Long): Long

    @Query("SELECT * FROM spending WHERE id = :id")
    suspend fun spending(id: String): SpendingRow?

    @Upsert
    suspend fun upsertSpending(row: SpendingRow)

    /** Adds [row] unless a spending with its id exists, deleted or not; -1 when it did. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSpendingIfAbsent(row: SpendingRow): Long

    @Query("SELECT * FROM regular WHERE deleted = 0 ORDER BY income DESC, dayOfMonth, created, id")
    suspend fun regulars(): List<RegularRow>

    @Query("SELECT * FROM regular WHERE id = :id")
    suspend fun regular(id: String): RegularRow?

    @Upsert
    suspend fun upsertRegular(row: RegularRow)

    /** Moves [lastAdded][RegularRow.lastAdded] on; [RegularRow.updated] stays, as the user changed nothing. */
    @Query("UPDATE regular SET lastAdded = :month WHERE id = :id")
    suspend fun setLastAdded(id: String, month: String)

    @Query("SELECT * FROM bill_mark")
    suspend fun marks(): List<BillMarkRow>

    @Query("SELECT * FROM bill_mark WHERE regularId = :regularId AND month = :month")
    suspend fun mark(regularId: String, month: String): BillMarkRow?

    @Upsert
    suspend fun upsertMark(row: BillMarkRow)

    /** Deleted ones too: a backup's restore compares against them. */
    @Query("SELECT * FROM spending")
    suspend fun allSpendings(): List<SpendingRow>

    @Query("SELECT * FROM regular")
    suspend fun allRegulars(): List<RegularRow>
}
