package br.usp

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.contrib.persistence.mongodb.MongoReadJournal
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.scaladsl.CurrentPersistenceIdsQuery
import akka.util.Timeout
import br.usp.domain.Ticket
import br.usp.domain.TicketDomain.{CreateTicket, DeleteTicket, GetTicket, GetTicketResponse}
import org.bson.types.ObjectId

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class KitchenRepository(system: ActorSystem[_]) {
  val sharding = ClusterSharding(system)
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getTickets(implicit system: ActorSystem[_]) = {
    val readJournal =
      PersistenceQuery(system).readJournalFor[CurrentPersistenceIdsQuery](MongoReadJournal.Identifier)
    readJournal
      .currentPersistenceIds()
      .map(id => Await.result(getTicket(id.split("\\|").last), 3.second).maybeTicket.get)
      .runFold(Seq.empty[Ticket])((set, ticket) => set.+:(ticket))
  }

  def getTicket(ticketId: String): Future[GetTicketResponse] = {
    val entityRef = sharding.entityRefFor(KitchenPersistence.EntityKey, ticketId)
    entityRef.ask(GetTicket)
  }
  def createTicket(ticket: Ticket): Future[String] = {
    val id = new ObjectId().toString
    val entityRef = sharding.entityRefFor(KitchenPersistence.EntityKey, id)
    entityRef.ask(CreateTicket(ticket, _))
  }
  def deleteTicket(consumerId: String): Future[String] = {
    val entityRef = sharding.entityRefFor(KitchenPersistence.EntityKey, consumerId)
    entityRef.ask(DeleteTicket)
  }
}
