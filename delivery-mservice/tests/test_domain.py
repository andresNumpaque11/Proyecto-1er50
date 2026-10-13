from src.domain.route.entity import Route
from src.domain.route.status import RouteStatus
from src.domain.shared.ids import OrderId

def test_route_is_ready_at_capacity():
 r=Route.create("Tunja",2,600); r.add_stop(OrderId("O1")); r.add_stop(OrderId("O2")); assert r.status==RouteStatus.READY_FOR_ASSIGNMENT
