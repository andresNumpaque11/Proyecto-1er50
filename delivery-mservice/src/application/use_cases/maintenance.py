from datetime import datetime, timezone


class ProcessRoutes:

    def __init__(
        self,
        routes,
        drivers,
        outbox,
        assignment,
        unit_of_work,
    ):
        self.routes = routes
        self.drivers = drivers
        self.outbox = outbox
        self.assignment = assignment
        self.unit_of_work = unit_of_work

    def execute(self):

        with self.unit_of_work.transaction() as session:

            now = datetime.now(timezone.utc)

            expired_routes = self.routes.find_expired(
                now,
                session=session,
            )

            for route in expired_routes:

                route.mark_ready_for_assignment()

                self.routes.save(
                    route,
                    session=session,
                )

                self._events(
                    route,
                    session,
                )

            ready_routes = self.routes.find_ready(
                session=session,
            )

            for route in ready_routes:

                if route.driver_id:
                    continue

                driver = self.assignment.assign(
                    route,
                    self.drivers.available(
                        session=session,
                    ),
                )

                if not driver:
                    continue

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
