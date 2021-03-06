package br.usp

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.{Await, Future}
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.contrib.persistence.mongodb.MongoReadJournal
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.scaladsl.CurrentPersistenceIdsQuery
import akka.stream.scaladsl.Source
import akka.util.Timeout
import br.usp.domain.{Order, OrderRequest, OrderState, Orders}
import br.usp.domain.OrderDomain._
import org.bson.types.ObjectId

import scala.concurrent.duration.DurationInt


class OrderRoutes(implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import br.usp.serialization.JsonFormats._
  //#import-json-formats

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  private val repository = new OrderRepository(system)

  val orderRoutes: Route =
    pathPrefix("orders") {
      concat(
        pathEnd {
          concat(
            get {
              onSuccess(repository.getOrders) { orders =>
                complete(StatusCodes.OK, Orders(orders.toSeq))
              }
            },
            post {
              entity(as[OrderRequest]) { o =>
                val order = Order(o.consumerId, OrderState.PENDING)
                onSuccess(repository.createOrder(order)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            })
        },
        path(Segment) { id =>
          concat(
            get {
              rejectEmptyResponse {
                onSuccess(repository.getOrder(id)) { response =>
                  complete(response.maybeOrder)
                }
              }
            },
            delete {
              onSuccess(repository.deleteOrder(id)) { performed =>
                complete((StatusCodes.OK, performed))
              }
            }
          )
        })
    }
}
