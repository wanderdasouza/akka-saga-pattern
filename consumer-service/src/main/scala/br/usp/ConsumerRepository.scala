package br.usp

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.contrib.persistence.mongodb.MongoReadJournal
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.scaladsl.CurrentPersistenceIdsQuery
import akka.util.Timeout
import br.usp.domain.Consumer
import br.usp.domain.ConsumerDomain.{CreateConsumer, DeleteConsumer, GetConsumer, GetConsumerResponse}
import org.bson.types.ObjectId

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class ConsumerRepository(system: ActorSystem[_]) {
  val sharding = ClusterSharding(system)
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))


  def getConsumers(implicit system: ActorSystem[_]): Future[Seq[Consumer]] = {
    val readJournal =
      PersistenceQuery(system).readJournalFor[CurrentPersistenceIdsQuery](MongoReadJournal.Identifier)
    readJournal
      .currentPersistenceIds()
      .map(id => Await.result(getConsumer(id.split("\\|").last), 2.second).maybeConsumer.get)
      .runFold(Seq.empty[Consumer])((set, consumer) => set.+:(consumer))
  }

  def getConsumer(consumerId: String): Future[GetConsumerResponse] = {
    val entityRef = sharding.entityRefFor(ConsumerPersistence.EntityKey, consumerId)
    entityRef.ask(GetConsumer)
  }
  def createConsumer(consumer: Consumer): Future[String] = {
    val id = new ObjectId().toString
    val entityRef = sharding.entityRefFor(ConsumerPersistence.EntityKey, id)
    entityRef.ask(CreateConsumer(consumer, _))
  }
  def deleteConsumer(consumerId: String): Future[String] = {
    val entityRef = sharding.entityRefFor(ConsumerPersistence.EntityKey, consumerId)
    entityRef.ask(DeleteConsumer)
  }
}
