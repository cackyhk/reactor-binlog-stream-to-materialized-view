package com.codehumane.accountbalance.binlog

import com.codehumane.accountbalance.schema.Column
import com.codehumane.accountbalance.schema.SchemaInfoContainer
import com.codehumane.accountbalance.schema.Table
import com.github.shyiko.mysql.binlog.BinaryLogClient
import com.github.shyiko.mysql.binlog.event.*
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer.CompatibilityMode.CHAR_AND_BINARY_AS_BYTE_ARRAY
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import java.net.URI
import java.util.concurrent.CompletableFuture

class BinaryLogReceiver(
    jdbcTemplate: JdbcTemplate,
    hikariDataSource: HikariDataSource,
    schemaInfoContainer: SchemaInfoContainer,
    onWriteRowsEventReceive: (WriteRowEvent) -> Unit
) {

    private val log = LoggerFactory.getLogger(BinaryLogReceiver::class.java)
    private val binaryLogClient: BinaryLogClient
    private val binaryLogReceivers: Set<BinaryLogEventReceiver> = setOf(
        TableMapBinaryLogEventReceiver(jdbcTemplate, schemaInfoContainer),
        WriteRowsBinaryLogEventReceiver(onWriteRowsEventReceive, schemaInfoContainer)
    )

    init {
        binaryLogClient = hikariDataSource.run {
            val uri = URI.create(jdbcUrl.removePrefix("jdbc:"))
            BinaryLogClient(
                uri.host,
                uri.port,
                username,
                password
            )
        }

        binaryLogClient.apply {
            setEventDeserializer(EventDeserializer().apply {
                setCompatibilityMode(CHAR_AND_BINARY_AS_BYTE_ARRAY)
            })

            registerEventListener {
                val eventType = it.getHeader<EventHeader>().eventType
                val receiver = binaryLogReceivers.find { r -> r.isReceivable(eventType) }
                receiver?.receive(it)
            }

            connectTimeout = 5000
            keepAliveInterval = 1000
            serverId = 6
        }
    }

    fun start() {
        log.info("binary log client start connection")
        CompletableFuture.runAsync { binaryLogClient.connect() }
    }

    interface BinaryLogEventReceiver {

        fun isReceivable(eventType: EventType): Boolean

        fun receive(event: Event)
    }

    private class TableMapBinaryLogEventReceiver(
        private val jdbcTemplate: JdbcTemplate,
        private val schemaInfoContainer: SchemaInfoContainer
    ) : BinaryLogEventReceiver {

        override fun isReceivable(eventType: EventType): Boolean {
            return eventType == EventType.TABLE_MAP
        }

        override fun receive(event: Event) {
            val data = event.getData<TableMapEventData>()
            schemaInfoContainer.computeIfAbsent(data.tableId) {
                Table(
                    data.database,
                    data.table,
                    data.tableId,
                    generatePrimaryKeyInfo(data),
                    generateColumnInfo(data)
                )
            }
        }

        private fun generatePrimaryKeyInfo(data: TableMapEventData): List<String> {
            val query = """
                select lower(column_name) column_name
                from information_schema.statistics
                where index_name = 'PRIMARY'
                  and table_schema = ?
                  and table_name = ?
                order by seq_in_index
            """.trimIndent().replace("\n", " ")

            return jdbcTemplate.query(query, arrayOf(data.database, data.table)) { rs, _ ->
                rs.getString("column_name")
            }
        }

        private fun generateColumnInfo(data: TableMapEventData): Map<String, Column> {
            val query = """
                select
                  ordinal_position,
                  lower(column_name) column_name,
                  lower(character_set_name) character_set_name,
                  instr(column_type, 'unsigned') is_unsigned
                from information_schema.columns
                where table_schema = ?
                  and table_name = ?
                order by ordinal_position
            """.trimIndent().replace("\n", " ")

            val columns = jdbcTemplate.query(query, arrayOf(data.database, data.table)) { rs, _ ->
                Column(
                    rs.getString("column_name"),
                    rs.getString("character_set_name"),
                    rs.getInt("is_unsigned") > 0
                )
            }

            return columns.associateBy({ it.column }, { it })
        }
    }

    private class WriteRowsBinaryLogEventReceiver(
        private val onWriteRowsEventReceive: (WriteRowEvent) -> Unit,
        private val schemaInfoContainer: SchemaInfoContainer
    ) : BinaryLogEventReceiver {

        private val targetEventTypes = setOf(
            EventType.PRE_GA_WRITE_ROWS,
            EventType.EXT_WRITE_ROWS,
            EventType.WRITE_ROWS
        )

        override fun isReceivable(eventType: EventType): Boolean {
            return targetEventTypes.contains(eventType)
        }

        override fun receive(event: Event) {
            val data = event.getData<WriteRowsEventData>()
            val table = schemaInfoContainer.get(data.tableId)

            table?.also { t ->
                data.rows.forEach { r ->
                    onWriteRowsEventReceive(WriteRowEvent(t, r))
                }
            }
        }

    }

}