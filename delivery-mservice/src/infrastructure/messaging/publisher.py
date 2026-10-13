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
