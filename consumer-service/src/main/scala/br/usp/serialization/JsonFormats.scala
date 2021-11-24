package br.usp.serialization

import br.usp.domain.ConsumerDomain.ActionPerformed
import br.usp.domain.{Consumer, ConsumerRequest, Consumers}
import spray.json.{DefaultJsonProtocol, enrichAny}

object JsonFormats {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import spray.json.DefaultJsonProtocol._

  import spray.json.{JsString, JsValue, RootJsonFormat}


  implicit val consumerJsonFormat = jsonFormat2(Consumer)

  implicit val consumerRequestJsonFormat = jsonFormat1(ConsumerRequest)

  implicit val consumersJsonFormat = jsonFormat1(Consumers)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)

  object OrderCreatedToKafkaProtocol extends DefaultJsonProtocol {
    implicit val orderCreatedToKafkaForm = jsonFormat3(OrderCreatedToKafka)
  }

  object ConsumerVerifiedToKafkaProtocol extends DefaultJsonProtocol {
    implicit val consumerVerifiedToKafkaForm = jsonFormat3(ConsumerVerifiedToKafka)
  }

  import OrderCreatedToKafkaProtocol._
  import ConsumerVerifiedToKafkaProtocol._


  implicit object KafkaEventFormat extends RootJsonFormat[KafkaEvent] {
    def write(event: KafkaEvent) = event match {
      case orderCreated: OrderCreatedToKafka => orderCreated.toJson
      case consumerVerified: ConsumerVerifiedToKafka => consumerVerified.toJson
    }
    def read(json: JsValue) =
      json.asJsObject.fields("eventType") match {
        case Seq(JsString("OrderCreated")) => json.convertTo[OrderCreatedToKafka]
        case Seq(JsString("ConsumerVerified")) => json.convertTo[ConsumerVerifiedToKafka]
      }
  }
}
