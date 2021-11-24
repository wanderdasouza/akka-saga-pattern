package br.usp

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Guardian {

  def apply(httpPort: Int): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    AccountingPersistence.initSharding(context.system)

    val routes = new AccountingRoutes()(context.system)
    HttpServer.startHttpServer(routes.accountRoutes, httpPort)(context.system)

    Behaviors.empty
  }
}
