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
import br.usp.domain.{Account,  Accounts}
import br.usp.domain.AccountDomain._
import org.bson.types.ObjectId

import scala.concurrent.duration.DurationInt


class AccountingRoutes(implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import br.usp.serialization.JsonFormats._
  //#import-json-formats
  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  private val repository = new AccountRepository(system)


  val accountRoutes: Route =
    pathPrefix("accounts") {
      concat(
        pathEnd {
          concat(
            get {
              onSuccess(repository.getAccounts) { accounts =>
                complete(StatusCodes.OK, Accounts(accounts.toSeq))
              }
            },
            post {
              entity(as[Account]) { account =>
                onSuccess(repository.createAccount(account)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            })
        },
        path(Segment) { id =>
          concat(
            get {
              rejectEmptyResponse {
                onSuccess(repository.getAccount(id)) { response =>
                  complete(response.maybeAccount)
                }
              }
            },
            delete {
              onSuccess(repository.deleteAccount(id)) { performed =>
                complete((StatusCodes.OK, performed))
              }
            }
          )
        })
    }
}
