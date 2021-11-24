package br.usp.serialization

trait KafkaEvent

final case class OrderCreatedToKafka(eventType: String, orderId: String, consumerId: String) extends KafkaEvent
final case class TicketCreatedToKafka(eventType: String, ticketId: String, consumerId: String, orderId: String) extends KafkaEvent