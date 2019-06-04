package com.codehumane.accountbalance.binlog.schema

data class Column(
    val column: String,
    val charset: String?,
    val unsigned: Boolean
)