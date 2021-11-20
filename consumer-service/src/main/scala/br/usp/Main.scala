package br.usp

import akka.actor.AddressFromURIString
import com.typesafe.config.Config
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.management.scaladsl.AkkaManagement
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

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
      implicit val system = ActorSystem[Nothing](Guardian(httpPort), "ConsumerApp", config)
      // AkkaManagement(system).start()
      implicit val mat: Materializer = Materializer(system)
      ConsumerConsumer.subscribe("order-created")
    }
  }

  private def configWithPort(port: Int): Config =
    ConfigFactory.parseString(
      s"""
       akka.remote.artery.canonical.port = $port
        """).withFallback(ConfigFactory.load())


}