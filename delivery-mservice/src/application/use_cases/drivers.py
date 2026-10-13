from ...domain.driver.entity import Driver
from ...domain.shared.ids import DriverId


class RegisterDriver:
    def __init__(self, repo):
        self.repo = repo

    def execute(self, c):
        d = Driver(DriverId(c.driver_id), c.name, c.phone)
        self.repo.save(d)
        return d


class ChangeAvailability:
    def __init__(self, repo):
        self.repo = repo

    def execute(self, c):
        d = self.repo.get(DriverId(c.driver_id))
        if not d:
            raise ValueError("Domiciliario no encontrado")
        d.become_available() if c.available else d.make_unavailable()
        self.repo.save(d)
        return d
