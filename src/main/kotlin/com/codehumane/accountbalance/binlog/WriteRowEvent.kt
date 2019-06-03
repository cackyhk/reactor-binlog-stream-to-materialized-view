package com.codehumane.accountbalance.binlog

import com.codehumane.accountbalance.schema.Table
import java.io.Serializable

data class WriteRowEvent(val table: Table, val row: Array<Serializable>) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as WriteRowEvent
        if (table != other.table) return false
        if (!row.contentEquals(other.row)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = table.hashCode()
        result = 31 * result + row.contentHashCode()
        return result
    }
}