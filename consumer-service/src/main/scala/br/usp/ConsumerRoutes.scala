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
import br.usp.domain.{Consumer,  Consumers}
import br.usp.domain.ConsumerDomain._
import org.bson.types.ObjectId

import scala.concurrent.duration.DurationInt


class ConsumerRoutes(implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import br.usp.serialization.JsonFormats._
  //#import-json-formats
  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  val repository: ConsumerRepository = new ConsumerRepository(system)

  val consumerRoutes: Route =
    pathPrefix("consumers") {
      concat(
        pathEnd {
          concat(
            get {
              onSuccess(repository.getConsumers) { consumers =>
                complete(StatusCodes.OK, Consumers(consumers.toSeq))
              }
            },
            post {
              entity(as[Consumer]) { consumer =>
                onSuccess(repository.createConsumer(consumer)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            })
        },
        path(Segment) { id =>
          concat(
            get {
              rejectEmptyResponse {
                onSuccess(repository.getConsumer(id)) { response =>
                  complete(response.maybeConsumer)
                }
              }
            },
            delete {
              onSuccess(repository.deleteConsumer(id)) { performed =>
                complete((StatusCodes.OK, performed))
              }
            }
          )
        })
    }
}
