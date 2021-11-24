package br.usp

import akka.Done
import akka.actor.typed.ActorSystem
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import br.usp.serialization.CreditCardAuthorizedToKafka
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import spray.json.JsonParser

import scala.concurrent.Future

object OrderConsumer {

  import br.usp.serialization.JsonFormats.CreditCardAuthorizedToKafkaProtocol._

  val configKafka: Config = ConfigFactory.load().getConfig("akka.kafka.consumer")
  val consumerSettings: ConsumerSettings[String, String] =
    ConsumerSettings(configKafka, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers("localhost:9094")
      .withGroupId("kitchen-consumer")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  def subscribe(topic: String)(implicit mat: Materializer, sys: ActorSystem[_]): Future[Done] = {
    val repository = new OrderRepository(sys)
    Consumer.committableSource(consumerSettings, Subscriptions.topics(topic))
      .map(msg  => {
        // println(s"Pedido criado - ${msg.record.val.convertTo[OrderCreatedToKafka]ue()}")
        val value = JsonParser(msg.record.value())
        val sampleData = value.convertTo[CreditCardAuthorizedToKafka]
        val id = sampleData.orderId
        repository.approveOrder(id)
      })
      .runWith(Sink.ignore)
  }
}
