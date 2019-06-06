package com.codehumane.accountbalance.transfer

import com.codehumane.accountbalance.account.AccountRepository
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/account/transfer")
class TransferController(
    private val transferRepository: TransferRepository,
    private val accountRepository: AccountRepository
) {

    private val log = LoggerFactory.getLogger(TransferController::class.java)

    @PostMapping
    fun create(@RequestBody command: TransferCommand) {
        transferRepository.save(
            Transfer(
                accountNumber = command.accountNumber,
                createdAt = LocalDateTime.now(),
                amount = command.amount
            )
        )
    }

    /**
     * 대량 추가를 테스트 하기 위한 엔드포인트
     */
    @PostMapping("/bulkcreate")
    fun bulkCreate() {
        log.info("bulk create started")
        val accounts = accountRepository.findAll()
        (1..400).forEach { idx ->
            accounts.forEach { account ->
                create(TransferCommand(account.accountNumber, idx.toLong()))
            }
        }

        log.info("bulk create completed")
    }

    data class TransferCommand(
        val accountNumber: String,
        val amount: Long
    )
}
