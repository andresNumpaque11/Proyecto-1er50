# Microservicio de turnos

## Arquitectura

`ms-turnos` aplica arquitectura hexagonal y DDD. La capa de dominio modela turnos, disponibilidades y prioridades; los controladores REST son adaptadores de entrada y los adaptadores JPA conectan los puertos de persistencia con PostgreSQL.

También separa las operaciones de escritura y lectura mediante CQRS lógico:

- **Comandos:** `GestionarTurnosUseCase` y `GestionarDisponibilidadUseCase`, implementados por servicios que aplican las reglas de negocio y escriben en PostgreSQL.
- **Consultas:** `ConsultarTurnosQuery` y `ConsultarDisponibilidadQuery`, implementadas por servicios de lectura independientes y puertos de consulta.
- Los contratos HTTP y las rutas existentes se conservan.

Por ahora, comandos y consultas comparten PostgreSQL y sus tablas. La separación es lógica; no hay una base de lectura independiente ni proyecciones eventualmente consistentes.

Los eventos `turno.solicitado`, `turno.cancelado` y `turno.atendido` se publican en RabbitMQ después del commit. La cola de atención prioriza turnos clínicos y se reconstruye desde PostgreSQL al iniciar.

## Autenticación

Las rutas `/api/**` requieren un token Bearer activo. `ms-turnos` consulta `GET /api/auth/me` en `users-service`, que verifica la firma JWT y que la cuenta siga activa. Los administradores y farmacéuticos pueden crear disponibilidades y atender turnos; los pacientes solo pueden consultar sus propios turnos, solicitar turnos para sí mismos y cancelar turnos propios. Swagger muestra el botón **Authorize** para ingresar el token obtenido desde `POST /api/auth/login`.

En Docker Desktop, la URL predeterminada del servicio de identidad es `http://host.docker.internal:8000`. Para ejecutar `ms-turnos` directamente en el host, configura `AUTH_SERVICE_BASE_URL=http://localhost:8000`. Si el servicio de identidad no responde, se rechazan las solicitudes protegidas con `503`; no se acepta acceso sin validar.

MFA requiere cambios al flujo de autenticación de `users-service`, fuera del alcance de este módulo.

## Ejecución y documentación API

Desde este directorio:

```powershell
.\mvnw.cmd spring-boot:run
```

La API escucha en `http://localhost:8081`. La documentación OpenAPI/Swagger está disponible en:

- `http://localhost:8081/swagger-ui.html`
- `http://localhost:8081/v3/api-docs`

Con Docker Compose, el gateway publica `http://localhost:8080`; las rutas de turnos y disponibilidades se conservan allí.

## Pruebas

```powershell
.\mvnw.cmd test
```
