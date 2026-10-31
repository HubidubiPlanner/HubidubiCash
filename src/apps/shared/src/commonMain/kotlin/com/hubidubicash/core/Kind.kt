package com.hubidubicash.core

/**
 * The eight kinds a spending can be, in their fixed order: the order "+"
 * lists them in, and the order of the circle's slices, so a kind keeps its
 * place and its colour whatever the amounts. [label] is the English, shown
 * through t().
 */
enum class Kind(val id: String, val label: String) {
    FOOD("food", "Food"),
    HOME("home", "Home"),
    TRANSPORT("transport", "Transport"),
    UTILITIES("utilities", "Utilities"),
    HEALTH("health", "Health"),
    FUN("fun", "Fun"),
    SHOPPING("shopping", "Shopping"),
    OTHER("other", "Other");

    companion object {
        /** The kind stored as [id]; Other for anything unknown, so nothing is ever lost from a list. */
        fun of(id: String?): Kind = entries.firstOrNull { it.id == id } ?: OTHER
    }
}

/** The kind a regular income is stored with. */
const val INCOME = "income"
