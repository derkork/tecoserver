package com.ancientlightstudios.tecoserver

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.config.environment.Environment
import org.springframework.cloud.config.environment.PropertySource
import org.springframework.cloud.config.server.environment.EnvironmentRepository
import org.springframework.cloud.config.server.environment.FailedToConstructEnvironmentException
import org.springframework.stereotype.Component
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

@Component
class TestContainerEnvironmentRepository : EnvironmentRepository {
    private val log: Logger = LoggerFactory.getLogger(TestContainerEnvironmentRepository::class.java)

    private val runningMySqls = mutableMapOf<String, MySQLContainer<Nothing>>()
    private val runningKafkas = mutableMapOf<String,KafkaContainer>()

    override fun findOne(application: String?, profile: String?, label: String?): Environment {
        try {
            val result = Environment("testcontainers")

            if (label == null) {
                return result
            }

            val containers = label.split(":")

            for (container in containers) {
                when {
                    container.startsWith("mysql") -> {
                        addMySQLConfig(container, result)
                    }
                    container.startsWith("kafka") -> {
                        addKafkaConfig(container, result)
                    }
                    else -> {
                        log.warn("Unsupported container definition: $container")
                    }
                }
            }
            return result
        } catch (e: Exception) {
            throw FailedToConstructEnvironmentException(e.message, e)
        }
    }

    private fun addKafkaConfig(container: String, result: Environment) {
        val parts = container.split("-")
        val version = parts[1]
        val name = parts[2]
        val running = if (!runningKafkas.containsKey(name)) {
            log.info("Starting new Kafka server name: '$name' version: '$version'")
            val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:${version}"))
            kafka.start()
            runningKafkas[name] = kafka
            kafka
        }
        else {
            runningKafkas[name]!!
        }

        result.add(
            PropertySource(
                container, mutableMapOf(
                    "spring.cloud.stream.kafka.binder.brokers=" to running.bootstrapServers
                )
            )
        )
    }

    private fun addMySQLConfig(container: String, result: Environment) {
        val parts = container.split("-")
        val version = parts[1]
        val name = parts[2]
        val running: MySQLContainer<Nothing> = if (!runningMySqls.containsKey(name)) {
            log.info("Starting new MySQL server name: '$name' version: '$version'")
            val mysql = MySQLContainer<Nothing>("mysql:${version}")
            mysql.withDatabaseName(name)
            mysql.withUrlParam("useSSL", "false")
            mysql.withUrlParam("autocommit", "false")
            mysql.withUrlParam("allowPublicKeyRetrieval", "true")
            mysql.withUrlParam("createDatabaseIfNotExist", "true")
            mysql.start()
            runningMySqls[name] = mysql
            mysql
        } else {
            runningMySqls[name]
        }!!

        result.add(
            PropertySource(
                container, mutableMapOf(
                    "spring.datasource.url" to running.jdbcUrl,
                    "spring.datasource.username" to running.username,
                    "spring.datasource.password" to running.password
                )
            )
        )
    }
}