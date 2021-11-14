package br.usp

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import br.usp.domain.TicketDomain._
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import br.usp.domain.Ticket
import br.usp.serialization.JsonSerializable

object KitchenPersistence {

 final case class State(id: String, orderId: String, consumerId: String) extends JsonSerializable {
   def createTicket(ticket: Ticket) = copy(orderId = ticket.orderId, consumerId = ticket.consumerId)
   def removeTicket = copy(orderId = null, consumerId = null)
 }

  object State {
    def empty(ticketId: String) = State(ticketId, null, null)
  }

  val EntityKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("Ticket")


  private def commandHandler(context: ActorContext[Command], state: State, command: Command): ReplyEffect[Event, State] = {
    command match {
      case GetTicket(replyTo) =>
        Effect
          .reply(replyTo)(GetTicketResponse(Option(Ticket(state.orderId, state.consumerId))))
      case CreateTicket(ticket, replyTo) =>
        Effect
          .persist(TicketCreated(ticket))
          .thenReply(replyTo)(newTicketState => newTicketState.id)
      case DeleteTicket(replyTo) =>
        Effect.persist(TicketDeleted).thenReply(replyTo)(newTicketState =>
          newTicketState.id
        )
    }
  }

  private def eventHandler(state: State, event: Event): State = {
    event match {
      case TicketCreated(ticket) =>
        state.createTicket(ticket)
      case TicketDeleted =>
        state.removeTicket
    }
  }

  def initSharding(system: ActorSystem[_]): Unit = {
    val behaviorFactory: EntityContext[Command] => Behavior[Command] = {
      entityContext =>
        KitchenPersistence(entityContext.entityId)
    }
    ClusterSharding(system).init(Entity(EntityKey)(behaviorFactory))
  }


  def apply(ticketId: String): Behavior[Command] = {
    Behaviors.setup { context =>
      EventSourcedBehavior[Command, Event, State](
        persistenceId = PersistenceId(EntityKey.name, ticketId),
        emptyState = State.empty(ticketId),
        commandHandler = (state, command) => commandHandler(context, state, command),
        eventHandler = (state, event) => eventHandler(state, event))
    }

  }


}

