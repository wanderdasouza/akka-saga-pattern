package br.usp.serialization

import br.usp.domain.AccountDomain.ActionPerformed
import br.usp.domain.{Account, Accounts}
import spray.json.{DefaultJsonProtocol, enrichAny}

object JsonFormats {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import spray.json.DefaultJsonProtocol._
  import spray.json.{JsString, JsValue, RootJsonFormat}


  implicit val accountJsonFormat = jsonFormat2(Account)


  implicit val accountsJsonFormat = jsonFormat1(Accounts)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)

  object OrderCreatedToKafkaProtocol extends DefaultJsonProtocol {
    implicit val orderCreatedToKafkaForm = jsonFormat3(OrderCreatedToKafka)
  }

  object TicketCreatedToKafkaProtocol extends DefaultJsonProtocol {
    implicit val ticketCreatedToKafkaForm = jsonFormat4(TicketCreatedToKafka)
  }

  object ConsumerVerifiedToKafkaProtocol extends DefaultJsonProtocol {
    implicit val consumerVerifiedToKafkaForm = jsonFormat3(ConsumerVerifiedToKafka)
  }

  object CreditCardAuthorizedToKafkaProtocol extends DefaultJsonProtocol {
    implicit val creditCardAuthorizedToKafkaForm = jsonFormat3(CreditCardAuthorizedToKafka)
  }


  import OrderCreatedToKafkaProtocol._
  import TicketCreatedToKafkaProtocol._
  import ConsumerVerifiedToKafkaProtocol._
  import CreditCardAuthorizedToKafkaProtocol._

  implicit object KafkaEventFormat extends RootJsonFormat[KafkaEvent] {
    def write(event: KafkaEvent) = event match {
      case orderCreated: OrderCreatedToKafka => orderCreated.toJson
      case ticketCreated: TicketCreatedToKafka => ticketCreated.toJson
      case consumerVerified: ConsumerVerifiedToKafka => consumerVerified.toJson
      case creditCardAuthorized: CreditCardAuthorizedToKafka => creditCardAuthorized.toJson
      case creditCardApproved: CreditCardAuthorizedToKafka => creditCardApproved.toJson
    }
    def read(json: JsValue) =
      json.asJsObject.fields("eventType") match {
        case Seq(JsString("OrderCreated")) => json.convertTo[OrderCreatedToKafka]
        case Seq(JsString("TicketCreated")) => json.convertTo[TicketCreatedToKafka]
        case Seq(JsString("ConsumerVerified")) => json.convertTo[ConsumerVerifiedToKafka]
        case Seq(JsString("CreditCardAuthorized")) => json.convertTo[CreditCardAuthorizedToKafka]
      }
  }
}
