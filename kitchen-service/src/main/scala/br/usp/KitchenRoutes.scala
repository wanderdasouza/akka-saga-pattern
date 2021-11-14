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
import akka.util.Timeout
import br.usp.domain.{Ticket, Tickets}
import br.usp.domain.TicketDomain.{CreateTicket, DeleteTicket, GetTicket, GetTicketResponse}
import org.bson.types.ObjectId

import scala.concurrent.duration.DurationInt


class KitchenRoutes(implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import br.usp.serialization.JsonFormats._
  //#import-json-formats

  private val sharding = ClusterSharding(system)

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getTickets = {
    val readJournal =
      PersistenceQuery(system).readJournalFor[CurrentPersistenceIdsQuery](MongoReadJournal.Identifier)
    readJournal
      .currentPersistenceIds()
      .map(id => Await.result(getTicket(id.split("\\|").last), 3.second).maybeTicket.get)
      .runFold(Seq.empty[Ticket])((set, consumer) => set.+:(consumer))
  }

  def getTicket(ticketId: String): Future[GetTicketResponse] = {
    val entityRef = sharding.entityRefFor(KitchenPersistence.EntityKey, ticketId)
    entityRef.ask(GetTicket(_))
  }
  def createTicket(ticket: Ticket): Future[String] = {
    val id = new ObjectId().toString
    val entityRef = sharding.entityRefFor(KitchenPersistence.EntityKey, id)
    entityRef.ask(CreateTicket(ticket, _))
  }
  def deleteTicket(consumerId: String): Future[String] = {
    val entityRef = sharding.entityRefFor(KitchenPersistence.EntityKey, consumerId)
    entityRef.ask(DeleteTicket(_))
  }

  val kitchenRoutes: Route =
    pathPrefix("tickets") {
      concat(
        pathEnd {
          concat(
            get {
              onSuccess(getTickets) { tickets =>
                complete(StatusCodes.OK, Tickets(tickets.toSeq))
              }
            },
            post {
              entity(as[Ticket]) { ticket =>
                onSuccess(createTicket(ticket)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            })
        },
        path(Segment) { id =>
          concat(
            get {
              rejectEmptyResponse {
                onSuccess(getTicket(id)) { response =>
                  complete(response.maybeTicket)
                }
              }
            },
            delete {
              onSuccess(deleteTicket(id)) { performed =>
                complete((StatusCodes.OK, performed))
              }
            }
          )
        })
    }
}
