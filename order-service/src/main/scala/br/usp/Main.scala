package br.usp

import akka.actor.AddressFromURIString
import com.typesafe.config.Config
import akka.actor.typed.ActorSystem
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.management.scaladsl.AkkaManagement
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global


//#main-class
object Main {

  //#start-http-server
  def main(args: Array[String]): Unit = {


    val seedNodePorts = ConfigFactory.load().getStringList("akka.cluster.seed-nodes")
      .asScala
      .flatMap { case AddressFromURIString(s) => s.port }


    val ports = args.headOption match {
      case Some(port) => Seq(port.toInt)
      case None => seedNodePorts
    }


    ports.foreach { port =>
      val httpPort =
        if (port > 0) 10000 + port // offset from akka port
        else 0 // let OS decide

      val config = configWithPort(port)
      val system = ActorSystem[Nothing](Guardian(httpPort), "OrderApp", config)
      AkkaManagement(system).start()
      val configKafka = ConfigFactory.load().getConfig("akka.kafka.consumer")
      implicit val mat: Materializer = Materializer(system)
      val consumerSettings =
        ConsumerSettings(configKafka, new StringDeserializer, new StringDeserializer)
          .withBootstrapServers("localhost:9094")
          .withGroupId("consumer-group")
          .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

      Consumer.committableSource(consumerSettings, Subscriptions.topics("user-created"))
        .map(msg  => println(msg.record.value())
        )
        .runWith(Sink.ignore)
    }
  }

  private def configWithPort(port: Int): Config =
    ConfigFactory.parseString(
      s"""
       akka.remote.artery.canonical.port = $port
        """).withFallback(ConfigFactory.load())


}