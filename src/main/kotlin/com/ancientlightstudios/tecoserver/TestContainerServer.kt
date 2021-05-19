package com.ancientlightstudios.tecoserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.config.server.EnableConfigServer

@SpringBootApplication
@EnableConfigServer
class TestContainerServer

fun main(args: Array<String>) {
    runApplication<TestContainerServer>(*args)
}
