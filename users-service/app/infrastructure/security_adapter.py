from app.security import (
    create_access_token,
    hash_password,
    verify_password,
)
from app.security import JWT_SECRET, ALGORITHM
import jwt


class JwtAndPasswordSecurityAdapter:
    def hash_password(self, password: str) -> str:
        return hash_password(password)

    def verify_password(self, password: str, hashed_password: str) -> bool:
        return verify_password(password, hashed_password)

    def create_access_token(self, user_id: int) -> str:
        return create_access_token(user_id)

    def user_id_from_token(self, token: str) -> int:
        try:
            payload = jwt.decode(token, JWT_SECRET, algorithms=[ALGORITHM])
            return int(payload["sub"])
        except (jwt.InvalidTokenError, KeyError, TypeError, ValueError) as error:
            raise ValueError("Invalid token") from error
