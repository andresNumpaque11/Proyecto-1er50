from ...domain.shared.ids import RouteId, OrderId


class StartRoute:
    def __init__(self, routes, drivers, outbox):
        self.routes = routes
        self.drivers = drivers
        self.outbox = outbox

    def execute(self, route_id):
        r = self.routes.get(RouteId.from_string(route_id))
        if not r:
            raise ValueError("Ruta no encontrada")
        r.start()
        if r.driver_id:
            d = self.drivers.get(r.driver_id)
            if d:
                d.start_route()
                self.drivers.save(d)
        self.routes.save(r)
        self._events(r)
        return r

    def _events(self, a):
        for e in a.pull_events():
            self.outbox.add(e)


class CompleteStop:
    def __init__(self, routes, orders, drivers, outbox):
        self.routes = routes
        self.orders = orders
        self.drivers = drivers
        self.outbox = outbox

    def execute(self, route_id, order_id):
        r = self.routes.get(RouteId.from_string(route_id))
        o = self.orders.get(OrderId(order_id))
        if not r or not o:
            raise ValueError("Ruta u orden no encontrada")
        r.complete_stop(o.id)
        o.complete()
        self.routes.save(r)
        self.orders.save(o)
        if r.status.value == "COMPLETED" and r.driver_id:
            d = self.drivers.get(r.driver_id)
            if d:
                d.become_available()
                self.drivers.save(d)
        self._events(r)
        self._events(o)
        return r

    def _events(self, a):
        for e in a.pull_events():
            self.outbox.add(e)
