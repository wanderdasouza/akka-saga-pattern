package br.usp.serialization

import br.usp.domain.OrderDomain.ActionPerformed
import br.usp.domain.{Order, OrderRequest, OrderState, Orders}
import br.usp.domain.OrderState.OrderState
import spray.json._

object JsonFormats {
  // import the default encoders for primitive types (Int, String, Lists etc)
  implicit val orderStateFormat: RootJsonFormat[OrderState] = new RootJsonFormat[OrderState] {
    override def write(obj: OrderState): JsValue = JsString(obj.toString)

    override def read(json: JsValue): OrderState = {
      json match {
        case JsString(txt) => OrderState.withName(txt)
        case somethingElse => throw DeserializationException(s"Expected a value from enum OrderState instead of $somethingElse")
      }
    }
  }

  import spray.json.DefaultJsonProtocol._

  import spray.json.{JsString, JsValue, RootJsonFormat}

  implicit val orderJsonFormat = jsonFormat2(Order)

  implicit val orderRequestJsonFormat = jsonFormat1(OrderRequest)


  implicit val ordersJsonFormat  = jsonFormat1(Orders)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)

  implicit val orderCreatedToKafkaForm = jsonFormat3(OrderCreatedToKafka)

  object CreditCardAuthorizedToKafkaProtocol extends DefaultJsonProtocol {
    implicit val creditCardAuthorizedToKafkaForm = jsonFormat3(CreditCardAuthorizedToKafka)
  }

  import CreditCardAuthorizedToKafkaProtocol._

  implicit object KafkaEventFormat extends RootJsonFormat[KafkaEvent] {
    def write(event: KafkaEvent) = event match {
      case orderCreated: OrderCreatedToKafka => orderCreated.toJson
      case creditCardAuthorized: CreditCardAuthorizedToKafka => creditCardAuthorized.toJson
    }
    def read(json: JsValue) =
      json.asJsObject.fields("eventType") match {
        case Seq(JsString("OrderCreated")) => json.convertTo[OrderCreatedToKafka]
        case Seq(JsString("CreditCardAuthorized")) => json.convertTo[CreditCardAuthorizedToKafka]
      }
  }

}
