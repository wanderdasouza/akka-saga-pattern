package br.usp.serialization

trait KafkaEvent

final case class OrderCreatedToKafka(eventType: String, orderId: String, consumerId: String) extends KafkaEvent
