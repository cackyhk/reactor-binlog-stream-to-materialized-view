package com.codehumane.accountbalance.binlog.schema

data class Table(
    val database: String,
    val table: String,
    val tableId: Long,
    val primaryKeys: List<String>,
    val columns: Map<String, Column>
)