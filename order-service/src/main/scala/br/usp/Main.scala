package br.usp

import akka.actor.AddressFromURIString
import akka.actor.typed.ActorSystem
import akka.contrib.persistence.mongodb.{MongoReadJournal, ScalaDslMongoReadJournal}
import akka.management.scaladsl.AkkaManagement
import akka.persistence.query.PersistenceQuery
import akka.stream.Materializer
import com.typesafe.config.{Config, ConfigFactory}

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
      implicit val system = ActorSystem[Nothing](Guardian(httpPort), "OrderApp", config)
      implicit val mat = Materializer(system)
      AkkaManagement(system).start()
      OrderConsumer.subscribe("credit-card-authorized")
    }
  }

  private def configWithPort(port: Int): Config =
    ConfigFactory.parseString(
      s"""
       akka.remote.artery.canonical.port = $port
        """).withFallback(ConfigFactory.load())


}