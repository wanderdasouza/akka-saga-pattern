package br.usp.serialization

import br.usp.domain.ConsumerDomain.ActionPerformed
import br.usp.domain.{Consumer, Consumers}

object JsonFormats {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import spray.json.DefaultJsonProtocol._

  import spray.json.{DeserializationException, JsString, JsValue, RootJsonFormat}


  implicit val consumerJsonFormat = jsonFormat1(Consumer)


  implicit val consumersJsonFormat = jsonFormat1(Consumers)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
