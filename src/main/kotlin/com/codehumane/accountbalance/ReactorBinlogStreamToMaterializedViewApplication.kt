package com.codehumane.accountbalance

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ReactorBinlogStreamToMaterializedViewApplication

fun main(args: Array<String>) {
	runApplication<ReactorBinlogStreamToMaterializedViewApplication>(*args)
}
