from typing import Annotated

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from app.application.user_use_cases import UserUseCases
from app.infrastructure.config.database import get_db
from app.domain.user import UserDomainError, UserEntity
from app.infrastructure.security.security_adapter import JwtAndPasswordSecurityAdapter
from app.infrastructure.persistence.sqlalchemy_user_repository import SqlAlchemyUserRepository


bearer_scheme = HTTPBearer(auto_error=False)


def get_user_use_cases(db: Annotated[Session, Depends(get_db)]) -> UserUseCases:
    return UserUseCases(
        SqlAlchemyUserRepository(db),
        JwtAndPasswordSecurityAdapter(),
    )


def get_current_user(
    credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(bearer_scheme)],
    use_cases: Annotated[UserUseCases, Depends(get_user_use_cases)],
) -> UserEntity:
    if credentials is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Credenciales no válidas o token expirado.",
            headers={"WWW-Authenticate": "Bearer"},
        )
    try:
        return use_cases.get_profile_by_token(credentials.credentials)
    except UserDomainError as error:
        raise HTTPException(
            status_code=error.status_code,
            detail=error.detail,
            headers={"WWW-Authenticate": "Bearer"},
        ) from None


def require_admin(
    current_user: Annotated[UserEntity, Depends(get_current_user)],
) -> UserEntity:
    if current_user.role != "admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Se requiere el rol de administrador.",
        )
    return current_user
