from .config import settings
from .persistence.mongo import MongoConnection
from .persistence.repositories import OrderRepo, RouteRepo, DriverRepo, OutboxRepo
from .messaging.publisher import LogPublisher, HttpPublisher
from ..domain.services.route_planning import (
    RoutePlanningService,
    CityAndCapacityStrategy,
)
from ..domain.services.driver_assignment import (
    DriverAssignmentService,
    FirstAvailableDriverStrategy,
)
from ..application.use_cases.orders import ReceiveDeliveryOrder
from ..application.use_cases.maintenance import ProcessRoutes
from ..application.use_cases.drivers import RegisterDriver, ChangeAvailability
from ..application.use_cases.routes import StartRoute, CompleteStop
from .persistence.unit_of_work import MongoUnitOfWork


class Container:
    def __init__(self):
        self.mongo = MongoConnection(settings.mongo_uri, settings.mongo_database)
        self.mongo.ensure_indexes()
        self.unit_of_work = MongoUnitOfWork(self.mongo)
        db = self.mongo.db
        self.orders = OrderRepo(db.delivery_orders)
        self.routes = RouteRepo(db.routes)
        self.drivers = DriverRepo(db.drivers)
        self.outbox = OutboxRepo(db.outbox)
        planner = RoutePlanningService(
            CityAndCapacityStrategy(),
            settings.route_max_stops,
            settings.route_max_wait_seconds,
        )
        assignment = DriverAssignmentService(FirstAvailableDriverStrategy())
        self.receive = ReceiveDeliveryOrder(
            self.orders,
            self.routes,
            self.drivers,
            self.outbox,
            planner,
            assignment,
            self.unit_of_work,
        )
        self.process_routes = ProcessRoutes(
            self.routes,
            self.drivers,
            self.outbox,
            assignment,
            self.unit_of_work,
        )
        self.register_driver = RegisterDriver(self.drivers)
        self.availability = ChangeAvailability(self.drivers)
        self.start_route = StartRoute(self.routes, self.drivers, self.outbox)
        self.complete_stop = CompleteStop(
            self.routes, self.orders, self.drivers, self.outbox
        )
        self.publisher = (
            HttpPublisher(
                settings.event_publisher_url, settings.event_publisher_timeout_seconds
            )
            if settings.event_publisher_mode == "http" and settings.event_publisher_url
            else LogPublisher()
        )


container = Container()
