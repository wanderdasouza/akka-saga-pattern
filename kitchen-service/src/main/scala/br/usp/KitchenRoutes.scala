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

  val repository = new KitchenRepository(system)

  val kitchenRoutes: Route =
    pathPrefix("tickets") {
      concat(
        pathEnd {
          concat(
            get {
              onSuccess(repository.getTickets) { tickets =>
                complete(StatusCodes.OK, Tickets(tickets.toSeq))
              }
            },
            post {
              entity(as[Ticket]) { ticket =>
                onSuccess(repository.createTicket(ticket)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            })
        },
        path(Segment) { id =>
          concat(
            get {
              rejectEmptyResponse {
                onSuccess(repository.getTicket(id)) { response =>
                  complete(response.maybeTicket)
                }
              }
            },
            delete {
              onSuccess(repository.deleteTicket(id)) { performed =>
                complete((StatusCodes.OK, performed))
              }
            }
          )
        })
    }
}
