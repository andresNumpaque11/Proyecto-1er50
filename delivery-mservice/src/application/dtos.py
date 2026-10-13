from dataclasses import dataclass


@dataclass(frozen=True)
class CreateDeliveryOrderCommand:
    order_id: str
    street: str
    city: str
    postal_code: str | None
    latitude: float | None
    longitude: float | None
    reference: str | None
    priority: str
    external_reference: str | None


@dataclass(frozen=True)
class RegisterDriverCommand:
    driver_id: str
    name: str
    phone: str | None


@dataclass(frozen=True)
class AvailabilityCommand:
    driver_id: str
    available: bool


@dataclass(frozen=True)
class StartRouteCommand:
    route_id: str


@dataclass(frozen=True)
class CompleteStopCommand:
    route_id: str
    order_id: str
