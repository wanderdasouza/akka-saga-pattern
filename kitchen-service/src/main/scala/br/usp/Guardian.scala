package br.usp

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Guardian {

  def apply(httpPort: Int): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    KitchenPersistence.initSharding(context.system)

    val routes = new KitchenRoutes()(context.system)
    HttpServer.startHttpServer(routes.kitchenRoutes, httpPort)(context.system)

    Behaviors.empty
  }
}
