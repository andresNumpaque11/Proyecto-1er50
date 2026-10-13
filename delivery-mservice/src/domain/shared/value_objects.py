from dataclasses import dataclass
from .errors import InvalidValueObject


@dataclass(frozen=True)
class Address:
    street: str
    city: str
    postal_code: str | None = None
    latitude: float | None = None
    longitude: float | None = None
    reference: str | None = None

    def __post_init__(self):
        if not self.street.strip() or not self.city.strip():
            raise InvalidValueObject("Dirección incompleta")
        if (self.latitude is None) != (self.longitude is None):
            raise InvalidValueObject("latitude y longitude deben llegar juntas")
        if self.latitude is not None and not -90 <= self.latitude <= 90:
            raise InvalidValueObject("Latitud inválida")
        if self.longitude is not None and not -180 <= self.longitude <= 180:
            raise InvalidValueObject("Longitud inválida")


@dataclass(frozen=True)
class Priority:
    value: str

    def __post_init__(self):
        value = self.value.upper()
        if value not in {"LOW", "NORMAL", "HIGH", "URGENT"}:
            raise InvalidValueObject("Prioridad inválida")
        object.__setattr__(self, "value", value)
