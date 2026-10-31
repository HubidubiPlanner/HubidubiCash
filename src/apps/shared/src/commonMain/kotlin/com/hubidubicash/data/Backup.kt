package com.hubidubicash.data

import com.hubidubicash.core.Sealed
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Everything in the app as one file, made by Settings › Export and read back
 * by Import, on the phone or the computer.
 *
 * Sealed with a password the person chooses ([Sealed]); inside, a ZIP holding
 * `backup.json`: every spending, regular income and spending, and what was
 * ticked in Bills. Nothing of the device itself goes in: no settings.
 *
 * A restore changes a thing only where the backup's copy is newer than the
 * one here, so restoring onto a device that has moved on loses nothing, and
 * something deleted here after the backup was made stays deleted.
 */
object Backup {
    const val FORMAT = "hubidubicash-backup"
    const val VERSION = 1
    private const val INDEX = "backup.json"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    class NotABackup : Exception("not a HubidubiCash backup")
    /** The file is sealed and no password was given. */
    class PasswordNeeded : Exception("password needed")

    data class Contents(
        val spendings: List<SpendingRow>,
        val regulars: List<RegularRow>,
        val marks: List<BillMarkRow>,
        val created: String?,
        val appVersion: String?,
    )

    /** What restoring would do here: the rows to write, and how many are new, newer, or already here. */
    data class Plan(
        val spendings: List<SpendingRow>,
        val regulars: List<RegularRow>,
        val marks: List<BillMarkRow>,
        val added: Int,
        val replaced: Int,
        val alreadyHere: Int,
    )

    @Serializable
    private data class FileJson(
        val format: String,
        val version: Int,
        @SerialName("app_version") val appVersion: String = "",
        val created: String = "",
        val spendings: List<SpendingJson> = emptyList(),
        val regulars: List<RegularJson> = emptyList(),
        val marks: List<MarkJson> = emptyList(),
    )

    @Serializable
    private data class SpendingJson(
        val id: String, val kind: String, val amount: Long, val day: String, val note: String = "",
        val regular: String? = null, val created: Long, val updated: Long,
    )

    @Serializable
    private data class RegularJson(
        val id: String, val income: Boolean, val kind: String, val amount: Long, val note: String = "",
        @SerialName("day_of_month") val dayOfMonth: Int, val since: String,
        @SerialName("last_added") val lastAdded: String? = null, val created: Long, val updated: Long,
    )

    @Serializable
    private data class MarkJson(val regular: String, val month: String, val paid: Boolean, val updated: Long)

    /** The live rows, to [out], sealed with [password] (null only in tests: a bare ZIP). */
    fun write(
        out: OutputStream,
        spendings: List<SpendingRow>,
        regulars: List<RegularRow>,
        marks: List<BillMarkRow>,
        appVersion: String,
        password: CharArray?,
        rounds: Int = Sealed.ROUNDS,
    ) {
        val file = FileJson(
            format = FORMAT, version = VERSION, appVersion = appVersion, created = Instant.now().toString(),
            spendings = spendings.filter { !it.deleted }.sortedBy { it.id }.map {
                SpendingJson(it.id, it.kind, it.amount, LocalDate.ofEpochDay(it.day).toString(), it.note, it.regularId,
                    it.created, it.updated)
            },
            regulars = regulars.filter { !it.deleted }.sortedBy { it.id }.map {
                RegularJson(it.id, it.income, it.kind, it.amount, it.note, it.dayOfMonth,
                    LocalDate.ofEpochDay(it.since).toString(), it.lastAdded, it.created, it.updated)
            },
            marks = marks.sortedWith(compareBy({ it.regularId }, { it.month }))
                .map { MarkJson(it.regularId, it.month, it.paid, it.updated) },
        )
        ZipOutputStream(if (password == null) out else Sealed.seal(out, password, rounds)).use { zip ->
            zip.putNextEntry(ZipEntry(INDEX))
            zip.write(json.encodeToString(FileJson.serializer(), file).toByteArray())
            zip.closeEntry()
        }
    }

    /** Whether the file starting in [input] is sealed, so a password must be asked for. */
    fun isSealed(input: InputStream): Boolean {
        val head = ByteArray(8)
        var got = 0
        while (got < head.size) { val k = input.read(head, got, head.size - got); if (k < 0) break; got += k }
        return Sealed.isSealed(head.copyOf(got))
    }

    /** What is in a backup. Throws [NotABackup], [PasswordNeeded], [Sealed.WrongPassword] or [Sealed.Damaged]. */
    fun read(input: InputStream, password: CharArray?): Contents {
        val b = BufferedInputStream(input, 64)
        b.mark(16)
        val sealed = isSealed(b)
        b.reset()
        val inside = if (sealed) Sealed.open(b, password ?: throw PasswordNeeded()) else b
        ZipInputStream(inside).use { zip ->
            while (true) {
                val entry = try {
                    zip.nextEntry
                } catch (e: Sealed.WrongPassword) {
                    throw e
                } catch (e: Sealed.Damaged) {
                    throw e
                } catch (e: java.io.IOException) {
                    null
                } ?: throw NotABackup()
                if (entry.name != INDEX) continue
                val f = runCatching { json.decodeFromString(FileJson.serializer(), zip.readBytes().decodeToString()) }
                    .getOrNull()
                if (f == null || f.format != FORMAT || f.version > VERSION) throw NotABackup()
                return Contents(
                    spendings = f.spendings.map {
                        SpendingRow(it.id, it.kind, it.amount, LocalDate.parse(it.day).toEpochDay(), it.note,
                            it.regular, it.created, it.updated)
                    },
                    regulars = f.regulars.map {
                        RegularRow(it.id, it.income, it.kind, it.amount, it.note, it.dayOfMonth.coerceIn(1, 31),
                            LocalDate.parse(it.since).toEpochDay(), it.lastAdded, it.created, it.updated)
                    },
                    marks = f.marks.map { BillMarkRow(it.regular, it.month, it.paid, it.updated) },
                    created = f.created.ifEmpty { null },
                    appVersion = f.appVersion.ifEmpty { null },
                )
            }
        }
    }

    /**
     * What restoring [contents] would change, given what is here. The newer
     * copy wins, by when it was last changed; on a tie, the one here stays. A
     * regular item keeps the later of the two months it was last added in, so
     * no month is ever added twice.
     */
    fun plan(
        contents: Contents,
        spending: (String) -> SpendingRow?,
        regular: (String) -> RegularRow?,
        mark: (String, String) -> BillMarkRow?,
    ): Plan {
        var added = 0; var replaced = 0; var same = 0
        fun <T> count(local: T?, newer: Boolean): Boolean = when {
            local == null -> { added++; true }
            newer -> { replaced++; true }
            else -> { same++; false }
        }
        val spendings = contents.spendings.filter { r ->
            val local = spending(r.id)
            count(local, local != null && r.updated > local.updated)
        }
        val regulars = contents.regulars.mapNotNull { r ->
            val local = regular(r.id)
            if (!count(local, local != null && r.updated > local.updated)) null
            else r.copy(lastAdded = listOfNotNull(r.lastAdded, local?.lastAdded).maxOrNull())
        }
        val marks = contents.marks.filter { m ->
            val local = mark(m.regularId, m.month)
            local == null || m.updated > local.updated
        }
        return Plan(spendings, regulars, marks, added, replaced, same)
    }

    /** "HubidubiCash 2026-10-31.hubidubicash". */
    fun fileName(today: LocalDate): String = "HubidubiCash $today.hubidubicash"
}
