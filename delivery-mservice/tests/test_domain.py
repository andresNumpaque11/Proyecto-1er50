from src.domain.route.entity import Route
from src.domain.route.status import RouteStatus
from src.domain.shared.ids import OrderId


def test_route_is_ready_at_capacity():
    route = Route.create("Tunja", 2, 600)
    route.add_stop(OrderId("O1"), 5.53, -73.36)
    route.add_stop(OrderId("O2"), 5.54, -73.37)
    assert route.status == RouteStatus.READY_FOR_ASSIGNMENT


def test_route_starts_only_after_driver_assignment():
    from src.domain.driver.entity import Driver
    from src.domain.shared.ids import DriverId
    from src.domain.shared.errors import InvalidStateTransition

    route = Route.create("Tunja", 1, 600)
    route.add_stop(OrderId("O1"), 5.53, -73.36)
    driver = Driver(DriverId("D1"), "Conductor")
    driver.assign(str(route.id))
    route.assign_driver(driver.id)
    route.start()
    assert route.status == RouteStatus.IN_PROGRESS
