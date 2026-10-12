import os
from datetime import datetime, timedelta, timezone

import jwt
from pwdlib import PasswordHash


JWT_SECRET = os.getenv("JWT_SECRET", "")
ACCESS_TOKEN_MINUTES = int(os.getenv("ACCESS_TOKEN_MINUTES", "30"))
ALGORITHM = "HS256"
password_hash = PasswordHash.recommended()


def validate_auth_settings() -> None:
    if len(JWT_SECRET) < 32:
        raise RuntimeError("JWT_SECRET debe tener al menos 32 caracteres.")
    if ACCESS_TOKEN_MINUTES < 1:
        raise RuntimeError("ACCESS_TOKEN_MINUTES debe ser un entero positivo.")


def hash_password(password: str) -> str:
    return password_hash.hash(password)


def verify_password(password: str, hashed_password: str) -> bool:
    return password_hash.verify(password, hashed_password)


def create_access_token(user_id: int) -> str:
    validate_auth_settings()
    now = datetime.now(timezone.utc)
    payload = {
        "sub": str(user_id),
        "iat": now,
        "exp": now + timedelta(minutes=ACCESS_TOKEN_MINUTES),
    }
    return jwt.encode(payload, JWT_SECRET, algorithm=ALGORITHM)
