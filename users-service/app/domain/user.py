from dataclasses import dataclass
from datetime import datetime


class UserDomainError(Exception):
    def __init__(self, status_code: int, detail: str):
        super().__init__(detail)
        self.status_code = status_code
        self.detail = detail


@dataclass
class UserEntity:
    id: int
    full_name: str
    email: str
    hashed_password: str
    role: str
    is_active: bool
    created_at: datetime
