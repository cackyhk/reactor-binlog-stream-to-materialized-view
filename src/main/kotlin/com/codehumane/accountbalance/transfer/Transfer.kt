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
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Transfer

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "Transfer(id=$id, accountNumber='$accountNumber', createdAt=$createdAt, amount=$amount)"
    }

}