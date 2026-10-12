import os
from contextlib import asynccontextmanager
from typing import AsyncIterator

from dotenv import load_dotenv

load_dotenv()

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import select

from app.database import Base, SessionLocal, engine
from app.models import User
from app.routes import auth_router, users_router
from app.security import hash_password, validate_auth_settings


def seed_admin() -> None:
    email = os.getenv("ADMIN_EMAIL")
    password = os.getenv("ADMIN_PASSWORD")
    if not email and not password:
        return
    if not email or not password:
        raise RuntimeError("ADMIN_EMAIL y ADMIN_PASSWORD deben configurarse juntos.")
    if len(password) < 8:
        raise RuntimeError("ADMIN_PASSWORD debe tener al menos 8 caracteres.")

    with SessionLocal() as db:
        normalized_email = email.strip().lower()
        user = db.scalar(select(User).where(User.email == normalized_email))
        if user is None:
            db.add(
                User(
                    full_name=os.getenv("ADMIN_NAME", "Administrator").strip(),
                    email=normalized_email,
                    hashed_password=hash_password(password),
                    role="admin",
                )
            )
            db.commit()
        elif user.role != "admin":
            raise RuntimeError("ADMIN_EMAIL ya existe y no tiene el rol de administrador.")


@asynccontextmanager
async def lifespan(_app: FastAPI) -> AsyncIterator[None]:
    validate_auth_settings()
    Base.metadata.create_all(bind=engine)
    seed_admin()
    yield


app = FastAPI(
    title="Users Service",
    description="Autenticación JWT y gestión de usuarios con control de acceso por roles.",
    version="1.0.0",
    lifespan=lifespan,
)
app.add_middleware(
    CORSMiddleware,
    allow_origins=[os.getenv("FRONTEND_ORIGIN", "http://localhost:5173")],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["Authorization", "Content-Type"],
)
app.include_router(auth_router)
app.include_router(users_router)


@app.get("/health", tags=["Salud"])
def health() -> dict[str, str]:
    return {"status": "ok"}
