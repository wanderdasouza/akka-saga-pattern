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

  private val sharding = ClusterSharding(system)

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getConsumers = {
    val readJournal =
      PersistenceQuery(system).readJournalFor[CurrentPersistenceIdsQuery](MongoReadJournal.Identifier)
    readJournal
      .currentPersistenceIds()
      .map(id => Await.result(getConsumer(id.split("\\|").last), 3.second).maybeConsumer.get)
      .runFold(Seq.empty[Consumer])((set, consumer) => set.+:(consumer))
  }

  def getConsumer(consumerId: String): Future[GetConsumerResponse] = {
    val entityRef = sharding.entityRefFor(ConsumerPersistence.EntityKey, consumerId)
    entityRef.ask(GetConsumer(_))
  }
  def createConsumer(consumer: Consumer): Future[String] = {
    val id = new ObjectId().toString
    val entityRef = sharding.entityRefFor(ConsumerPersistence.EntityKey, id)
    entityRef.ask(CreateConsumer(consumer, _))
  }
  def deleteConsumer(consumerId: String): Future[String] = {
    val entityRef = sharding.entityRefFor(ConsumerPersistence.EntityKey, consumerId)
    entityRef.ask(DeleteConsumer(_))
  }

  val consumerRoutes: Route =
    pathPrefix("consumers") {
      concat(
        pathEnd {
          concat(
            get {
              onSuccess(getConsumers) { consumers =>
                complete(StatusCodes.OK, Consumers(consumers.toSeq))
              }
            },
            post {
              entity(as[Consumer]) { consumer =>
                onSuccess(createConsumer(consumer)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            })
        },
        path(Segment) { id =>
          concat(
            get {
              rejectEmptyResponse {
                onSuccess(getConsumer(id)) { response =>
                  complete(response.maybeConsumer)
                }
              }
            },
            delete {
              onSuccess(deleteConsumer(id)) { performed =>
                complete((StatusCodes.OK, performed))
              }
            }
          )
        })
    }
}
