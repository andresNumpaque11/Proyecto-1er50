from dataclasses import dataclass
from datetime import datetime, timezone

from ..shared.ids import OrderId


@dataclass
class DeliveryStop:
    order_id: OrderId
    sequence: int
    latitude: float
    longitude: float
    completed_at: datetime | None = None

    @property
    def is_completed(self):
        return self.completed_at is not None

    def complete(self):
        self.completed_at = datetime.now(timezone.utc)
