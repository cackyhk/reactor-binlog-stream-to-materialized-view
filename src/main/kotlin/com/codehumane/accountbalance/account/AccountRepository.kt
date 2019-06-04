package com.codehumane.accountbalance.account

import com.codehumane.accountbalance.transfer.TransferRepository
import org.springframework.stereotype.Repository
import javax.annotation.PostConstruct

@Repository
class AccountRepository(private val transferRepository: TransferRepository) {

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

    @PostConstruct
    fun sumByPastTransfers() {
        transferRepository.findAll().forEach {
            val transferAmount = it.amount
            findByAccountNumber(it.accountNumber)?.apply {
                this.balance += transferAmount
            }
        }
    }

    fun findByAccountNumber(accountNumber: String): Account? {
        return accountNumberToAccount[accountNumber]
    }

}