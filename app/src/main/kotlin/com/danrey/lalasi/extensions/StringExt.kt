// StringExt.kt

package com.danrey.lalasi.extensions

import androidx.sqlite.db.SimpleSQLiteQuery

inline fun <reified T : Enum<T>> String?.toEnum(defaultValue: T): T =
    if (this == null) {
        defaultValue
    } else {
        try {
            // names saved before the rename to LALASI (home style, Discord button) still resolve
            enumValueOf(if (this == "MOCHYY" || this == "MOCHY") "LALASI" else this)
        } catch (e: IllegalArgumentException) {
            defaultValue
        }
    }

fun String.toSQLiteQuery(): SimpleSQLiteQuery = SimpleSQLiteQuery(this)
