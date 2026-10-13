from dataclasses import dataclass
from datetime import datetime

@dataclass(frozen=True)
class DomainEvent:
    event_name: str
    occurred_at: datetime
    aggregate_id: str
    payload: dict

def event(name, aggregate_id, payload, when=None):
    return DomainEvent(name, when or datetime.utcnow(), aggregate_id, payload)
