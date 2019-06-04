package com.codehumane.accountbalance.account

import org.springframework.stereotype.Repository

@Repository
class AccountRepository {

    private val accounts: Set<Account> = setOf(
        Account("123", 0),
        Account("234", 0),
        Account("345", 0),
        Account("456", 0),
        Account("567", 0)
    )

    private val accountNumberToAccount: Map<String, Account>

    init {
        accountNumberToAccount = accounts.associateBy({ it.accountNumber }, { it })
    }

    fun findByAccountNumber(accountNumber: String): Account? {
        return accountNumberToAccount[accountNumber]
    }

    fun findAll(): Set<Account> {
        return accounts
    }

}