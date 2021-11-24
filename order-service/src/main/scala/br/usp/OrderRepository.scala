package br.usp

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.contrib.persistence.mongodb.MongoReadJournal
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.scaladsl.CurrentPersistenceIdsQuery
import akka.util.Timeout
import br.usp.domain.Order
import br.usp.domain.OrderDomain.{ApproveOrder, CreateOrder, DeleteOrder, GetOrder, GetOrderResponse}
import org.bson.types.ObjectId

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class OrderRepository(system: ActorSystem[_]) {
  val sharding = ClusterSharding(system)
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getOrders(implicit system: ActorSystem[_]): Future[Seq[Order]] = {
    val readJournal =
      PersistenceQuery(system).readJournalFor[CurrentPersistenceIdsQuery](MongoReadJournal.Identifier)
    readJournal
      .currentPersistenceIds()
      .map(id => Await.result(getOrder(id.split("\\|").last), 2.second).maybeOrder.get)
      .runFold(Seq.empty[Order])((set, order) => set.+:(order))
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

  def approveOrder(orderId: String): Future[String] = {
    val entityRef = sharding.entityRefFor(OrderPersistence.EntityKey, orderId)
    entityRef.ask(ApproveOrder)
  }
}
