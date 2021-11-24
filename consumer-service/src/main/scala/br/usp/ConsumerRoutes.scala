package br.usp

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import br.usp.domain.{Consumer, ConsumerRequest, Consumers}


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
              entity(as[ConsumerRequest]) { consumerRequest =>
                onSuccess(repository.createConsumer(consumerRequest)) { performed =>
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
