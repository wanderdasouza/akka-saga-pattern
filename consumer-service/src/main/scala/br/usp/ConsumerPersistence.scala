package br.usp

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import br.usp.domain.ConsumerDomain._
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import br.usp.domain.Consumer
import br.usp.serialization.JsonSerializable

object ConsumerPersistence {

 final case class State(id: String, name: String) extends JsonSerializable {
   def createConsumer(consumer: Consumer) = copy(name = consumer.name)
   def removeConsumer = copy(name = null)
 }

  object State {
    def empty(consumerId: String) = State(consumerId, null)
  }

  val EntityKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("Consumer")


  private def commandHandler(context: ActorContext[Command], state: State, command: Command): ReplyEffect[Event, State] = {
    command match {
      case GetConsumer(replyTo) =>
        Effect
          .reply(replyTo)(GetConsumerResponse(Option(Consumer(state.name))))
      case CreateConsumer(consumer, replyTo) =>
        Effect
          .persist(ConsumerCreated(consumer))
          .thenReply(replyTo)(newConsumerState => newConsumerState.id)
      case DeleteConsumer(replyTo) =>
        Effect.persist(ConsumerDeleted).thenReply(replyTo)(newConsumerState =>
          newConsumerState.id
        )
    }
  }

  private def eventHandler(state: State, event: Event): State = {
    event match {
      case ConsumerCreated(consumer) =>
        state.createConsumer(consumer)
      case ConsumerDeleted =>
        state.removeConsumer
    }
  }

  def initSharding(system: ActorSystem[_]): Unit = {
    val behaviorFactory: EntityContext[Command] => Behavior[Command] = {
      entityContext =>
        ConsumerPersistence(entityContext.entityId)
    }
    ClusterSharding(system).init(Entity(EntityKey)(behaviorFactory))
  }


  def apply(consumerId: String): Behavior[Command] = {
    Behaviors.setup { context =>
      EventSourcedBehavior[Command, Event, State](
        persistenceId = PersistenceId(EntityKey.name, consumerId),
        emptyState = State.empty(consumerId),
        commandHandler = (state, command) => commandHandler(context, state, command),
        eventHandler = (state, event) => eventHandler(state, event))
    }

  }


}

