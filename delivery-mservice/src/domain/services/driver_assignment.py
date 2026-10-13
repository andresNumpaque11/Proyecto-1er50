from abc import ABC, abstractmethod
from ..driver.entity import Driver
from ..route.entity import Route


class DriverSelectionStrategy(ABC):
    @abstractmethod
    def select(
        self,
        drivers: list[Driver],
        route: Route,
    ) -> Driver | None: ...


class FirstAvailableDriverStrategy(DriverSelectionStrategy):
    def select(
        self,
        drivers: list[Driver],
        route: Route,
    ) -> Driver | None:

        if not drivers:
            return None

        return drivers[0]


class DriverAssignmentService:
    def __init__(
        self,
        selection_strategy: DriverSelectionStrategy,
    ):
        self.selection_strategy = selection_strategy

    def assign(
        self,
        route: Route,
        drivers: list[Driver],
    ) -> Driver | None:

        driver = self.selection_strategy.select(
            drivers,
            route,
        )

        if driver is None:
            return None

        driver.assign(str(route.id))

        route.assign_driver(driver.id)

        return driver
