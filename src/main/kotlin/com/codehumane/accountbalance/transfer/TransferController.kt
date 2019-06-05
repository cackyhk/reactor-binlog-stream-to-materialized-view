package com.codehumane.accountbalance.transfer

import com.codehumane.accountbalance.account.AccountRepository
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
        accountRepository.findAll().forEach { account ->
            (0..100).forEach { idx ->
                create(TransferCommand(account.accountNumber, idx.toLong()))
            }
        }
    }

    data class TransferCommand(
        val accountNumber: String,
        val amount: Long
    )
}