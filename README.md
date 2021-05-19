# TecoServer

## What is it?

TecoServer is a Spring Cloud configuration server that simplifies local development. Let's say you need to work on a bunch of microservices implemented with Spring Boot and these need things like a MySQL database or a Kafka server to work. Usually what you do is you will set up a server manually, by using Docker or a VM. And all the developers in your team will have to do this as well. 

With TecoServer, all you need to do is run TecoServer and configure your microservices to fetch part of their configuration from it. TecoServer will then automatically fire up a MySQL or Kafka (for now) and give the connectivity information to your program.

## How does it work?
TecoServer uses the excellent Testcontainers library to start up servers in docker containers on the fly. Testcontainers already has great support for automated integration tests, but it can actually also be used to simply start a server for local development. 

TecoServer is a Spring Cloud configuration server, that any Spring-powered application can use to fetch configuration from. By using a special configuration label you can instruct TecoServer to spawn up a Docker container and return connectivity information for this as Spring Boot configuration properties.

## Requirements
Java 8 or later to start TecoServer. A recent Docker version must be installed on the same host as TecoServer.

## How to use it?
It's really easy. Download the [JAR file](https://github.com/derkork/tecoserver/releases/tag/snapshot) to your development machine and then simply start it:

```bash
java -jar tecoserver.jar
```

Now in your Spring Boot powered app, add the following dependency if you don't already have it:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
    <version>3.0.3</version> <!-- Use the version relevant for your project, this is just an example -->
</dependency>
```
This adds the libraries that enable Spring to fetch configurations from TecoServer. Finally, you will need to tell your application where TecoServer is located and what type of containers you need. To do this it's best to create a profile for local development and then configure TecoServer there. So go ahead and create an `application-local.properties` if you don't already have one and add the following lines to it:

```properties
# Tell Spring we want to fetch info from a config server.
spring.config.import=configserver:
# Tell Spring where TecoServer is running. By default it runs on port 54321.
spring.cloud.config.uri=http://localhost:54321
# Tell TecoServer what you need. In this case we need a MySQL version 8, named "mydb" and a Kafka server version 6.0.2 named "shared".
spring.cloud.config.label=mysql-8-mydb:kafka-6.0.2-shared
```

Now when you start up your application, it will connect to TecoServer and try to fetch configuration. TecoServer will inspect your requirements and spawn up the required Docker containers. It will then return Spring Boot configuration settings, so your application can connect to the spawned servers.

## Container lifecycle
Each spawned container will be remembered by its name. So if you restart your application, the same container will be used again. This also makes it easy to share a container between two applications (e.g. if you have two microservices that need to communicate with each other using Kafka) - simply configure the same name on both services and both will connect to the same server. The containers will only shut down when you shut down TecoServer.

## Configuration
There is not a lot that can be configured.
### Changing the port of TecoServer
You can run TecoServer on a different port than 54321. To do so, simply add a system property to your startup script - e.g. this would start TecoServer on Port 5100:

```bash
java -Dserver.port=5100 tecoserver.jar
```

## Supported Server Types
### MySQL
MySQL server. Uses the `mysql` docker image in the specified version. The MySQL will not use SSL. Returns the following Spring configuration settings:

| Setting       | Contains    |
| :------------- | :---------- | 
| `spring.datasource.url` | JDBC Connection URL to the database. | 
| `spring.datasource.username` | Username for connection. | 
| `spring.datasource.password` | Password for connection. | 

### Kafka
Kafka with integrated ZooKeeper server. Uses the `confluentinc/cp-kafka` docker image in the specified version. The Kafka will use a plaintext connection.

| Setting       | Contains    |
| :------------- | :---------- | 
| `spring.cloud.stream.kafka.binder.brokers` | Bootstrap URL for the Kafka client. | 
