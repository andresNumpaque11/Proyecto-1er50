from datetime import datetime, timezone

from ...domain.delivery_order.entity import DeliveryOrder
from ...domain.delivery_order.status import DeliveryOrderStatus
from ...domain.route.entity import Route
from ...domain.route.status import RouteStatus
from ...domain.route.delivery_stop import DeliveryStop
from ...domain.driver.entity import Driver
from ...domain.driver.status import DriverStatus
from ...domain.shared.ids import OrderId, RouteId, DriverId
from ...domain.shared.value_objects import Address, Priority


def ensure_utc(dt: datetime | None) -> datetime | None:
    if dt is None:
        return None

    if dt.tzinfo is None:
        return dt.replace(tzinfo=timezone.utc)

    return dt.astimezone(timezone.utc)


class OrderRepo:
    def __init__(self, collection):
        self.c = collection

    def get(self, order_id, session=None):
        document = self.c.find_one(
            {"_id": order_id.value},
            session=session,
        )

        return order_from(document) if document else None

    def exists(self, order_id, session=None):
        return (
            self.c.find_one(
                {"_id": order_id.value},
                {"_id": 1},
                session=session,
            )
            is not None
        )

    def save(self, order, session=None):
        self.c.replace_one(
            {"_id": order.id.value},
            order_doc(order),
            upsert=True,
            session=session,
        )


class RouteRepo:
    def __init__(self, collection):
        self.c = collection

    def get(self, route_id, session=None):
        document = self.c.find_one(
            {"_id": str(route_id)},
            session=session,
        )

        return route_from(document) if document else None

    def save(self, route, session=None):
        self.c.replace_one(
            {"_id": str(route.id)},
            route_doc(route),
            upsert=True,
            session=session,
        )

    def find_open_by_city(self, city, session=None):
        document = self.c.find_one(
            {
                "status": RouteStatus.OPEN.value,
                "city": city,
            },
            sort=[("created_at", 1)],
            session=session,
        )

        return route_from(document) if document else None

    def find_ready(self, session=None):
        documents = self.c.find(
            {
                "status": RouteStatus.READY_FOR_ASSIGNMENT.value,
            },
            session=session,
        )

        return [
            route_from(document)
            for document in documents
        ]

    def find_expired(self, now, session=None):
        now = ensure_utc(now)

        documents = self.c.find(
            {
                "status": RouteStatus.OPEN.value,
            },
            session=session,
        )

        routes = [
            route_from(document)
            for document in documents
        ]

        return [
            route
            for route in routes
            if route.should_close_by_time(now)
        ]


class DriverRepo:
    def __init__(self, collection):
        self.c = collection

    def get(self, driver_id, session=None):
        document = self.c.find_one(
            {"_id": driver_id.value},
            session=session,
        )

        return driver_from(document) if document else None

    def save(self, driver, session=None):
        self.c.replace_one(
            {"_id": driver.id.value},
            driver_doc(driver),
            upsert=True,
            session=session,
        )

    def available(self, session=None):
        documents = self.c.find(
            {
                "status": DriverStatus.AVAILABLE.value,
            },
            session=session,
        )

        return [
            driver_from(document)
            for document in documents
        ]


class OutboxRepo:
    def __init__(self, collection):
        self.c = collection

    def add(self, event, session=None):
        self.c.insert_one(
            {
                "event_name": event.event_name,
                "occurred_at": ensure_utc(event.occurred_at),
                "aggregate_id": event.aggregate_id,
                "payload": event.payload,
                "status": "PENDING",
                "attempts": 0,
                "last_error": None,
                "created_at": ensure_utc(event.occurred_at),
            },
            session=session,
        )

    def pending(self, limit):
        return list(
            self.c.find(
                {"status": "PENDING"},
                sort=[("created_at", 1)],
                limit=limit,
            )
        )

    def published(self, event_id):
        self.c.update_one(
            {"_id": event_id},
            {
                "$set": {
                    "status": "PUBLISHED",
                }
            },
        )

    def failed(self, event_id, error):
        self.c.update_one(
            {"_id": event_id},
            {
                "$set": {
                    "last_error": error,
                },
                "$inc": {
                    "attempts": 1,
                },
            },
        )


def order_doc(o):
    return {
        "_id": o.id.value,
        "destination": o.destination.__dict__,
        "priority": o.priority.value,
        "status": o.status.value,
        "route_id": str(o.route_id) if o.route_id else None,
        "external_reference": o.external_reference,
        "created_at": ensure_utc(o.created_at),
        "updated_at": ensure_utc(o.updated_at),
    }


def order_from(d):
    return DeliveryOrder(
        OrderId(d["_id"]),
        Address(**d["destination"]),
        Priority(d["priority"]),
        DeliveryOrderStatus(d["status"]),
        (
            RouteId.from_string(d["route_id"])
            if d.get("route_id")
            else None
        ),
        d.get("external_reference"),
        ensure_utc(d["created_at"]),
        ensure_utc(d["updated_at"]),
    )


def route_doc(r):
    return {
        "_id": str(r.id),
        "city": r.city,
        "max_stops": r.max_stops,
        "max_wait_seconds": r.max_wait_seconds,
        "status": r.status.value,
        "driver_id": r.driver_id.value if r.driver_id else None,

        "stops": [
            {
                "order_id": s.order_id.value,
                "sequence": s.sequence,
                "latitude": s.latitude,
                "longitude": s.longitude,
                "completed_at": s.completed_at,
            }
            for s in r.stops
        ],

        "created_at": r.created_at,
        "assigned_at": r.assigned_at,
        "started_at": r.started_at,
        "completed_at": r.completed_at,
    }


def route_from(d):
    return Route(
        RouteId.from_string(d["_id"]),
        d["city"],
        d["max_stops"],
        d["max_wait_seconds"],

        [
            DeliveryStop(
                OrderId(s["order_id"]),
                s["sequence"],
                s["latitude"],
                s["longitude"],
                s.get("completed_at"),
            )
            for s in d.get("stops", [])
        ],

        DriverId(d["driver_id"])
        if d.get("driver_id")
        else None,

        RouteStatus(d["status"]),
        d["created_at"],
        d.get("assigned_at"),
        d.get("started_at"),
        d.get("completed_at"),
    )


def driver_doc(d):
    return {
        "_id": d.id.value,
        "name": d.name,
        "phone": d.phone,
        "status": d.status.value,
        "current_route_id": d.current_route_id,
        "created_at": ensure_utc(d.created_at),
    }


def driver_from(d):
    return Driver(
        DriverId(d["_id"]),
        d["name"],
        d.get("phone"),
        DriverStatus(d["status"]),
        d.get("current_route_id"),
        ensure_utc(d["created_at"]),
    )