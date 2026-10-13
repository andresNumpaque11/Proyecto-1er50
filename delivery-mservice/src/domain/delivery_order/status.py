from enum import StrEnum


class DeliveryOrderStatus(StrEnum):
    PENDING = "PENDING"
    READY_FOR_ROUTE = "READY_FOR_ROUTE"
    ASSIGNED = "ASSIGNED"
    IN_DELIVERY = "IN_DELIVERY"
    DELIVERED = "DELIVERED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"
