package com.codehumane.accountbalance.account

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/account")
class AccountController(private val accountRepository: AccountRepository) {

    @GetMapping
    fun get(): Set<Account> {
        return accountRepository.findAll()
    }
}