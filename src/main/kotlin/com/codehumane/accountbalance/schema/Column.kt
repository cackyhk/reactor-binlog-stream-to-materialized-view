package com.codehumane.accountbalance.schema

data class Column(
    val column: String,
    val charset: String?,
    val unsigned: Boolean
)