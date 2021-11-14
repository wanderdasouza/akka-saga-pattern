package br.usp.serialization

import br.usp.domain.TicketDomain.ActionPerformed
import br.usp.domain.{Ticket, Tickets}

object JsonFormats {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import spray.json.DefaultJsonProtocol._

  import spray.json.{DeserializationException, JsString, JsValue, RootJsonFormat}


  implicit val consumerJsonFormat = jsonFormat2(Ticket)


  implicit val ticketsJsonFormat = jsonFormat1(Tickets)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
