package br.usp

import akka.Done
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onSuccess}
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import br.usp.domain.Consumers
import br.usp.serialization.OrderCreatedToKafka
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import spray.json.DefaultJsonProtocol.StringJsonFormat
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ConsumerConsumer {

  import br.usp.serialization.JsonFormats.OrderCreatedToKafkaProtocol._

  val configKafka: Config = ConfigFactory.load().getConfig("akka.kafka.consumer")
  val consumerSettings: ConsumerSettings[String, String] =
    ConsumerSettings(configKafka, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers("localhost:9094")
      .withGroupId("consumer-group")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  def subscribe(topic: String)(implicit mat: Materializer, sys: ActorSystem[_]): Future[Done] = {
    val repository = new ConsumerRepository(sys)
    Consumer.committableSource(consumerSettings, Subscriptions.topics(topic))
      .map(msg  => {
          // println(s"Pedido criado - ${msg.record.val.convertTo[OrderCreatedToKafka]ue()}")
          val value = msg.record.value()
          val sampleData = value.parseJson.convertTo[OrderCreatedToKafka]
          val id = sampleData.consumerId
          repository.getConsumer(id).onComplete(consumer =>
            println(s"\n\nUsuario verificado - ${consumer.get.maybeConsumer.get.name}\n\n")
          )
          }
      )
      .runWith(Sink.ignore)
  }
}
