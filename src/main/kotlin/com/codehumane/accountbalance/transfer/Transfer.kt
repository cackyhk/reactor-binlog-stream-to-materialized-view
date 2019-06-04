package com.codehumane.accountbalance.transfer

import java.time.LocalDateTime
import javax.persistence.*

@Entity
class Transfer(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val accountNumber: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime,

    @Column(nullable = false)
    val amount: Long
)