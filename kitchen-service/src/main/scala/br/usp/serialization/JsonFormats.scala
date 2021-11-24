package br.usp.serialization

import br.usp.domain.TicketDomain.ActionPerformed
import br.usp.domain.{Ticket, Tickets}
import spray.json.{DefaultJsonProtocol, enrichAny}

object JsonFormats {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import spray.json.DefaultJsonProtocol._

  import spray.json.{DeserializationException, JsString, JsValue, RootJsonFormat}


  implicit val consumerJsonFormat = jsonFormat2(Ticket)


  implicit val ticketsJsonFormat = jsonFormat1(Tickets)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)

  object OrderCreatedToKafkaProtocol extends DefaultJsonProtocol {
    implicit val orderCreatedToKafkaForm = jsonFormat3(OrderCreatedToKafka)
  }

  object TicketCreatedToKafkaProtocol extends DefaultJsonProtocol {
    implicit val ticketCreatedToKafkaForm = jsonFormat4(TicketCreatedToKafka)
  }

  import OrderCreatedToKafkaProtocol._
  import TicketCreatedToKafkaProtocol._

  implicit object KafkaEventFormat extends RootJsonFormat[KafkaEvent] {
    def write(event: KafkaEvent) = event match {
      case orderCreated: OrderCreatedToKafka => orderCreated.toJson
      case ticketCreated: TicketCreatedToKafka => ticketCreated.toJson
    }
    def read(json: JsValue) =
      json.asJsObject.fields("eventType") match {
        case Seq(JsString("OrderCreated")) => json.convertTo[OrderCreatedToKafka]
        case Seq(JsString("TicketCreated")) => json.convertTo[TicketCreatedToKafka]
      }
  }
}
