package com.codehumane.accountbalance.account

import org.springframework.stereotype.Repository

@Repository
class AccountRepository {

    private val accounts: Set<Account> = setOf(
        Account("1", 0),
        Account("2", 0),
        Account("3", 0),
        Account("4", 0),
        Account("5", 0),
        Account("6", 0),
        Account("7", 0),
        Account("8", 0),
        Account("9", 0),
        Account("10", 0),
        Account("11", 0),
        Account("12", 0),
        Account("13", 0),
        Account("14", 0),
        Account("15", 0),
        Account("16", 0)
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