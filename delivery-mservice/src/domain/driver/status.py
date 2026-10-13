from enum import StrEnum


class DriverStatus(StrEnum):
    AVAILABLE = "AVAILABLE"
    ASSIGNED = "ASSIGNED"
    IN_ROUTE = "IN_ROUTE"
    UNAVAILABLE = "UNAVAILABLE"
