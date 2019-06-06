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
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import reactor.core.publisher.TopicProcessor
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
    private val writeEventsSource = Flux.create<WriteRowEvent>({ sink ->
        val binaryLogReceiver = BinaryLogReceiver(jdbcTemplate, hikariDataSource, tableContainer) {
            sink.next(it)
        }

        binaryLogReceiver.start()
    }, FluxSink.OverflowStrategy.ERROR)

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
        val topicProcessor = TopicProcessor.create<AccumulateTrace>()

        /**
         * binary log event 수신
         */
        writeEventsSource
            .filter { filterTable(it) } // 관심 있는 테이블 이벤트만 수신
            .map { mapToTransfer(it) } // 이체 이력으로 변환
            .flatMapSequential<Transfer>({ delayRandomly(it) }, 512, 32) // 비동기로 처리하되 순서를 보장하기
//            .log() // 로그 남기기 (편하다. 스레드 이름을 통해 병렬로 실행은 되는지, 순서 보장은 되고 있는지 확인 가능)
            .map { AccumulateTrace(it, accumulateTransferAmount(it)) } // 이체 이력으로 계좌 잔고 계산
            .doOnError { terminateOnUnrecoverableError(it) } // 에러 처리
            .subscribe(topicProcessor) // 토픽에 이벤트 전달

        /**
         * 수신된 이벤트를 10개의 스레드가 병렬로 나누어 처리하기.
         * 단, 각 스레드 별로 들어온 이벤트는 차례대로 처리
         */
        (1..16).forEach { idx ->
            Flux.from(topicProcessor) // 토픽 이벤트 수신
                .filter { it.account.accountNumber == idx.toString() } // 병렬 구성
                .delayElements(Duration.ofMillis((Math.random() * 10).toLong())) // 순서 보장이 잘 되는지 확인하기 위해 delay
                .map { it.transfer }
                .log() // 잘 되는지 로그로 확인
                .subscribe() // 고고씽
        }
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
     * 0~5초의 지연을 임의로 발생시킨 뒤 주어진 값을 Mono 형태로 반환한다.
     *
     * 처리 시간이 중구 난방으로 달라지더라도,
     * 비동기 `flatMap`이 순서를 보장하는지 여부 확인을 위함.
     */
    private fun <T> delayRandomly(value: T): Mono<T> {
        val delayInMilliseconds = (Math.random() * 10).toLong() // 100ms까지 지연을 허용한다고 가정

        return Mono
            .delay(Duration.ofMillis(delayInMilliseconds))
            .map { value }
    }

    /**
     * `Transfer` 정보를 기반으로 최종 계좌 잔고를 계산한다.
     */
    private fun accumulateTransferAmount(transfer: Transfer): Account {
        val account = accountRepository.findByAccountNumber(transfer.accountNumber)

        return checkNotNull(account).apply {
            this.balance += transfer.amount
//            log.info("account balance accumulated: $account")
        }
    }

    /**
     * 복구 불가능한 오류가 발생한 경우 시스템을 종료한다.
     */
    private fun terminateOnUnrecoverableError(it: Throwable?) {
        log.error("unrecoverable error. system exit", it)
        System.exit(666)
    }

    data class AccumulateTrace(
        val transfer: Transfer,
        val account: Account
    )
}