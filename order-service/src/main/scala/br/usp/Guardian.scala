package br.usp

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Guardian {

  def apply(httpPort: Int): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    OrderPersistence.initSharding(context.system)

    val routes = new OrderRoutes()(context.system)
    HttpServer.startHttpServer(routes.orderRoutes, httpPort)(context.system)

    Behaviors.empty
  }
}
