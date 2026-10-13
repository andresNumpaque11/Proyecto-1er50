from dataclasses import dataclass, field
from datetime import datetime, timezone
from ..shared.aggregate import AggregateRoot
from ..shared.ids import OrderId, RouteId
from ..shared.value_objects import Address, Priority
from ..shared.errors import InvalidStateTransition
from .status import DeliveryOrderStatus


@dataclass
class DeliveryOrder(AggregateRoot):
    id: OrderId
    destination: Address
    priority: Priority
    status: DeliveryOrderStatus = DeliveryOrderStatus.PENDING
    route_id: RouteId | None = None
    external_reference: str | None = None
    created_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    updated_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))

    def __post_init__(self):
        AggregateRoot.__init__(self)

    @classmethod
    def create(cls, order_id, destination, priority, external_reference=None):
        o = cls(order_id, destination, priority, external_reference=external_reference)
        o.record_event(
            __import__("src.domain.events.events", fromlist=["event"]).event(
                "delivery.order.received.v1",
                order_id.value,
                {"order_id": order_id.value, "city": destination.city},
                o.created_at,
            )
        )
        return o

    def mark_ready_for_route(self):
        self._transition(
            {DeliveryOrderStatus.PENDING}, DeliveryOrderStatus.READY_FOR_ROUTE
        )

    def assign_to_route(self, route_id):
        self._transition(
            {DeliveryOrderStatus.READY_FOR_ROUTE}, DeliveryOrderStatus.ASSIGNED
        )
        self.route_id = route_id

    def start_delivery(self):
        self._transition(
            {DeliveryOrderStatus.ASSIGNED}, DeliveryOrderStatus.IN_DELIVERY
        )

    def complete(self):
        self._transition(
            {DeliveryOrderStatus.IN_DELIVERY, DeliveryOrderStatus.ASSIGNED},
            DeliveryOrderStatus.DELIVERED,
        )

    def fail(self):
        self._transition(
            {DeliveryOrderStatus.IN_DELIVERY, DeliveryOrderStatus.ASSIGNED},
            DeliveryOrderStatus.FAILED,
        )

    def cancel(self):
        self._transition(
            {
                DeliveryOrderStatus.PENDING,
                DeliveryOrderStatus.READY_FOR_ROUTE,
                DeliveryOrderStatus.ASSIGNED,
            },
            DeliveryOrderStatus.CANCELLED,
        )

    def _transition(self, allowed, target):
        if self.status not in allowed:
            raise InvalidStateTransition(
                f"No se puede pasar de {self.status} a {target}"
            )
        self.status = target
        self.updated_at = datetime.now(timezone.utc)
