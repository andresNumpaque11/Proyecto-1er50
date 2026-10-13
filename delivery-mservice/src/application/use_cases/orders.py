from ..dtos import CreateDeliveryOrderCommand

from ...domain.delivery_order.entity import DeliveryOrder

from ...domain.shared.ids import OrderId

from ...domain.shared.value_objects import (
    Address,
    Priority,
)


class ReceiveDeliveryOrder:

    def __init__(
        self,
        orders,
        routes,
        drivers,
        outbox,
        planner,
        assignment,
        unit_of_work,
    ):
        self.orders = orders
        self.routes = routes
        self.drivers = drivers
        self.outbox = outbox
        self.planner = planner
        self.assignment = assignment
        self.unit_of_work = unit_of_work

    def execute(
        self,
        command: CreateDeliveryOrderCommand,
    ):

        order_id = OrderId(command.order_id)

        with self.unit_of_work.transaction() as session:

            existing = self.orders.get(
                order_id,
                session=session,
            )

            if existing:
                return existing

            order = DeliveryOrder.create(
                order_id=order_id,
                destination=Address(
                    command.street,
                    command.city,
                    command.postal_code,
                    command.latitude,
                    command.longitude,
                    command.reference,
                ),
                priority=Priority(command.priority),
                external_reference=command.external_reference,
            )

            order.mark_ready_for_route()

            self.orders.save(
                order,
                session=session,
            )

            self._events(
                order,
                session,
            )

            existing_route = self.routes.find_open_by_city(
                command.city,
                session=session,
            )

            routes = [existing_route] if existing_route else []

            route, _ = self.planner.plan(
                order,
                routes,
            )

            order.assign_to_route(route.id)

            self.orders.save(
                order,
                session=session,
            )

            self.routes.save(
                route,
                session=session,
            )

            self._events(
                route,
                session,
            )

            if route.status.value == "READY_FOR_ASSIGNMENT":

                self._assign(
                    route,
                    session,
                )

            return order

    def _assign(
        self,
        route,
        session,
    ):

        drivers = self.drivers.available(
            session=session,
        )

        driver = self.assignment.assign(
            route,
            drivers,
        )

        if not driver:
            return

        self.drivers.save(
            driver,
            session=session,
        )

        self.routes.save(
            route,
            session=session,
        )

        self._events(
            route,
            session,
        )

    def _events(
        self,
        aggregate,
        session,
    ):

        for event in aggregate.pull_events():

            self.outbox.add(
                event,
                session=session,
            )
