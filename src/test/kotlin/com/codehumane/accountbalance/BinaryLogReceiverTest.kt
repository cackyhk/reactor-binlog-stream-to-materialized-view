package com.codehumane.accountbalance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URI

class BinaryLogReceiverTest {

    @Test
    fun name() {
        val uri = URI.create("jdbc:mysql://localhost:3306/test".removePrefix("jdbc:"))
        assertEquals("localhost", uri.host)
    }
}