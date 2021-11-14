package br.usp

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import br.usp.domain.OrderDomain._
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityContext, EntityTypeKey}
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import br.usp.domain.{Order, OrderState}
import br.usp.domain.OrderState.OrderState
import br.usp.serialization.JsonSerializable
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

object OrderPersistence {

  val configKafka = ConfigFactory.load().getConfig("akka.kafka.producer")
  val producerSettings =
    ProducerSettings[String, String](configKafka, new StringSerializer, new StringSerializer)
      .withBootstrapServers("localhost:9094")

 final case class State(id: String, orderState: OrderState) extends JsonSerializable {
   def createOrder(order: Order) = copy(orderState = OrderState.PENDING)
   def removeOrder = copy(orderState = null)
 }

  object State {
    def empty(orderId: String) = State(orderId, null)
  }

  val EntityKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("Order")


  private def commandHandler(context: ActorContext[Command], state: State, command: Command): ReplyEffect[Event, State] = {
    implicit val mat: Materializer = Materializer(context.system)
    command match {
      case GetOrder(replyTo) =>
        Effect
          .reply(replyTo)(GetOrderResponse(Option(Order(state.orderState))))
      case CreateOrder(order, replyTo) =>
        Source
          .single(new ProducerRecord("user-created", "key", OrderCreated(order).toString))
          .runWith(Producer.plainSink(producerSettings))
        Effect
          .persist(OrderCreated(order))
          .thenReply(replyTo)(newUserState => newUserState.id)
      case DeleteOrder(replyTo) =>
        Effect.persist(OrderDeleted).thenReply(replyTo)(newOrderState =>
          newOrderState.id
        )
    }
  }

  private def eventHandler(state: State, event: Event): State = {
    event match {
      case OrderCreated(user) =>
        state.createOrder(user)
      case OrderDeleted =>
        state.removeOrder
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
        eventHandler = (state, event) => eventHandler(state, event))
    }

  }


}

