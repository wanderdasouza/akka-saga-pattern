package br.usp

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.contrib.persistence.mongodb.MongoReadJournal
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.scaladsl.CurrentPersistenceIdsQuery
import akka.util.Timeout
import br.usp.domain.Account
import br.usp.domain.AccountDomain.{ApproveTicket, CreateAccount, DeleteAccount, GetAccount, GetAccountResponse, VerifyConsumer}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class AccountRepository(system: ActorSystem[_]) {
  val sharding = ClusterSharding(system)
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))


  def getAccounts(implicit system: ActorSystem[_]): Future[Seq[Account]] = {
    val readJournal =
      PersistenceQuery(system).readJournalFor[CurrentPersistenceIdsQuery](MongoReadJournal.Identifier)
    readJournal
      .currentPersistenceIds()
      .map(id => Await.result(getAccount(id.split("\\|").last), 2.second).maybeAccount.get)
      .runFold(Seq.empty[Account])((set, account) => set.+:(account))
  }

  def getAccount(accountId: String): Future[GetAccountResponse] = {
    val entityRef = sharding.entityRefFor(AccountingPersistence.EntityKey, accountId)
    entityRef.ask(GetAccount)
  }

  def createAccount(account: Account): Future[String] = {
    val id = account.orderId + ">" + account.consumerId
    val entityRef = sharding.entityRefFor(AccountingPersistence.EntityKey, id)
    entityRef.ask(CreateAccount(account, _))
  }

  def deleteAccount(accountId: String): Future[String] = {
    val entityRef = sharding.entityRefFor(AccountingPersistence.EntityKey, accountId)
    entityRef.ask(DeleteAccount)
  }

  def approveTicket(account: Account): Future[String] = {
    val id = account.orderId + ">" + account.consumerId
    val entityRef = sharding.entityRefFor(AccountingPersistence.EntityKey, id)
    entityRef.ask(ApproveTicket)
  }

  def verifyConsumer(account: Account): Future[String] = {
    val id = account.orderId + ">" + account.consumerId
    val entityRef = sharding.entityRefFor(AccountingPersistence.EntityKey, id)
    entityRef.ask(VerifyConsumer)
  }


}

