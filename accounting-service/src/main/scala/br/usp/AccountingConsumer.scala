package br.usp

import akka.Done
import akka.actor.typed.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import br.usp.domain.Account
import br.usp.serialization.{ConsumerVerifiedToKafka, CreditCardAuthorizedToKafka, OrderCreatedToKafka, TicketCreatedToKafka}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import spray.json.JsObject.empty.asJsObject
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

object AccountingConsumer {

  import br.usp.serialization.JsonFormats.OrderCreatedToKafkaProtocol._
  import br.usp.serialization.JsonFormats.ConsumerVerifiedToKafkaProtocol._
  import br.usp.serialization.JsonFormats.TicketCreatedToKafkaProtocol._

  val configKafka: Config = ConfigFactory.load().getConfig("akka.kafka.consumer")
  val consumerSettings: ConsumerSettings[String, String] =
    ConsumerSettings(configKafka, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers("localhost:9094")
      .withGroupId("account-consumer")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  def subscribe(topics: Set[String])(implicit mat: Materializer, sys: ActorSystem[_]): Future[Done] = {
    val repository = new AccountRepository(sys)
    Consumer.committableSource(consumerSettings, Subscriptions.topics(topics))
      .map(msg  => {
        // println(s"Pedido criado - ${msg.record.val.convertTo[OrderCreatedToKafka]ue()}")
        val value = msg.record.value().parseJson
        value.asJsObject.fields("eventType") match {
          case JsString("OrderCreated") =>
            val event = value.convertTo[OrderCreatedToKafka]
            val orderId = event.orderId
            val consumerId = event.consumerId
            repository.createAccount(Account(orderId, consumerId))
          case JsString("ConsumerVerified") =>
            val event = value.convertTo[ConsumerVerifiedToKafka]
            val orderId = event.orderId
            val consumerId = event.consumerId
            repository.verifyConsumer(Account(orderId, consumerId))
          case JsString("TicketCreated") =>
            val event = value.convertTo[TicketCreatedToKafka]
            val orderId = event.orderId
            val consumerId = event.consumerId
            repository.approveTicket(Account(orderId, consumerId))

            // AccountingProducer.publish("credit-card-authorized", CreditCardAuthorizedToKafka(orderId))
        }
      })
      .runWith(Sink.ignore)
  }
}
