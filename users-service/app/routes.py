from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user, require_admin
from app.models import User
from app.schemas import (
    LoginRequest,
    RoleUpdate,
    StatusUpdate,
    TokenResponse,
    UserCreate,
    UserResponse,
)
from app.security import ACCESS_TOKEN_MINUTES, create_access_token, hash_password, verify_password


auth_router = APIRouter(prefix="/api/auth", tags=["Autenticación"])
users_router = APIRouter(prefix="/api/users", tags=["Usuarios"])


@auth_router.post("/register", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
def register(payload: UserCreate, db: Session = Depends(get_db)) -> User:
    user = User(
        full_name=payload.full_name,
        email=str(payload.email),
        hashed_password=hash_password(payload.password),
    )
    db.add(user)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(status_code=409, detail="Ya existe una cuenta con ese correo.") from None
    db.refresh(user)
    return user


@auth_router.post("/login", response_model=TokenResponse)
def login(payload: LoginRequest, db: Session = Depends(get_db)) -> TokenResponse:
    user = db.scalar(select(User).where(User.email == str(payload.email)))
    if user is None or not verify_password(payload.password, user.hashed_password) or not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Correo o contraseña incorrectos.",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return TokenResponse(
        access_token=create_access_token(user.id),
        expires_in=ACCESS_TOKEN_MINUTES * 60,
        user=user,
    )


@auth_router.get("/me", response_model=UserResponse)
def get_profile(current_user: User = Depends(get_current_user)) -> User:
    return current_user


@users_router.get("", response_model=list[UserResponse])
def list_users(
    _admin: User = Depends(require_admin),
    db: Session = Depends(get_db),
) -> list[User]:
    return list(db.scalars(select(User).order_by(User.id)).all())


@users_router.post(
    "/pharmacists",
    response_model=UserResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_pharmacist(
    payload: UserCreate,
    _admin: User = Depends(require_admin),
    db: Session = Depends(get_db),
) -> User:
    pharmacist = User(
        full_name=payload.full_name,
        email=str(payload.email),
        hashed_password=hash_password(payload.password),
        role="farmaceutico",
    )
    db.add(pharmacist)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(status_code=409, detail="Ya existe una cuenta con ese correo.") from None
    db.refresh(pharmacist)
    return pharmacist


def prevent_last_admin_change(target: User, db: Session) -> None:
    if target.role != "admin" or not target.is_active:
        return
    active_admins = db.scalar(
        select(func.count()).select_from(User).where(User.role == "admin", User.is_active.is_(True))
    )
    if active_admins <= 1:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="No se puede quitar o desactivar al último administrador activo.",
        )


def get_target_user(user_id: int, db: Session) -> User:
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Usuario no encontrado.")
    return user


@users_router.patch("/{user_id}/role", response_model=UserResponse)
def update_role(
    user_id: int,
    payload: RoleUpdate,
    _admin: User = Depends(require_admin),
    db: Session = Depends(get_db),
) -> User:
    user = get_target_user(user_id, db)
    if payload.role != user.role:
        prevent_last_admin_change(user, db)
        user.role = payload.role
        db.commit()
        db.refresh(user)
    return user


@users_router.patch("/{user_id}/status", response_model=UserResponse)
def update_status(
    user_id: int,
    payload: StatusUpdate,
    _admin: User = Depends(require_admin),
    db: Session = Depends(get_db),
) -> User:
    user = get_target_user(user_id, db)
    if payload.is_active != user.is_active:
        if not payload.is_active:
            prevent_last_admin_change(user, db)
        user.is_active = payload.is_active
        db.commit()
        db.refresh(user)
    return user


@users_router.delete("/{user_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_user(
    user_id: int,
    admin: User = Depends(require_admin),
    db: Session = Depends(get_db),
) -> None:
    user = get_target_user(user_id, db)
    if user.id == admin.id:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="No puedes eliminar tu propia cuenta desde esta operación.",
        )
    prevent_last_admin_change(user, db)
    db.delete(user)
    db.commit()
