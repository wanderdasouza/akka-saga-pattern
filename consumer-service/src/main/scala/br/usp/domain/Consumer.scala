package br.usp.domain

import akka.actor.typed.ActorRef
import br.usp.serialization.JsonSerializable
import scala.collection.immutable


//#consumer-case-classes
final case class Consumer(id: String, name: String) extends JsonSerializable
final case class ConsumerRequest(name: String) extends JsonSerializable
final case class Consumers(consumers: immutable.Seq[Consumer]) extends JsonSerializable


object ConsumerDomain {
  // actor protocol
  sealed trait Command extends JsonSerializable
  final case class CreateConsumer(consumer: Consumer, replyTo: ActorRef[String]) extends Command
  final case class GetConsumer(replyTo: ActorRef[GetConsumerResponse]) extends Command
  final case class DeleteConsumer(replyTo: ActorRef[String]) extends Command



  sealed trait Event extends JsonSerializable
  case class ConsumerCreated(order: Consumer) extends Event
  case object ConsumerDeleted extends Event

  final case class GetConsumerResponse(maybeConsumer: Option[Consumer]) extends JsonSerializable
  final case class ActionPerformed(consumer: Consumer) extends JsonSerializable

}

