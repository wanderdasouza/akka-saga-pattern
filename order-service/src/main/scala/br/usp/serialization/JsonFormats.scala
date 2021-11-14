package br.usp.serialization

import br.usp.domain.OrderDomain.ActionPerformed
import br.usp.domain.{Order, OrderRequest, OrderState, Orders}
import br.usp.domain.OrderState.OrderState

object JsonFormats {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import spray.json.DefaultJsonProtocol._

  import spray.json.{DeserializationException, JsString, JsValue, RootJsonFormat}

  implicit val orderStateFormat = new RootJsonFormat[OrderState] {
    override def write(obj: OrderState): JsValue = JsString(obj.toString)

    override def read(json: JsValue): OrderState = {
      json match {
        case JsString(txt) => OrderState.withName(txt)
        case somethingElse => throw DeserializationException(s"Expected a value from enum OrderState instead of $somethingElse")
      }
    }
  }

  implicit val orderJsonFormat = jsonFormat1(Order)

  implicit val orderRequestJsonFormat = jsonFormat1(OrderRequest)


  implicit val ordersJsonFormat = jsonFormat1(Orders)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
