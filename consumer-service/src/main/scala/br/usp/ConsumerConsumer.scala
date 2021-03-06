package br.usp

import akka.Done
import akka.actor.typed.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import br.usp.serialization.{ConsumerVerifiedToKafka, OrderCreatedToKafka}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ConsumerConsumer {

  import br.usp.serialization.JsonFormats.OrderCreatedToKafkaProtocol._

  val configKafka: Config = ConfigFactory.load().getConfig("akka.kafka.consumer")
  val consumerSettings: ConsumerSettings[String, String] =
    ConsumerSettings(configKafka, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers("localhost:9094")
      .withGroupId("consumer-consumer")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  def subscribe(topic: String)(implicit mat: Materializer, sys: ActorSystem[_]): Future[Done] = {
    val repository = new ConsumerRepository(sys)
    Consumer.committableSource(consumerSettings, Subscriptions.topics(topic))
      .map(msg  => {
          val value = msg.record.value()
          val sampleData = value.parseJson.convertTo[OrderCreatedToKafka]
          val consumerId = sampleData.consumerId
          val orderId = sampleData.orderId
          repository.getConsumer(consumerId).onComplete { consumer =>
            consumer.get.maybeConsumer match {
              case Some(_) =>
                ConsumerProducer.publish("consumer-verified", ConsumerVerifiedToKafka("ConsumerVerified", consumerId, orderId))
            }

          }
          })
      .runWith(Sink.ignore)
  }
}
