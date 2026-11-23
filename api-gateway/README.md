# Eureka Server

Este módulo ejecuta el registro de servicios Eureka en el puerto `8761`. Los microservicios se registran aquí para que el API Gateway pueda resolverlos por nombre y distribuir solicitudes entre instancias.

- Consola Eureka: `http://localhost:8761`
- Health: `http://localhost:8761/actuator/health`
- Métricas Prometheus: `http://localhost:8761/actuator/prometheus`

El gateway HTTP y sus rutas están en el módulo separado `gateway`; este servicio no enruta peticiones de aplicación.
