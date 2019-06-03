package com.codehumane.accountbalance.schema

import org.springframework.stereotype.Component

@Component
class SchemaInfoContainer {

    private val tables: MutableMap<Long, Table> = mutableMapOf() // tableId to table

    fun computeIfAbsent(tableId: Long, function: (Long) -> Table) {
        tables.computeIfAbsent(tableId, function)
    }

    fun get(tableId: Long): Table? {
        return tables[tableId]
    }

}