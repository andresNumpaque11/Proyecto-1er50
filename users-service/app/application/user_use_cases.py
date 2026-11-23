from app.application.ports import SecurityPort, UserRepositoryPort
from app.domain.user import UserDomainError, UserEntity


class UserUseCases:
    def __init__(self, repository: UserRepositoryPort, security: SecurityPort):
        self.repository = repository
        self.security = security

    def register(self, full_name: str, email: str, password: str) -> UserEntity:
        if self.repository.find_by_email(email) is not None:
            raise UserDomainError(409, "Ya existe una cuenta con ese correo.")
        return self.repository.create(
            full_name,
            email,
            self.security.hash_password(password),
            "paciente",
        )

    def login(self, email: str, password: str) -> tuple[UserEntity, str]:
        user = self.repository.find_by_email(email)
        if (
            user is None
            or not self.security.verify_password(password, user.hashed_password)
            or not user.is_active
        ):
            raise UserDomainError(401, "Correo o contraseña incorrectos.")
        return user, self.security.create_access_token(user.id)

    def get_profile_by_token(self, token: str) -> UserEntity:
        try:
            user_id = self.security.user_id_from_token(token)
        except (TypeError, ValueError):
            raise UserDomainError(401, "Credenciales no válidas o token expirado.") from None
        user = self.repository.find_by_id(user_id)
        if user is None or not user.is_active:
            raise UserDomainError(401, "Credenciales no válidas o token expirado.")
        return user

    def list_users(self) -> list[UserEntity]:
        return self.repository.list_all()

    def create_pharmacist(self, full_name: str, email: str, password: str) -> UserEntity:
        if self.repository.find_by_email(email) is not None:
            raise UserDomainError(409, "Ya existe una cuenta con ese correo.")
        return self.repository.create(
            full_name,
            email,
            self.security.hash_password(password),
            "farmaceutico",
        )

    def _prevent_last_admin_change(self, target: UserEntity) -> None:
        if target.role == "admin" and target.is_active:
            if self.repository.count_active_admins() <= 1:
                raise UserDomainError(
                    409,
                    "No se puede quitar o desactivar al último administrador activo.",
                )

    def update_role(self, user_id: int, role: str) -> UserEntity:
        user = self.repository.find_by_id(user_id)
        if user is None:
            raise UserDomainError(404, "Usuario no encontrado.")
        if role != user.role:
            self._prevent_last_admin_change(user)
            user = self.repository.update_role(user, role)
        return user

    def update_status(self, user_id: int, is_active: bool) -> UserEntity:
        user = self.repository.find_by_id(user_id)
        if user is None:
            raise UserDomainError(404, "Usuario no encontrado.")
        if is_active != user.is_active:
            if not is_active:
                self._prevent_last_admin_change(user)
            user = self.repository.update_status(user, is_active)
        return user

    def delete_user(self, user_id: int, admin_id: int) -> None:
        user = self.repository.find_by_id(user_id)
        if user is None:
            raise UserDomainError(404, "Usuario no encontrado.")
        if user.id == admin_id:
            raise UserDomainError(409, "No puedes eliminar tu propia cuenta desde esta operación.")
        self._prevent_last_admin_change(user)
        self.repository.delete(user)
