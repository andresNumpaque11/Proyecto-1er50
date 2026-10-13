from abc import ABC, abstractmethod
from math import radians, sin, cos, sqrt, atan2

from ..route.entity import Route


class RoutePlanningStrategy(ABC):

    @abstractmethod
    def choose(self, order, routes): ...


class CityAndCapacityStrategy(RoutePlanningStrategy):

    def choose(self, order, routes):
        candidates = [
            route
            for route in routes
            if route.status.value == "OPEN"
            and route.city.casefold() == order.destination.city.casefold()
            and not route.is_full
        ]

        if not candidates:
            return None

        return min(candidates, key=lambda route: route.stop_count)


class GeographicRoutePlanningStrategy:

    def choose_position(self, order, route):

        if not route.stops:
            return 1

        new_lat = order.destination.latitude
        new_lon = order.destination.longitude

        best_position = 1
        best_cost = float("inf")

        # Insertar antes de la primera parada
        first = route.stops[0]

        cost = self._distance(
            new_lat,
            new_lon,
            first.latitude,
            first.longitude,
        )

        if cost < best_cost:
            best_cost = cost
            best_position = 1

        # Evaluar inserciones entre paradas existentes
        for index in range(len(route.stops) - 1):

            current_stop = route.stops[index]
            next_stop = route.stops[index + 1]

            old_distance = self._distance(
                current_stop.latitude,
                current_stop.longitude,
                next_stop.latitude,
                next_stop.longitude,
            )

            new_distance = self._distance(
                current_stop.latitude,
                current_stop.longitude,
                new_lat,
                new_lon,
            ) + self._distance(
                new_lat,
                new_lon,
                next_stop.latitude,
                next_stop.longitude,
            )

            cost = new_distance - old_distance

            if cost < best_cost:
                best_cost = cost
                best_position = index + 2

        # Insertar después de la última parada
        last = route.stops[-1]

        cost = self._distance(
            last.latitude,
            last.longitude,
            new_lat,
            new_lon,
        )

        if cost < best_cost:
            best_position = len(route.stops) + 1

        return best_position

    @staticmethod
    def _distance(lat1, lon1, lat2, lon2):

        earth_radius = 6371.0

        lat1 = radians(lat1)
        lon1 = radians(lon1)

        lat2 = radians(lat2)
        lon2 = radians(lon2)

        delta_lat = lat2 - lat1
        delta_lon = lon2 - lon1

        a = sin(delta_lat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(delta_lon / 2) ** 2

        c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earth_radius * c


class RoutePlanningService:

    def __init__(
        self,
        strategy,
        max_stops,
        max_wait,
        geographic_strategy=None,
    ):
        self.strategy = strategy
        self.max_stops = max_stops
        self.max_wait = max_wait
        self.geographic_strategy = (
            geographic_strategy or GeographicRoutePlanningStrategy()
        )

    def plan(self, order, routes):

        route = self.strategy.choose(order, routes)

        created = False

        if route is None:
            route = Route.create(
                order.destination.city,
                self.max_stops,
                self.max_wait,
            )

            created = True

        position = self.geographic_strategy.choose_position(
            order,
            route,
        )

        route.add_stop(
            order.id,
            order.destination.latitude,
            order.destination.longitude,
            position,
        )

        return route, created
