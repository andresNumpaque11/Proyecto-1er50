import json, logging
from urllib.request import Request, urlopen

log = logging.getLogger(__name__)


class LogPublisher:
    def publish(self, e):
        log.info("EVENT %s %s %s", e.event_name, e.aggregate_id, e.payload)


class HttpPublisher:
    def __init__(self, url, timeout):
        self.url = url
        self.timeout = timeout

    def publish(self, e):
        body = json.dumps(
            {
                "event_name": e.event_name,
                "occurred_at": e.occurred_at.isoformat(),
                "aggregate_id": e.aggregate_id,
                "payload": e.payload,
            }
        ).encode()
        req = Request(
            self.url,
            body,
            headers={"Content-Type": "application/json", "X-Event-Type": e.event_name},
            method="POST",
        )
        with urlopen(req, timeout=self.timeout) as r:
            if r.status >= 300:
                raise RuntimeError(f"Publisher status {r.status}")


class RabbitMQPublisher:
    """Publica eventos de dominio en el exchange topic compartido del proyecto."""
    def __init__(self, host, port, username, password, exchange):
        self.host, self.port = host, port
        self.username, self.password = username, password
        self.exchange = exchange

    def publish(self, event):
        import pika
        credentials = pika.PlainCredentials(self.username, self.password)
        params = pika.ConnectionParameters(
            host=self.host, port=self.port, credentials=credentials,
            heartbeat=30, blocked_connection_timeout=5
        )
        body = json.dumps({
            "event_name": event.event_name,
            "occurred_at": event.occurred_at.isoformat(),
            "aggregate_id": event.aggregate_id,
            "payload": event.payload,
            "source": "delivery-service",
        }, default=str).encode()
        connection = pika.BlockingConnection(params)
        try:
            channel = connection.channel()
            channel.exchange_declare(exchange=self.exchange, exchange_type="topic", durable=True)
            channel.basic_publish(
                exchange=self.exchange,
                routing_key=event.event_name,
                body=body,
                properties=pika.BasicProperties(
                    content_type="application/json", delivery_mode=2,
                    message_id=f"{event.event_name}:{event.aggregate_id}:{event.occurred_at.isoformat()}"
                ),
                mandatory=False,
            )
        finally:
            connection.close()
