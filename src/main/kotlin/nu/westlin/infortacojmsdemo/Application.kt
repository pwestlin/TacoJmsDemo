package nu.westlin.infortacojmsdemo

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jms.annotation.JmsListener
import org.springframework.jms.core.JmsClient
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@SpringBootApplication
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

@Service
class MessageSenderService(
    private val jmsClient: JmsClient,
    private val fooRepository: JdbcFooRepository
) {

    fun send(msg: String) {
        jmsClient
            .destination("demo.queue")
            .send(msg)
        fooRepository.store(msg)
    }

    fun send(msgs: List<String>) {
        msgs.forEach { send(it) }
    }
}

@Repository
class JdbcFooRepository(
    private val jdbcClient: JdbcClient
) {

    @Suppress("JpaQueryApiInspection")
    fun store(msg: String) {
        if ("Foo" in msg) {
            throw RuntimeException("I do not like 'Foo' in messages!")
        }

        jdbcClient
            .sql("INSERT INTO messages (data) VALUES (:msg)")
            .param("msg", msg)
            .update()
    }
}

@Service
class MessageListenerService {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @JmsListener(destination = "demo.queue")
    fun demoQueueListener1(msg: String) {
        logger.info("1: msg: $msg")
    }

/*
    @JmsListener(destination = "demo.queue")
    fun demoQueueListener2(msg: String) {
        logger.info("2: msg: $msg")
    }
*/

    @JmsListener(destination = "demo.queue2")
    fun demoQueue2Listener1(msg: String) {
        logger.info("2-1: msg: $msg")
        Thread.sleep(1_000)
    }
}

@RestController
@RequestMapping("/")
class MessageController(
    private val messageSenderService: MessageSenderService,
) {

    @PostMapping("/messages/{message}")
    fun postMessage(@PathVariable message: String) {
        messageSenderService.send(message)
    }

    @PostMapping("/messages")
    fun postMessages(@RequestParam messages: List<String>) {
        messageSenderService.send(messages)
    }
}
