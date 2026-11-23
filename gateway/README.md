# API Gateway

Este módulo publica la entrada HTTP del sistema en el puerto `8080`. Usa Eureka y Spring Cloud LoadBalancer para dirigir las rutas a sus microservicios:

| Rutas | Servicio |
| --- | --- |
| `/api/turnos/**`, `/api/disponibilidad/**` | `MS-TURNOS` |
| `/api/inventory/**` | `INVENTORY-SERVICE` |
| `/api/auth/**`, `/api/users/**` | `USERS-SERVICE` |
| `/api/v1/**` | `DELIVERY-SERVICE` |

También centraliza CORS y expone health y métricas en `/actuator/health` y `/actuator/prometheus`.

Las rutas de inventario y delivery verifican el Bearer token consultando `GET /api/auth/me` en `users-service`; las rutas de turnos aplican esa misma validación y autorización por rol dentro de `ms-turnos`, incluso si se accede directamente al servicio. Los endpoints de autenticación de `users-service` conservan su propia validación.

En Docker Desktop, el servicio de identidad usa por defecto `http://host.docker.internal:8000`. Para ejecutar el gateway directamente en el host, define `AUTH_SERVICE_BASE_URL=http://localhost:8000`. Si identidad no está disponible, las rutas protegidas fallan con `503`, no pasan sin validar.

Esta integración valida token y estado activo; MFA requiere modificar el flujo de identidad fuera de estos módulos. No expongas directamente otros puertos de microservicios en un despliegue público.
