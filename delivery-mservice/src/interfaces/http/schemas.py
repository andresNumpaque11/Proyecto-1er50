from pydantic import BaseModel, Field


class AddressIn(BaseModel):
    street: str
    city: str
    postal_code: str | None = None
    latitude: float | None = Field(None, ge=-90, le=90)
    longitude: float | None = Field(None, ge=-180, le=180)
    reference: str | None = None


class OrderIn(BaseModel):
    order_id: str = Field(min_length=1)
    destination: AddressIn
    priority: str = "NORMAL"
    external_reference: str | None = None


class OrderOut(BaseModel):
    order_id: str
    status: str
    route_id: str | None


class DriverIn(BaseModel):
    driver_id: str
    name: str
    phone: str | None = None


class AvailabilityIn(BaseModel):
    available: bool


class DriverOut(BaseModel):
    driver_id: str
    name: str
    status: str
    current_route_id: str | None


class RouteOut(BaseModel):
    route_id: str
    city: str
    status: str
    driver_id: str | None
    stops: list[dict]
