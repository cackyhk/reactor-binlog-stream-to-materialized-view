package com.codehumane.accountbalance.account

import com.codehumane.accountbalance.binlog.BinaryLogReceiver
import com.codehumane.accountbalance.binlog.WriteRowEvent
import com.codehumane.accountbalance.binlog.schema.TableContainer
import com.codehumane.accountbalance.transfer.TransferRepository
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import javax.annotation.PostConstruct

@Service
class AccountBalanceAccumulator(
    jdbcTemplate: JdbcTemplate,
    hikariDataSource: HikariDataSource,
    tableContainer: TableContainer,
    private val accountRepository: AccountRepository,
    private val transferRepository: TransferRepository
) {

    private val log = LoggerFactory.getLogger(AccountBalanceAccumulator::class.java)
    private val writeEvents = Flux.create<WriteRowEvent> { sink ->
        val binaryLogReceiver = BinaryLogReceiver(jdbcTemplate, hikariDataSource, tableContainer) {
            log.info("WriteRowEvent next: {}", it)
            sink.next(it)
        }

        binaryLogReceiver.start()
    }

    @PostConstruct
    fun initialize() {
        accumulateBalanceByPastTransfers()
        subscribeTransferWrite()
    }

    private fun accumulateBalanceByPastTransfers() {
        transferRepository.findAll().forEach {
            accountRepository.findByAccountNumber(it.accountNumber)?.apply {
                this.balance += it.amount
            }
        }
    }

    private fun subscribeTransferWrite() {
        writeEvents.subscribe {
            log.info("WriteRowEvent subscribed: {}", it)
        }
    }

}