package com.codehumane.accountbalance.transfer

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/account/transfer")
class TransferController(private val transferRepository: TransferRepository) {

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

    data class TransferCommand(
        val accountNumber: String,
        val amount: Long
    )
}