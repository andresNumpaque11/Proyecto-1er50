# Proyecto-1er50
microservicios gestion de medicamentos

## Solicitudes y despacho de medicamentos

El servicio de inventario expone estas rutas:

- `POST /api/inventory/medication-requests`: crea una solicitud pendiente. Recibe `medication` (id del medicamento), `quantity` (entero positivo) y `requestedBy` (solicitante).
- `POST /api/inventory/medication-requests/:id/dispatch`: despacha la solicitud completa. Consume primero los lotes próximos a vencer y no despacha si el stock vigente es insuficiente.

## Gestión de usuarios y autenticación

El microservicio `users-service` implementa registro e inicio de sesión con JWT, perfiles de usuario y control administrativo de roles y cuentas. Su API utiliza FastAPI y SQLite por defecto; el frontend está construido con React.

Consulta [users-service/README.md](./users-service/README.md) para instalar dependencias y ejecutar ambos componentes. La documentación OpenAPI se publica en `/docs`.
