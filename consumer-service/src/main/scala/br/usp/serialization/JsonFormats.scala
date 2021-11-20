package br.usp.serialization

import br.usp.domain.ConsumerDomain.ActionPerformed
import br.usp.domain.{Consumer, Consumers}
import spray.json.{DefaultJsonProtocol, enrichAny}

object JsonFormats {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import spray.json.DefaultJsonProtocol._

  import spray.json.{DeserializationException, JsString, JsValue, RootJsonFormat}


  implicit val consumerJsonFormat = jsonFormat1(Consumer)


  implicit val consumersJsonFormat = jsonFormat1(Consumers)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)

  object OrderCreatedToKafkaProtocol extends DefaultJsonProtocol {
    implicit val orderCreatedToKafkaForm = jsonFormat3(OrderCreatedToKafka)
  }

  import OrderCreatedToKafkaProtocol._


  implicit object KafkaEventFormat extends RootJsonFormat[KafkaEvent] {
    def write(event: KafkaEvent) = event match {
      case orderCreated: OrderCreatedToKafka => orderCreated.toJson
    }
    def read(json: JsValue) =
      json.asJsObject.fields("eventType") match {
        case Seq(JsString("OrderCreated")) => json.convertTo[OrderCreatedToKafka]
      }
  }
}
