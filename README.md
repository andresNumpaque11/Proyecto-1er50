# Proyecto-1er50
microservicios gestion de medicamentos

## Solicitudes y despacho de medicamentos

El servicio de inventario expone estas rutas:

- `POST /api/inventory/medication-requests`: crea una solicitud pendiente. Recibe `medication` (id del medicamento), `quantity` (entero positivo) y `requestedBy` (solicitante).
- `POST /api/inventory/medication-requests/:id/dispatch`: despacha la solicitud completa. Consume primero los lotes próximos a vencer y no despacha si el stock vigente es insuficiente.

## Gestión de usuarios y autenticación

El microservicio `users-service` implementa registro e inicio de sesión con JWT y control de acceso para pacientes, farmacéuticos y administradores. El registro público crea pacientes; solo el administrador puede crear cuentas de farmacéutico. Su API utiliza FastAPI y SQLite por defecto; el frontend está construido con React.

Consulta [users-service/README.md](./users-service/README.md) para instalar dependencias y ejecutar ambos componentes. La documentación OpenAPI se publica en `/docs`.

## Monitoreo

Inventario y usuarios exponen métricas Prometheus en `/metrics`. El archivo `prometheus.yml` configura el scrape de sus puertos locales predeterminados (`7879` y `8000`). Con los servicios activos, inicia el stack con `docker compose -f Docker-compose.yml up -d prometheus grafana`. Prometheus queda disponible en `http://localhost:9090`; Grafana en `http://localhost:3001` (usuario y contraseña iniciales: `admin`/`admin`). El datasource y el dashboard HTTP de SIGTAM se provisionan automáticamente.
