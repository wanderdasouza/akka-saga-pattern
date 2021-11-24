package br.usp

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import br.usp.domain.AccountDomain.{ConsumerVerified, TicketApproved, _}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import akka.stream.Materializer
import br.usp.domain.Account
import br.usp.serialization.{CreditCardAuthorizedToKafka, JsonSerializable}

object AccountingPersistence {


  sealed trait State extends JsonSerializable

  case class PendingAuthorization(orderId: String, consumerId: String) extends State

  case class PendingConsumerVerification(orderId: String, consumerId: String) extends State

  case class PendingTicketApproval(orderId: String, consumerId: String) extends State

  case class CreditCardAuthorized(orderId: String, consumerId: String) extends State

  case object AccountRemoved extends State


  val EntityKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("Account")


  private def commandHandler(state: State, command: Command): ReplyEffect[Event, State] = {
    command match {
      case GetAccount(replyTo) =>
        state match {
          case pending: PendingAuthorization =>
            Effect
              .reply(replyTo)(GetAccountResponse(Option(Account(pending.orderId, pending.consumerId))))
          case pendingConsumerVerification: PendingConsumerVerification =>
            Effect
              .reply(replyTo)(GetAccountResponse(Option(Account(pendingConsumerVerification.orderId, pendingConsumerVerification.consumerId))))
          case pendingTicketApproval: PendingTicketApproval =>
            Effect
              .reply(replyTo)(GetAccountResponse(Option(Account(pendingTicketApproval.orderId, pendingTicketApproval.consumerId))))

          case creditCardApproved: CreditCardAuthorized =>
            Effect
              .reply(replyTo)(GetAccountResponse(Option(Account(creditCardApproved.orderId, creditCardApproved.consumerId))))
        }

      case CreateAccount(account, replyTo) =>
        Effect
          .persist(AccountCreated(account))
          .thenReply(replyTo)(newAccountState => newAccountState match {
            case pending: PendingAuthorization => pending.orderId
            case pendingConsumerVerification: PendingConsumerVerification => pendingConsumerVerification.orderId
            case pendingTicketApproval: PendingTicketApproval => pendingTicketApproval.orderId
            case pending: PendingAuthorization => pending.orderId
          })
      case DeleteAccount(replyTo) =>
        Effect.persist(AccountDeleted).thenReply(replyTo)(newAccountState => newAccountState match {
          case pending: PendingAuthorization =>  pending.orderId
          case pendingConsumerVerification: PendingConsumerVerification =>  pendingConsumerVerification.orderId
          case pendingTicketCreation: PendingTicketApproval =>  pendingTicketCreation.orderId
          case creditCardAuthorized: CreditCardAuthorized => creditCardAuthorized.orderId
        })

      case ApproveTicket(replyTo) =>
        Effect.persist(TicketApproved).thenReply(replyTo)(newAccountState => newAccountState match {
          case PendingAuthorization(orderId, _) =>  orderId
          case PendingConsumerVerification(orderId, _) => orderId
          case PendingTicketApproval(orderId, _) =>  orderId
          case CreditCardAuthorized(orderId, _) =>  orderId
        })
      case VerifyConsumer(replyTo) =>
        Effect.persist(ConsumerVerified).thenReply(replyTo)(newAccountState => newAccountState match {
          case PendingAuthorization(orderId, _) =>  orderId
          case PendingConsumerVerification(orderId, _) => orderId
          case PendingTicketApproval(orderId, _) =>  orderId
          case CreditCardAuthorized(orderId, _) =>  orderId
        })
    }
  }

  private def eventHandler(context: ActorContext[_], state: State, event: Event): State = {
    implicit val mat: Materializer = Materializer(context.system)

    event match {
      case AccountCreated(account) => PendingAuthorization(account.orderId, account.consumerId)

      case TicketApproved => state match {
        case PendingAuthorization(orderId, consumerId) => PendingConsumerVerification(orderId, consumerId)
        case PendingTicketApproval(orderId, consumerId) =>
          AccountingProducer.publish("credit-card-authorized", CreditCardAuthorizedToKafka("CreditCardAuthorized", orderId, consumerId))
          CreditCardAuthorized(orderId, consumerId)
      }

      case ConsumerVerified => state match {
        case PendingAuthorization(orderId, consumerId) => PendingTicketApproval(orderId, consumerId)
        case PendingConsumerVerification(orderId, consumerId) => CreditCardAuthorized(orderId, consumerId)
      }

      case AccountCreated(account) => PendingAuthorization(account.orderId, account.consumerId)

      case AccountDeleted => AccountRemoved
    }
  }

  def initSharding(system: ActorSystem[_]): Unit = {
    val behaviorFactory: EntityContext[Command] => Behavior[Command] = {
      entityContext =>
        AccountingPersistence(entityContext.entityId)
    }
    ClusterSharding(system).init(Entity(EntityKey)(behaviorFactory))
  }


  def apply(accountId: String): Behavior[Command] = {
    Behaviors.setup { context =>
      val ids = accountId.split(">")
      val orderId = ids(0)
      val consumerId = ids(1)
      EventSourcedBehavior[Command, Event, State](
        persistenceId = PersistenceId(EntityKey.name, orderId),
        emptyState = PendingAuthorization(orderId, consumerId),
        commandHandler = (state, command) => commandHandler(state, command),
        eventHandler = (state, event) => eventHandler(context, state, event))
    }

  }


}

