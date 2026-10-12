# Microservicio de usuarios

Servicio de autenticación y control de acceso para el proyecto de gestión de medicamentos. Incluye API REST con FastAPI, tokens de acceso JWT, persistencia SQLite y una interfaz React para usuarios y administradores.

## Requisitos

- Python 3.10 o superior
- Node.js 20 o superior y npm

## Configuración y ejecución

Desde `users-service`, crea el archivo `.env` a partir de `.env.example` y reemplaza `JWT_SECRET` por un secreto aleatorio de al menos 32 caracteres. Para generar uno, puedes usar `python -c "import secrets; print(secrets.token_urlsafe(48))"`.

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload
```

En otra terminal:

```powershell
cd frontend
npm install
npm run dev
```

La API queda en `http://localhost:8000`; su documentación interactiva está en `/docs`. La aplicación web se sirve en `http://localhost:5173`. Por defecto se utiliza SQLite en `users.db`. Para usar otra base de datos SQLAlchemy, configura `DATABASE_URL`.

El administrador inicial se crea al iniciar el backend si se configuran juntos `ADMIN_EMAIL` y `ADMIN_PASSWORD`. Si ambas variables están vacías, no se crea automáticamente ninguna cuenta administrativa. El registro público siempre asigna el rol `paciente`; solo un administrador puede crear cuentas `farmaceutico`. Al iniciar, el servicio migra automáticamente las cuentas existentes con el rol `user` a `paciente`.

## API

| Método | Ruta | Acceso | Descripción |
| --- | --- | --- | --- |
| `GET` | `/health` | Público | Estado del servicio |
| `POST` | `/api/auth/register` | Público | Crear cuenta de paciente |
| `POST` | `/api/auth/login` | Público | Autenticar y recibir un access token JWT |
| `GET` | `/api/auth/me` | Autenticado | Consultar el perfil propio |
| `GET` | `/api/users` | Admin | Listar usuarios |
| `POST` | `/api/users/pharmacists` | Admin | Crear una cuenta de farmacéutico |
| `PATCH` | `/api/users/{id}/role` | Admin | Cambiar entre `paciente`, `farmaceutico` y `admin` |
| `PATCH` | `/api/users/{id}/status` | Admin | Activar o desactivar una cuenta |
| `DELETE` | `/api/users/{id}` | Admin | Eliminar una cuenta |

Envía el token en `Authorization: Bearer <access_token>`. Los tokens duran 30 minutos por defecto y el rol/estado se consultan en la base de datos en cada petición protegida, así que los cambios de permisos y desactivaciones tienen efecto inmediato. El último administrador activo no se puede degradar, desactivar ni eliminar.

## Pruebas

```powershell
pytest
```
