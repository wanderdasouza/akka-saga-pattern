package br.usp

import akka.actor.AddressFromURIString
import akka.actor.typed.ActorSystem
import akka.contrib.persistence.mongodb.MongoReadJournal
import akka.kafka.ProducerSettings
import akka.management.scaladsl.AkkaManagement
import akka.persistence.query.{NoOffset, Offset, PersistenceQuery}
import akka.persistence.query.scaladsl.CurrentEventsByTagQuery
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.typesafe.config.{Config, ConfigFactory}
import br.usp.serialization.JsonFormats.KafkaEventFormat
import br.usp.serialization.OrderCreatedToKafka
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import br.usp.domain.OrderDomain._
import spray.json.enrichAny

import scala.collection.JavaConverters._


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
    }
  }

  private def configWithPort(port: Int): Config =
    ConfigFactory.parseString(
      s"""
       akka.remote.artery.canonical.port = $port
        """).withFallback(ConfigFactory.load())


}