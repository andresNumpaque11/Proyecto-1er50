from dataclasses import dataclass, field
from datetime import datetime, timezone

from ..shared.aggregate import AggregateRoot
from ..shared.ids import RouteId, OrderId, DriverId
from ..shared.errors import InvalidStateTransition, BusinessRuleViolation
from .status import RouteStatus
from .delivery_stop import DeliveryStop
from ..events.events import event


@dataclass
class Route(AggregateRoot):
    id: RouteId
    city: str
    max_stops: int = 10
    max_wait_seconds: int = 600
    stops: list[DeliveryStop] = field(default_factory=list)
    driver_id: DriverId | None = None
    status: RouteStatus = RouteStatus.OPEN
    created_at: datetime = field(
        default_factory=lambda: datetime.now(timezone.utc)
    )
    assigned_at: datetime | None = None
    started_at: datetime | None = None
    completed_at: datetime | None = None

    def __post_init__(self):
        AggregateRoot.__init__(self)

    @classmethod
    def create(cls, city, max_stops, max_wait_seconds):
        return cls(
            RouteId.new(),
            city,
            max_stops,
            max_wait_seconds,
        )

    @property
    def stop_count(self):
        return len(self.stops)

    @property
    def is_full(self):
        return self.stop_count >= self.max_stops

    @property
    def all_stops_completed(self):
        return bool(self.stops) and all(
            stop.is_completed for stop in self.stops
        )

    def should_close_by_time(self, now):
        """
        Indica si una ruta abierta con paradas debe cerrarse
        porque ha superado el tiempo máximo de espera.

        Las fechas sin zona horaria se interpretan como UTC.
        """
        if now.tzinfo is None:
            now = now.replace(tzinfo=timezone.utc)
        else:
            now = now.astimezone(timezone.utc)

        created_at = self.created_at

        if created_at.tzinfo is None:
            created_at = created_at.replace(tzinfo=timezone.utc)
        else:
            created_at = created_at.astimezone(timezone.utc)

        return (
            self.stop_count > 0
            and (now - created_at).total_seconds()
            >= self.max_wait_seconds
        )

    def add_stop(self, order_id, latitude, longitude, sequence=None):
        if self.status != RouteStatus.OPEN:
            raise InvalidStateTransition("La ruta no está abierta")

        if self.is_full:
            raise BusinessRuleViolation("La ruta está llena")

        if any(s.order_id == order_id for s in self.stops):
            raise BusinessRuleViolation("La orden ya pertenece a la ruta")

        if sequence is None:
            sequence = self.stop_count + 1

        stop = DeliveryStop(
            order_id=order_id,
            sequence=sequence,
            latitude=latitude,
            longitude=longitude,
        )

        self.stops.insert(sequence - 1, stop)

        self._recalculate_sequences()

        if self.is_full:
            self.mark_ready_for_assignment()

    def mark_ready_for_assignment(self):
        if self.status == RouteStatus.OPEN:
            self.status = RouteStatus.READY_FOR_ASSIGNMENT

            self.record_event(
                event(
                    "delivery.route.ready.v1",
                    str(self.id),
                    {
                        "route_id": str(self.id),
                        "stop_count": self.stop_count,
                    },
                )
            )

    def assign_driver(self, driver_id):
        if self.status != RouteStatus.READY_FOR_ASSIGNMENT:
            raise InvalidStateTransition(
                "La ruta no está lista para asignación"
            )

        self.driver_id = driver_id
        self.status = RouteStatus.ASSIGNED
        self.assigned_at = datetime.now(timezone.utc)

        self.record_event(
            event(
                "delivery.driver.assigned.v1",
                str(self.id),
                {
                    "route_id": str(self.id),
                    "driver_id": driver_id.value,
                },
                self.assigned_at,
            )
        )

    def start(self):
        if self.status != RouteStatus.ASSIGNED:
            raise InvalidStateTransition(
                "La ruta debe estar asignada"
            )

        self.status = RouteStatus.IN_PROGRESS
        self.started_at = datetime.now(timezone.utc)

        self.record_event(
            event(
                "delivery.route.started.v1",
                str(self.id),
                {"route_id": str(self.id)},
                self.started_at,
            )
        )

    def complete_stop(self, order_id):
        if self.status != RouteStatus.IN_PROGRESS:
            raise InvalidStateTransition(
                "La ruta no está en progreso"
            )

        stop = next(
            (
                stop
                for stop in self.stops
                if stop.order_id == order_id
            ),
            None,
        )

        if stop is None:
            raise BusinessRuleViolation(
                "La orden no pertenece a la ruta"
            )

        if stop.is_completed:
            raise BusinessRuleViolation(
                "La parada ya fue completada"
            )

        stop.complete()

        self.record_event(
            event(
                "delivery.stop.completed.v1",
                str(self.id),
                {
                    "route_id": str(self.id),
                    "order_id": order_id.value,
                },
                stop.completed_at,
            )
        )

        if self.all_stops_completed:
            self.status = RouteStatus.COMPLETED
            self.completed_at = datetime.now(timezone.utc)

            self.record_event(
                event(
                    "delivery.route.completed.v1",
                    str(self.id),
                    {"route_id": str(self.id)},
                    self.completed_at,
                )
            )

    def _recalculate_sequences(self):
        for index, stop in enumerate(self.stops, start=1):
            stop.sequence = index