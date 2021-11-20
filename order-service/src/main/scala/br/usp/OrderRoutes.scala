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

  private val sharding = ClusterSharding(system)

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getOrders: Future[Seq[Order]] = {
    val readJournal =
      PersistenceQuery(system).readJournalFor[CurrentPersistenceIdsQuery](MongoReadJournal.Identifier)
    readJournal
      .currentPersistenceIds()
      .map(id => Await.result(getOrder(id.split("\\|").last), 3.second).maybeOrder.get)
      .runFold(Seq.empty[Order])((set, user) => set.+:(user))
  }

  def getOrder(orderId: String): Future[GetOrderResponse] = {
    val entityRef = sharding.entityRefFor(OrderPersistence.EntityKey, orderId)
    entityRef.ask(GetOrder)
  }
  def createOrder(order: Order): Future[String] = {
    val id = new ObjectId().toString
    val entityRef = sharding.entityRefFor(OrderPersistence.EntityKey, id)
    entityRef.ask(CreateOrder(order, _))
  }
  def deleteOrder(orderId: String): Future[String] = {
    val entityRef = sharding.entityRefFor(OrderPersistence.EntityKey, orderId)
    entityRef.ask(DeleteOrder)
  }

  val orderRoutes: Route =
    pathPrefix("orders") {
      concat(
        pathEnd {
          concat(
            get {
              onSuccess(getOrders) { orders =>
                complete(StatusCodes.OK, Orders(orders.toSeq))
              }
            },
            post {
              entity(as[OrderRequest]) { o =>
                val order = Order(o.consumerId, OrderState.PENDING)
                onSuccess(createOrder(order)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            })
        },
        path(Segment) { id =>
          concat(
            get {
              rejectEmptyResponse {
                onSuccess(getOrder(id)) { response =>
                  complete(response.maybeOrder)
                }
              }
            },
            delete {
              onSuccess(deleteOrder(id)) { performed =>
                complete((StatusCodes.OK, performed))
              }
            }
          )
        })
    }
}
