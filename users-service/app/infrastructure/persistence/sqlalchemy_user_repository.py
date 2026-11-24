from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.domain.user import UserDomainError, UserEntity
from app.infrastructure.db.models import User


class SqlAlchemyUserRepository:
    def __init__(self, db: Session):
        self.db = db

    @staticmethod
    def to_entity(user: User) -> UserEntity:
        return UserEntity(
            id=user.id,
            full_name=user.full_name,
            email=user.email,
            hashed_password=user.hashed_password,
            role=user.role,
            is_active=user.is_active,
            created_at=user.created_at,
        )

    def create(self, full_name: str, email: str, hashed_password: str, role: str = "paciente") -> UserEntity:
        user = User(full_name=full_name, email=email, hashed_password=hashed_password, role=role)
        self.db.add(user)
        try:
            self.db.commit()
        except IntegrityError:
            self.db.rollback()
            raise UserDomainError(409, "Ya existe una cuenta con ese correo.") from None
        self.db.refresh(user)
        return self.to_entity(user)

    def find_by_email(self, email: str) -> UserEntity | None:
        user = self.db.scalar(select(User).where(User.email == email))
        return self.to_entity(user) if user else None

    def find_by_id(self, user_id: int) -> UserEntity | None:
        user = self.db.get(User, user_id)
        return self.to_entity(user) if user else None

    def list_all(self) -> list[UserEntity]:
        return [self.to_entity(user) for user in self.db.scalars(select(User).order_by(User.id)).all()]

    def update_role(self, user: UserEntity, role: str) -> UserEntity:
        orm_user = self.db.get(User, user.id)
        orm_user.role = role
        self.db.commit()
        self.db.refresh(orm_user)
        return self.to_entity(orm_user)

    def update_status(self, user: UserEntity, is_active: bool) -> UserEntity:
        orm_user = self.db.get(User, user.id)
        orm_user.is_active = is_active
        self.db.commit()
        self.db.refresh(orm_user)
        return self.to_entity(orm_user)

    def delete(self, user: UserEntity) -> None:
        orm_user = self.db.get(User, user.id)
        self.db.delete(orm_user)
        self.db.commit()

    def count_active_admins(self) -> int:
        return self.db.scalar(
            select(func.count()).select_from(User).where(User.role == "admin", User.is_active.is_(True))
        )
