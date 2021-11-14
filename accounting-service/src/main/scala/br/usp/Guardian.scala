package br.usp

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Guardian {

  def apply(httpPort: Int): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    ConsumerPersistence.initSharding(context.system)

    val routes = new ConsumerRoutes()(context.system)
    HttpServer.startHttpServer(routes.consumerRoutes, httpPort)(context.system)

    Behaviors.empty
  }
}
