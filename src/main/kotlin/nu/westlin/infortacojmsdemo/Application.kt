package nu.westlin.infortacojmsdemo

import com.tibco.tibjms.TibjmsConnectionFactory
import jakarta.jms.ConnectionFactory
import jakarta.jms.Session
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.server.context.WebServerInitializedEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jms.annotation.JmsListener
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
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

    fun send(queue: String, msg: String) {
        jmsClient
            .destination(queue)
            .send(msg)
        fooRepository.store(msg)
    }

    fun send(queue: String, msgs: List<String>) {
        msgs.forEach { send(queue, it) }
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

    private lateinit var port: String

    @EventListener
    fun webServerInitializedEvent(event: WebServerInitializedEvent) {
        port = event.webServer.port.toString()
    }

    @JmsListener(destination = "LM.UTV.PEVEST.NONEXCLUSIVE")
    fun nonExclusiveListener(msg: String) {
        logger.info("$port - nonExclusiveListener: msg: $msg")
        Thread.sleep(5_000)
        logger.info("$port - nonExclusiveListener: Done with msg: $msg")
    }

    @JmsListener(destination = "LM.UTV.PEVEST.EXCLUSIVE")
    fun exclusiveListener(msg: String) {
        logger.info("$port - exclusiveListener: msg: $msg")
        Thread.sleep(5_000)
        logger.info("$port - exclusiveListener: Done with msg: $msg")
    }
}

@RestController
@RequestMapping("/")
class MessageController(
    private val messageSenderService: MessageSenderService,
) {

    @PostMapping("/messages/{message}/{queue}")
    fun postMessage(@PathVariable message: String, @PathVariable queue: String) {
        messageSenderService.send(queue, message)
    }

    @PostMapping("/messages/{queue}")
    fun postMessages(@PathVariable queue: String, @RequestParam messages: List<String>) {
        messageSenderService.send(queue, messages)
    }
}

@Configuration
class JmsConfiguration {

    @Bean
    fun connectionFactory(
        @Value($$"${tibco.ems.url}") url: String,
        @Value($$"${tibco.ems.username}") username: String,
        @Value($$"${tibco.ems.password}") password: String
    ): ConnectionFactory {
        return TibjmsConnectionFactory(url).apply {
            setUserName(username)
            setUserPassword(password)
        }
    }

    @Bean
    fun jmsClient(connectionFactory: ConnectionFactory): JmsClient = JmsClient.create(connectionFactory)

    @Bean
    fun jmsListenerContainerFactory(
        connectionFactory: ConnectionFactory
    ): DefaultJmsListenerContainerFactory {
        return DefaultJmsListenerContainerFactory().apply {
            setConnectionFactory(connectionFactory)
            setSessionTransacted(true)
            // Nedan behövs inte när man satt setSessionTransacted(true), åtminstone inte i Tibco.
            // Vi sätter det ändå för att vara lite framtidssäkra, ex. när/om vi byter till Artemis. :)
            setSessionAcknowledgeMode(Session.SESSION_TRANSACTED)
        }
    }
}
