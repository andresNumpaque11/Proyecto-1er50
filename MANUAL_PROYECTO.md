# Manual de despliegue, pruebas y observabilidad

Este manual describe los cambios de `ms-turnos`, `api-gateway` y `gateway`, además de cómo levantar y revisar el sistema localmente en Windows.

## Cambios realizados

### `ms-turnos`

- Mantiene arquitectura hexagonal y DDD: dominio, casos de uso, puertos y adaptadores JPA/REST.
- Se separaron comandos de consulta (CQRS lógico). Las interfaces de comandos escriben y aplican reglas; los servicios de consulta leen turnos o disponibilidades. Ambas rutas aún comparten PostgreSQL: no hay una base de lectura separada.
- La cola prioriza turnos clínicos frente a normales y se reconstruye desde los turnos pendientes al iniciar.
- Publica en RabbitMQ `turno.solicitado`, `turno.cancelado` y `turno.atendido` después del commit.
- Para rutas `/api/**`, consulta `GET /api/auth/me` en `users-service`. Rechaza tokens inválidos, y responde `503` si el servicio de identidad no está disponible.
- Autoriza personal (`admin`, `farmaceutico`) a crear disponibilidades y atender turnos. Pacientes pueden solicitar turnos para sí mismos, consultar sus propios turnos y cancelar solo los propios.
- Expone OpenAPI/Swagger con esquema Bearer JWT y agrega pruebas de CQRS, autenticación, permisos y reglas de turnos.

### `gateway`

- Enruta por Eureka las rutas de turnos, inventario, usuarios y delivery.
- Valida el Bearer token de inventario y delivery consultando al servicio de identidad.
- Centraliza CORS, health y métricas de Actuator/Prometheus.
- Falla de forma cerrada: si la identidad no está disponible, no reenvía las rutas protegidas.

### `api-gateway`

- Actúa como servidor Eureka: registra y permite descubrir microservicios.
- Expone health y métricas para Actuator/Prometheus.
- No es el gateway HTTP; el enrutamiento de API está en el módulo `gateway`.

### Alcance pendiente

- MFA no se implementa en estos tres módulos: requiere cambios en el flujo de identidad de `users-service`.
- CQRS está separado lógicamente, pero lectura y escritura todavía usan la misma base y tablas.
- Prometheus no tiene configurado un exporter de RabbitMQ. Los mensajes se inspeccionan desde su consola.

## Requisitos

- Docker Desktop iniciado.
- Java 21.
- Maven (o el wrapper Maven donde esté disponible).
- Node.js y Python para los servicios que se ejecutan fuera de Docker.

## Iniciar el proyecto

Desde PowerShell, ve a la raíz del proyecto:

```powershell
cd "C:\ruta\al\proyecto"
```

Inicia la infraestructura definida en Docker Compose y recompila los módulos Java:

```powershell
docker compose -f Docker-compose.yml up -d --build
docker compose -f Docker-compose.yml ps
```

Si solo necesitas actualizar los módulos de esta entrega:

```powershell
docker compose -f Docker-compose.yml up -d --build ms-turnos gateway api-gateway
```

Los servicios `users-service` e `inventory-Service` se ejecutan en el host, de acuerdo con sus respectivas guías:

```powershell
# Terminal 1: API de usuarios
cd "C:\ruta\al\proyecto\users-service"
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload

# Terminal 2: inventario
cd "C:\ruta\al\proyecto\inventory-Service"
npm install
npm run dev
```

El frontend se inicia por separado:

```powershell
cd "C:\ruta\al\proyecto\users-service\frontend"
npm install
npm run dev
```

Delivery se inicia desde su propio compose:

```powershell
cd "C:\ruta\al\proyecto\delivery-mservice"
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose up -d --build
```

### Direcciones principales

| Componente | Dirección |
| --- | --- |
| Frontend | `http://localhost:5173` |
| API de usuarios / Swagger | `http://localhost:8000/docs` |
| Gateway de APIs | `http://localhost:8080` |
| `ms-turnos` directo | `http://localhost:8081` |
| Eureka | `http://localhost:8761` |
| Swagger de turnos | `http://localhost:8081/swagger-ui.html` |
| RabbitMQ Management | `http://localhost:15672` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3001` |

El gateway y `ms-turnos` llaman a usuarios mediante `AUTH_SERVICE_BASE_URL`. En Docker Desktop, el valor predeterminado es `http://host.docker.internal:8000`; al ejecutar esos servicios directamente en Windows, usa `http://localhost:8000`.

## Probar la aplicación

1. Inicia sesión en el frontend. El login devuelve un JWT.
2. Usa un usuario administrador o farmacéutico para crear disponibilidad. Un paciente no tiene permiso para esa operación.
3. Usa una cuenta paciente para seleccionar una disponibilidad y solicitar un turno.
4. Comprueba que los turnos urgentes aparecen con prioridad clínica.
5. Inicia sesión como personal y usa **Atender siguiente**; debe elegir primero los turnos clínicos.
6. Comprueba que un paciente puede cancelar sus turnos pendientes, pero no los de otra persona.
7. Prueba la API en Swagger. En **Authorize**, ingresa `Bearer <token>` obtenido desde `POST /api/auth/login`.

Pruebas automatizadas Java, desde la raíz:

```powershell
mvn -f ms-turnos\pom.xml test
mvn -f gateway\pom.xml test
mvn -f api-gateway\pom.xml test
```

## RabbitMQ: inspeccionar eventos

1. Abre `http://localhost:15672` e inicia sesión con `guest` / `guest`.
2. En **Exchanges**, busca `sigtam.turnos`.
3. En **Queues and Streams**, crea una cola temporal, por ejemplo `turnos.prueba`.
4. Enlázala al exchange `sigtam.turnos` con el binding key `turno.*`.
5. Solicita, atiende o cancela un turno desde la aplicación.
6. Abre la cola y selecciona **Get messages**. Se deben ver mensajes JSON para las claves `turno.solicitado`, `turno.atendido` o `turno.cancelado`.

La cola debe enlazarse antes de generar los eventos: RabbitMQ no conserva los mensajes de un exchange si no había una cola enlazada. La aplicación declara el exchange, pero no declara una cola de consumo.

## Prometheus: revisar recolección

1. Abre `http://localhost:9090/targets`.
2. Revisa los jobs `ms-turnos`, `gateway` y `api-gateway`; el estado esperado es **UP**.
3. En **Graph**, ejecuta `up`. Cada target disponible debe devolver `1`.
4. Ejecuta `http_server_requests_seconds_count` para ver el conteo de solicitudes HTTP, después de usar la aplicación.
5. Los targets scrapean cada 15 segundos, así que los cambios pueden tardar un intervalo en aparecer.

Los endpoints de métricas de los servicios son `/actuator/prometheus`.

## Grafana: visualizar métricas

1. Abre `http://localhost:3001`. Credenciales locales iniciales: `admin` / `admin`.
2. En **Connections → Data sources → Add data source**, selecciona **Prometheus**.
3. Configura la URL `http://prometheus:9090` y selecciona **Save & test**.
4. Crea un dashboard y agrega un panel con consulta `up`.
5. Agrega otro panel con `http_server_requests_seconds_count` para observar solicitudes.

Grafana persiste su configuración en un volumen Docker. El dashboard no se provisiona automáticamente; esta guía describe cómo crearlo manualmente.

## Detener servicios

Desde la raíz:

```powershell
docker compose -f Docker-compose.yml down
```

Para detener también delivery:

```powershell
cd "C:\ruta\al\proyecto\delivery-mservice"
docker compose down
```

No agregues `-v` si deseas conservar los datos de las bases y volúmenes.
