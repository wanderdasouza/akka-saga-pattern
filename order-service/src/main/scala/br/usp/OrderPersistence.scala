package br.usp

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import br.usp.domain.OrderDomain._
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import br.usp.domain.{Order, OrderState}
import akka.stream.Materializer
import br.usp.domain.OrderState.OrderState
import br.usp.serialization.{JsonSerializable, OrderCreatedToKafka}

object OrderPersistence {

 final case class State(id: String, consumerId: String, orderState: OrderState) extends JsonSerializable {
   def createOrder(): State = copy(orderState = OrderState.PENDING)
   def approveOrder(): State = copy(orderState = OrderState.APPROVED)
   def rejectOrder(): State = copy(orderState = OrderState.REJECTED)
   def removeOrder(): State = copy(orderState = null)
 }

  object State {
    def empty(orderId: String): State = State(orderId, null, null)
  }

  val EntityKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("Order")

  val chain: State => Unit = {
    case State(_, _, OrderState.PENDING) =>
      println("\n\nOrder is pending\n\n")
    case State(_, _, OrderState.APPROVED) =>
      println("Order is approved")
    case State(_, _, OrderState.REJECTED) =>
      println("Order is rejected")
    case State(_, _, null) =>
      println("Order is removed")
  }
  private def commandHandler(context: ActorContext[Command], state: State, command: Command): ReplyEffect[Event, State] = {
    implicit val mat: Materializer = Materializer(context.system)
    command match {
      case GetOrder(replyTo) =>
        Effect
          .reply(replyTo)(GetOrderResponse(Option(Order(state.consumerId, state.orderState))))
      case CreateOrder(order, replyTo) =>
        Effect
          .persist(OrderCreated(order))
          .thenReply(replyTo)(newUserState => newUserState.id)
      case DeleteOrder(replyTo) =>
        Effect.persist(OrderDeleted).thenReply(replyTo)(newOrderState =>
          newOrderState.id
        )
    }
  }

  private def eventHandler(context: ActorContext[_], state: State, event: Event): State = {
    implicit val mat: Materializer = Materializer(context.system)
    event match {
      case OrderCreated(order) =>
        val f = state.createOrder()
        OrderProducer.publish("order-created", OrderCreatedToKafka("OrderCreated", f.id, order.consumerId))
        f
      case OrderDeleted =>
        state.removeOrder()
    }
  }

  def initSharding(system: ActorSystem[_]): Unit = {
    val behaviorFactory: EntityContext[Command] => Behavior[Command] = {
      entityContext =>
        OrderPersistence(entityContext.entityId)
    }
    ClusterSharding(system).init(Entity(EntityKey)(behaviorFactory))
  }


  def apply(orderId: String): Behavior[Command] = {
    Behaviors.setup { context =>
      EventSourcedBehavior[Command, Event, State](
        persistenceId = PersistenceId(EntityKey.name, orderId),
        emptyState = State.empty(orderId),
        commandHandler = (state, command) => commandHandler(context, state, command),
        eventHandler = (state, event) => eventHandler(context, state, event))
        .withTagger(_ => Set("orders"))
    }

  }


}

