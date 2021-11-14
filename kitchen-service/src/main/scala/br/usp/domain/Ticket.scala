package br.usp.domain

import akka.actor.typed.ActorRef
import br.usp.serialization.JsonSerializable

import java.text.Format
import scala.collection.immutable


//#ticket-case-classes
final case class Ticket(orderId: String, consumerId: String) extends JsonSerializable
final case class Tickets(tickets: immutable.Seq[Ticket]) extends JsonSerializable


object TicketDomain {
  // actor protocol
  sealed trait Command extends JsonSerializable
  final case class CreateTicket(ticket: Ticket, replyTo: ActorRef[String]) extends Command
  final case class GetTicket(replyTo: ActorRef[GetTicketResponse]) extends Command
  final case class DeleteTicket(replyTo: ActorRef[String]) extends Command



  sealed trait Event extends JsonSerializable
  case class TicketCreated(ticket: Ticket) extends Event
  case object TicketDeleted extends Event

  final case class GetTicketResponse(maybeTicket: Option[Ticket]) extends JsonSerializable
  final case class ActionPerformed(ticket: Ticket) extends JsonSerializable

}

