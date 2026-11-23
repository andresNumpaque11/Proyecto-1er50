from fastapi import APIRouter, Depends, HTTPException, status

from app.application.user_use_cases import UserUseCases
from app.dependencies import get_current_user, get_user_use_cases, require_admin
from app.domain.user import UserDomainError, UserEntity
from app.schemas import (
    LoginRequest,
    RoleUpdate,
    StatusUpdate,
    TokenResponse,
    UserCreate,
    UserResponse,
)
from app.security import ACCESS_TOKEN_MINUTES


auth_router = APIRouter(prefix="/api/auth", tags=["Autenticación"])
users_router = APIRouter(prefix="/api/users", tags=["Usuarios"])


def raise_domain_error(error: UserDomainError) -> None:
    raise HTTPException(status_code=error.status_code, detail=error.detail) from None


@auth_router.post("/register", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
def register(payload: UserCreate, use_cases: UserUseCases = Depends(get_user_use_cases)) -> UserEntity:
    try:
        return use_cases.register(payload.full_name, str(payload.email), payload.password)
    except UserDomainError as error:
        raise_domain_error(error)


@auth_router.post("/login", response_model=TokenResponse)
def login(payload: LoginRequest, use_cases: UserUseCases = Depends(get_user_use_cases)) -> TokenResponse:
    try:
        user, access_token = use_cases.login(str(payload.email), payload.password)
        return TokenResponse(
            access_token=access_token,
            expires_in=ACCESS_TOKEN_MINUTES * 60,
            user=UserResponse.model_validate(user),
        )
    except UserDomainError as error:
        raise_domain_error(error)


@auth_router.get("/me", response_model=UserResponse)
def get_profile(current_user: UserEntity = Depends(get_current_user)) -> UserEntity:
    return current_user


@users_router.get("", response_model=list[UserResponse])
def list_users(
    _admin: UserEntity = Depends(require_admin),
    use_cases: UserUseCases = Depends(get_user_use_cases),
) -> list[UserEntity]:
    return use_cases.list_users()


@users_router.post(
    "/pharmacists",
    response_model=UserResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_pharmacist(
    payload: UserCreate,
    _admin: UserEntity = Depends(require_admin),
    use_cases: UserUseCases = Depends(get_user_use_cases),
) -> UserEntity:
    try:
        return use_cases.create_pharmacist(payload.full_name, str(payload.email), payload.password)
    except UserDomainError as error:
        raise_domain_error(error)


@users_router.patch("/{user_id}/role", response_model=UserResponse)
def update_role(
    user_id: int,
    payload: RoleUpdate,
    _admin: UserEntity = Depends(require_admin),
    use_cases: UserUseCases = Depends(get_user_use_cases),
) -> UserEntity:
    try:
        return use_cases.update_role(user_id, payload.role)
    except UserDomainError as error:
        raise_domain_error(error)


@users_router.patch("/{user_id}/status", response_model=UserResponse)
def update_status(
    user_id: int,
    payload: StatusUpdate,
    _admin: UserEntity = Depends(require_admin),
    use_cases: UserUseCases = Depends(get_user_use_cases),
) -> UserEntity:
    try:
        return use_cases.update_status(user_id, payload.is_active)
    except UserDomainError as error:
        raise_domain_error(error)


@users_router.delete("/{user_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_user(
    user_id: int,
    admin: UserEntity = Depends(require_admin),
    use_cases: UserUseCases = Depends(get_user_use_cases),
) -> None:
    try:
        use_cases.delete_user(user_id, admin.id)
    except UserDomainError as error:
        raise_domain_error(error)
