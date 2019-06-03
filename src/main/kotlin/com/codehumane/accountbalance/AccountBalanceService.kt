package com.codehumane.accountbalance

import com.codehumane.accountbalance.binlog.BinaryLogReceiver
import com.codehumane.accountbalance.binlog.WriteRowEvent
import com.codehumane.accountbalance.schema.SchemaInfoContainer
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import javax.annotation.PostConstruct

@Service
class AccountBalanceService(
    jdbcTemplate: JdbcTemplate,
    hikariDataSource: HikariDataSource,
    schemaInfoContainer: SchemaInfoContainer
) {

    private val log = LoggerFactory.getLogger(AccountBalanceService::class.java)
    private val writeEvents = Flux.create<WriteRowEvent> { sink ->
        val binaryLogReceiver = BinaryLogReceiver(jdbcTemplate, hikariDataSource, schemaInfoContainer) {
            log.info("WriteRowEvent next: {}", it)
            sink.next(it)
        }

        binaryLogReceiver.start()
    }

    @PostConstruct
    fun initialize() {
        writeEvents.subscribe {
            log.info("WriteRowEvent subscribed: {}", it)
        }
    }

    fun get(): String {
        return "nothing-yet"
    }
}