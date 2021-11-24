package br.usp

import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import br.usp.serialization.JsonFormats.KafkaEventFormat
import br.usp.serialization.KafkaEvent
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import spray.json.enrichAny

object ConsumerProducer {

  val configKafka: Config = ConfigFactory.load().getConfig("akka.kafka.producer")
  val producerSettings: ProducerSettings[String,String]  = ProducerSettings[String, String](configKafka, new StringSerializer, new StringSerializer)
    .withBootstrapServers("localhost:9094")

  def publish(topic: String, event: KafkaEvent)(implicit mat: Materializer): Unit = {
    Source
      .single(new ProducerRecord(topic, "key", event.toJson.toString()))
      .runWith(Producer.plainSink(producerSettings))
  }
}
