package com.codehumane.accountbalance

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/account/balance")
class AccountBalanceController(private val accountBalanceService: AccountBalanceService) {

    @GetMapping
    fun get(): String {
        return accountBalanceService.get()
    }
}