package br.usp.domain

import akka.actor.typed.ActorRef
import br.usp.domain.OrderState.OrderState
import br.usp.serialization.JsonSerializable

import java.text.Format
import scala.collection.immutable

  object OrderState extends Enumeration {
    type OrderState = Value
    val PENDING, REJECTED, APPROVED = Value
  }

//#user-case-classes
final case class Order(state: OrderState) extends JsonSerializable
final case class Orders(orders: immutable.Seq[Order]) extends JsonSerializable

final case class OrderRequest(userId: String) extends JsonSerializable


object OrderDomain {
  // actor protocol
  sealed trait Command extends JsonSerializable
  final case class CreateOrder(order: Order, replyTo: ActorRef[String]) extends Command
  final case class GetOrder(replyTo: ActorRef[GetOrderResponse]) extends Command
  final case class DeleteOrder(replyTo: ActorRef[String]) extends Command



  sealed trait Event extends JsonSerializable
  case class OrderCreated(order: Order) extends Event
  case object OrderDeleted extends Event

  final case class GetOrderResponse(maybeOrder: Option[Order]) extends JsonSerializable
  final case class ActionPerformed(order: Order) extends JsonSerializable

}

