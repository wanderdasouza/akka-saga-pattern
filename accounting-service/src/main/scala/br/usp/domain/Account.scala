package br.usp.domain

import akka.actor.typed.ActorRef
import br.usp.serialization.JsonSerializable

import java.text.Format
import scala.collection.immutable


//#Account-case-classes
final case class Account(orderId: String, consumerId: String) extends JsonSerializable
final case class Accounts(Accounts: immutable.Seq[Account]) extends JsonSerializable


object AccountDomain {
  // actor protocol
  sealed trait Command extends JsonSerializable
  final case class CreateAccount(account: Account, replyTo: ActorRef[String]) extends Command
  final case class GetAccount(replyTo: ActorRef[GetAccountResponse]) extends Command
  final case class DeleteAccount(replyTo: ActorRef[String]) extends Command

  final case class ApproveTicket(replyTo: ActorRef[String]) extends Command
  final case class VerifyConsumer(replyTo: ActorRef[String]) extends Command



  sealed trait Event extends JsonSerializable
  case class AccountCreated(account: Account) extends Event
  case object AccountDeleted extends Event

  case object TicketApproved extends Event
  case object ConsumerVerified extends Event

  final case class GetAccountResponse(maybeAccount: Option[Account]) extends JsonSerializable
  final case class ActionPerformed(Account: Account) extends JsonSerializable

}

