package com.codehumane.accountbalance.binlog

import com.codehumane.accountbalance.binlog.schema.Column
import org.springframework.stereotype.Component
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

@Component
class ColumnValuesConverter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss") // LocalDateTime에 맞춤

    internal fun convert(event: WriteRowEvent): MutableMap<String, String> {
        val columnToValue = mutableMapOf<String, String>()
        val columns = event.table.columns.iterator()
        val values = event.row

        (0 until values.size).forEach { idx ->
            val column = columns.next()
            val value = deserializeValue(values[idx], column.value)
            columnToValue[column.key] = value
        }

        return columnToValue
    }

    private fun deserializeValue(value: Serializable, column: Column): String {
        if (value is String) {
            return value
        }

        if (value is ByteArray) {
            check(column.charset.isNullOrBlank() || column.charset!!.startsWith("utf8"))
            return String(value, Charsets.UTF_8)
        }

        if (value is Date) {
            return dateFormat.format(value)
        }

        return value.toString()
    }
}