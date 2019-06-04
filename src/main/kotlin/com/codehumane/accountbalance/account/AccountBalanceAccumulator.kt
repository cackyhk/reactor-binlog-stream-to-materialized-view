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
import reactor.core.publisher.Mono
import java.time.Duration
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

    /**
     * DB에 쌓여 있는 `Transfer` 정보들을 기반으로 `Account` 잔고 값을 계산한다.
     */
    private fun accumulateBalanceByPastTransfers() {
        transferRepository.findAll().forEach {
            accumulateTransferAmount(it)
        }
    }

    /**
     * 전체 파이프 라인 구성 (쓰기 로그 수신 및 계좌 잔고 계산)
     */
    private fun subscribeTransferWrite() {
        writeEventsSource
            .filter { filterTable(it) }
            .map { mapToTransfer(it) }
            .flatMap { transfer -> completeWithRandomDelay { accumulateTransferAmount(transfer) } }
            .doOnError { terminateOnUnrecoverableError(it) }
            .subscribe()
    }

    /**
     * 대상 테이블 여부 필터링
     */
    private fun filterTable(it: WriteRowEvent) =
        Transfer::class.simpleName!!.toLowerCase() == it.table.table

    /**
     * WriteRowEvent(row 쓰기 binary log)를 Transfer 객체로 변환
     */
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

    /**
     * 0~5초의 지연을 임의로 발생시킨 뒤, 주어진 함수를 수행한다.
     *
     * 처리 시간이 중구 난방으로 달라지더라도,
     * 비동기 `flatMap`이 순서를 보장하는지 여부 확인을 위함.
     */
    private fun completeWithRandomDelay(func: () -> Account): Mono<Account> {
        val delayInSeconds = (Math.random() * 10).toLong() % 4
        log.info("delayInSeconds: $delayInSeconds")

        return Mono
            .delay(Duration.ofSeconds(delayInSeconds))
            .map { func() }
    }

    /**
     * `Transfer` 정보를 기반으로 최종 계좌 잔고를 계산한다.
     */
    private fun accumulateTransferAmount(transfer: Transfer): Account {
        val account = accountRepository.findByAccountNumber(transfer.accountNumber)

        return checkNotNull(account).apply {
            this.balance += transfer.amount
            log.info("account balance accumulated: $account")
        }
    }

    /**
     * 복구 불가능한 오류가 발생한 경우 시스템을 종료한다.
     */
    private fun terminateOnUnrecoverableError(it: Throwable?) {
        log.error("unrecoverable error. system exit", it)
        System.exit(666)
    }

}