from dataclasses import dataclass, field
from datetime import datetime, timezone
from ..shared.aggregate import AggregateRoot
from ..shared.ids import DriverId
from ..shared.errors import InvalidStateTransition
from .status import DriverStatus


@dataclass
class Driver(AggregateRoot):
    id: DriverId
    name: str
    phone: str | None = None
    status: DriverStatus = DriverStatus.AVAILABLE
    current_route_id: str | None = None
    created_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))

    def __post_init__(self):
        AggregateRoot.__init__(self)


    def assign(self, route_id: str) -> None:
        if self.status != DriverStatus.AVAILABLE:
            raise InvalidStateTransition("El domiciliario no está disponible.")
        self.status = DriverStatus.ASSIGNED
        self.current_route_id = route_id

    def start_route(self):
        if self.status != DriverStatus.ASSIGNED:
            raise InvalidStateTransition("Domiciliario sin ruta asignada")
        self.status = DriverStatus.IN_ROUTE

    def become_available(self):
        self.status = DriverStatus.AVAILABLE
        self.current_route_id = None

    def make_unavailable(self):
        if self.status != DriverStatus.AVAILABLE:
            raise InvalidStateTransition("No se puede retirar este domiciliario")
        self.status = DriverStatus.UNAVAILABLE
