package com.codehumane.accountbalance.account

import com.codehumane.accountbalance.binlog.BinaryLogReceiver
import com.codehumane.accountbalance.binlog.ColumnValuesConverter
import com.codehumane.accountbalance.binlog.WriteRowEvent
import com.codehumane.accountbalance.binlog.schema.TableContainer
import com.codehumane.accountbalance.transfer.Transfer
import com.codehumane.accountbalance.transfer.TransferRepository
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.time.LocalDateTime
import javax.annotation.PostConstruct

@Service
class AccountBalanceAccumulator(
    jdbcTemplate: JdbcTemplate,
    hikariDataSource: HikariDataSource,
    tableContainer: TableContainer,
    private val accountRepository: AccountRepository,
    private val transferRepository: TransferRepository,
    private val columnValuesConverter: ColumnValuesConverter
) {

    private val log = LoggerFactory.getLogger(AccountBalanceAccumulator::class.java)
    private val writeEventsSource = Flux.create<WriteRowEvent> { sink ->
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
        writeEventsSource
            .filter { "transfer" == it.table.table }
            .map { mapToTransfer(it) }
            .subscribe { accumulateTransferAmount(it) }
    }

    private fun accumulateTransferAmount(transfer: Transfer) {
        accountRepository.findByAccountNumber(transfer.accountNumber)?.apply {
            this.balance += transfer.amount
        }
    }

    private fun mapToTransfer(event: WriteRowEvent): Transfer {
        return columnValuesConverter.convert(event).let {

            Transfer(
                it["id"]!!.toLong(),
                it["account_number"]!!,
                LocalDateTime.parse(it["created_at"])!!,
                it["amount"]!!.toLong()
            )
        }
    }

}