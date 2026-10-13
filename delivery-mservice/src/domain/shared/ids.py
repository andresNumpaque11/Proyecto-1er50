from dataclasses import dataclass
from uuid import UUID, uuid4
from .errors import InvalidValueObject


@dataclass(frozen=True)
class OrderId:
    value: str

    def __post_init__(self):
        if not self.value.strip():
            raise InvalidValueObject("order_id no puede estar vacío")


@dataclass(frozen=True)
class DriverId:
    value: str

    def __post_init__(self):
        if not self.value.strip():
            raise InvalidValueObject("driver_id no puede estar vacío")


@dataclass(frozen=True)
class RouteId:
    value: UUID

    @classmethod
    def new(cls):
        return cls(uuid4())

    @classmethod
    def from_string(cls, value: str):
        return cls(UUID(value))

    def __str__(self):
        return str(self.value)
