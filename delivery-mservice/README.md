<<<<<<< HEAD
# Proyecto-1er50
microservicios gestion de medicamentos

## Solicitudes y despacho de medicamentos

El servicio de inventario expone estas rutas:

- `POST /api/inventory/medication-requests`: crea una solicitud pendiente. Recibe `medication` (id del medicamento), `quantity` (entero positivo) y `requestedBy` (solicitante).
- `POST /api/inventory/medication-requests/:id/dispatch`: despacha la solicitud completa. Consume primero los lotes próximos a vencer y no despacha si el stock vigente es insuficiente.

## Gestión de usuarios y autenticación

El microservicio `users-service` implementa registro e inicio de sesión con JWT y control de acceso para pacientes, farmacéuticos y administradores. El registro público crea pacientes; solo el administrador puede crear cuentas de farmacéutico. Su API utiliza FastAPI y SQLite por defecto; el frontend está construido con React.

Consulta [users-service/README.md](./users-service/README.md) para instalar dependencias y ejecutar ambos componentes. La documentación OpenAPI se publica en `/docs`.
=======
# Microservicio de Domicilios

Microservicio listo para levantar con Docker, con DDD + arquitectura hexagonal, MongoDB, planificación automática, asignación automática y Outbox.

## Flujo

`POST /api/v1/delivery-orders` recibe una orden del microservicio de farmacia. El consumidor **no envía route_id ni order_ids para construir una ruta**. El servicio busca una ruta abierta de la misma ciudad; si no existe, crea una. Cuando llega a `ROUTE_MAX_STOPS=10`, la ruta pasa automáticamente a `READY_FOR_ASSIGNMENT` y se busca un domiciliario `AVAILABLE`. Si no llega a 10, el worker la cierra por tiempo después de `ROUTE_MAX_WAIT_SECONDS`.

## Docker

```bash
cp .env.example .env
docker compose up --build
```

Swagger: http://localhost:8000/docs
Health: http://localhost:8000/health
Readiness: http://localhost:8000/ready

## Prueba

1. Registrar domiciliario:
```bash
curl -X POST http://localhost:8000/api/v1/drivers -H 'Content-Type: application/json' -d '{"driver_id":"D1","name":"Carlos","phone":"3000000000"}'
```
2. Crear órdenes en la misma ciudad. No se envía ninguna ruta:
```bash
curl -X POST http://localhost:8000/api/v1/delivery-orders -H 'Content-Type: application/json' -d '{"order_id":"ORD-001","destination":{"street":"Carrera 10 #20-30","city":"Tunja"},"priority":"NORMAL"}'
```
3. Al llegar a 10 órdenes compatibles, el sistema asigna D1 automáticamente.
4. Consultar la ruta con `GET /api/v1/routes/{route_id}`.
5. Iniciar con `POST /api/v1/routes/{route_id}/start`.
6. Completar cada parada con `POST /api/v1/routes/{route_id}/stops/{order_id}/complete`.

## Eventos / integración

Los eventos se guardan en `outbox`. Por defecto se escriben en logs. Para enviar a otro microservicio mediante HTTP, configurar:

`EVENT_PUBLISHER_MODE=http`
`EVENT_PUBLISHER_URL=http://otro-servicio:8000/api/v1/events`

En producción, el puerto `EventPublisher` debe implementarse sobre el broker elegido (Kafka, RabbitMQ, etc.).

## Nota de diseño

La estrategia inicial agrupa por ciudad y capacidad. Está detrás de `RoutePlanningStrategy`, por lo que se puede sustituir por una estrategia geográfica real sin cambiar los casos de uso. La selección de domiciliario también usa `DriverSelectionStrategy` y puede evolucionar a distancia, zona, carga, horario, etc.
>>>>>>> 0fc3867 (feat: implement delivery microservice)
