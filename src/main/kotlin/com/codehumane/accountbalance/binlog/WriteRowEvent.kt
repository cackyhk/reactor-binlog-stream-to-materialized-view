package com.codehumane.accountbalance.binlog

import com.codehumane.accountbalance.binlog.schema.Table
import java.io.Serializable

data class WriteRowEvent(
    val table: Table,
    val row: Array<Serializable>
)