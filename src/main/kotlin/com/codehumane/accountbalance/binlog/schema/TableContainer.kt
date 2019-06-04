package com.codehumane.accountbalance.binlog.schema

import org.springframework.stereotype.Component

@Component
class TableContainer {

    private val tables: MutableMap<Long, Table> = mutableMapOf() // tableId to table

    fun computeIfAbsent(tableId: Long, function: (Long) -> Table) {
        tables.computeIfAbsent(tableId, function)
    }

    fun get(tableId: Long): Table? {
        return tables[tableId]
    }

}