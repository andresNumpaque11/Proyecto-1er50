import asyncio
import logging
import os
from contextlib import asynccontextmanager
from datetime import timezone

import py_eureka_client.eureka_client as eureka_client
from fastapi import FastAPI, HTTPException, Header, Query, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
import jwt
import httpx
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import PlainTextResponse

from .schemas import OrderIn, OrderOut, DriverIn, DriverOut, AvailabilityIn, RouteOut
from ...infrastructure.config import settings
from ...infrastructure.container import Container
from ...infrastructure.workers import planning_loop, outbox_loop
from ...application.dtos import CreateDeliveryOrderCommand, RegisterDriverCommand, AvailabilityCommand
from ...domain.shared.ids import RouteId

log = logging.getLogger("delivery")

container: Container | None = None

bearer = HTTPBearer(auto_error=False)


async def authenticated_user(credentials: HTTPAuthorizationCredentials | None = Depends(bearer)):
    if credentials is None or not settings.jwt_secret:
        raise HTTPException(status_code=401, detail="Se requiere un token JWT válido")
    try:
        jwt.decode(credentials.credentials, settings.jwt_secret, algorithms=["HS256"])
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="Token inválido o expirado")
    try:
        async with httpx.AsyncClient(timeout=4.0) as client:
            response = await client.get(
                f"{settings.auth_service_url.rstrip('/')}/api/auth/me",
                headers={"Authorization": f"Bearer {credentials.credentials}"}
            )
        if response.status_code in (401, 403):
            raise HTTPException(status_code=401, detail="Sesión no válida")
        response.raise_for_status()
        return response.json()
    except HTTPException:
        raise
    except Exception:
        raise HTTPException(status_code=503, detail="No se pudo validar el usuario con users-service")


def allow_roles(*roles):
    async def dependency(user=Depends(authenticated_user)):
        if user.get("role") not in roles:
            raise HTTPException(status_code=403, detail="No tienes permisos para esta operación")
        return user
    return dependency



@asynccontextmanager
async def lifespan(app: FastAPI):
    global container
    container = Container()
    await asyncio.to_thread(container.mongo.ping)
    planning_task = asyncio.create_task(planning_loop(container))
    outbox_task = asyncio.create_task(outbox_loop(container))
    registered = False
    try:
        await eureka_client.init_async(
            eureka_server=os.getenv("EUREKA_SERVER", "http://localhost:8761/eureka/"),
            app_name="delivery-service",
            instance_host=os.getenv("EUREKA_INSTANCE_HOST", "localhost"),
            instance_port=int(os.getenv("EUREKA_INSTANCE_PORT", str(settings.port))),
        )
        registered = True
    except Exception as exc:
        log.warning("No se pudo registrar en Eureka: %s", exc)
    try:
        yield
    finally:
        if registered:
            await eureka_client.stop_async()
        planning_task.cancel()
        outbox_task.cancel()
        await asyncio.gather(planning_task, outbox_task, return_exceptions=True)
        container.mongo.close()
        container = None


app = FastAPI(title="SIGTAM - Delivery Service", version="2.0.0",
              description="Gestión de pedidos domiciliarios, rutas, conductores y entregas.",
              lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=os.getenv("CORS_ORIGINS", "http://localhost:5173,http://127.0.0.1:5173").split(","),
    allow_credentials=True, allow_methods=["*"], allow_headers=["*"],
)


def get_container():
    if container is None:
        raise HTTPException(status_code=503, detail="Servicio aún no inicializado")
    return container


@app.get("/health")
def health():
    return {"status": "ok", "service": settings.app_name}


@app.get("/ready")
def ready():
    c = get_container()
    try:
        c.mongo.ping()
        return {"status": "ready", "mongodb": "ok"}
    except Exception as exc:
        raise HTTPException(status_code=503, detail=str(exc))


@app.get("/metrics", response_class=PlainTextResponse)
def metrics():
    c = get_container()
    return (
        "# HELP delivery_orders_total Total de pedidos registrados\n"
        "# TYPE delivery_orders_total gauge\n"
        f"delivery_orders_total {len(c.orders.list(100000))}\n"
        "# HELP delivery_routes_total Total de rutas registradas\n"
        "# TYPE delivery_routes_total gauge\n"
        f"delivery_routes_total {len(c.routes.list(100000))}\n"
        "# HELP delivery_drivers_total Total de conductores registrados\n"
        "# TYPE delivery_drivers_total gauge\n"
        f"delivery_drivers_total {len(c.drivers.list(100000))}\n"
    )


@app.post("/api/v1/delivery-orders", response_model=OrderOut, status_code=201, dependencies=[Depends(allow_roles("admin", "farmaceutico", "medico"))])
def create_order(r: OrderIn, idempotency_key: str | None = Header(default=None, alias="Idempotency-Key")):
    c = get_container()
    try:
        order = c.receive.execute(CreateDeliveryOrderCommand(
            r.order_id, r.destination.street, r.destination.city,
            r.destination.postal_code, r.destination.latitude, r.destination.longitude,
            r.destination.reference, r.priority, r.external_reference
        ))
        return order_out(order)
    except Exception as exc:
        raise HTTPException(status_code=409 if "transición" in str(exc).lower() else 400, detail=str(exc))


@app.get("/api/v1/delivery-orders", response_model=list[OrderOut], dependencies=[Depends(allow_roles("admin", "farmaceutico", "medico", "conductor"))])
def list_orders(limit: int = Query(default=200, ge=1, le=1000), status: str | None = None):
    orders = get_container().orders.list(limit)
    if status:
        orders = [o for o in orders if o.status.value == status.upper()]
    return [order_out(o) for o in orders]


@app.get("/api/v1/delivery-orders/{order_id}", response_model=OrderOut, dependencies=[Depends(allow_roles("admin", "farmaceutico", "medico", "conductor"))])
def get_order(order_id: str):
    from ...domain.shared.ids import OrderId
    order = get_container().orders.get(OrderId(order_id))
    if not order:
        raise HTTPException(status_code=404, detail="Pedido no encontrado")
    return order_out(order)


def order_out(order):
    return OrderOut(
        order_id=order.id.value, status=order.status.value,
        route_id=str(order.route_id) if order.route_id else None,
        destination=order.destination.__dict__, priority=order.priority.value,
        external_reference=order.external_reference,
        created_at=order.created_at.isoformat()
    )


@app.post("/api/v1/drivers", response_model=DriverOut, status_code=201, dependencies=[Depends(allow_roles("admin"))])
def register_driver(r: DriverIn):
    try:
        d = get_container().register_driver.execute(RegisterDriverCommand(r.driver_id, r.name, r.phone))
        return driver_out(d)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc))


@app.get("/api/v1/drivers", response_model=list[DriverOut], dependencies=[Depends(allow_roles("admin", "conductor"))])
def list_drivers():
    return [driver_out(d) for d in get_container().drivers.list()]


@app.get("/api/v1/drivers/{driver_id}", response_model=DriverOut, dependencies=[Depends(allow_roles("admin", "conductor"))])
def get_driver(driver_id: str):
    from ...domain.shared.ids import DriverId
    d = get_container().drivers.get(DriverId(driver_id))
    if not d:
        raise HTTPException(status_code=404, detail="Conductor no encontrado")
    return driver_out(d)


@app.patch("/api/v1/drivers/{driver_id}/availability", response_model=DriverOut, dependencies=[Depends(allow_roles("admin", "conductor"))])
def update_driver_availability(driver_id: str, r: AvailabilityIn):
    try:
        d = get_container().availability.execute(AvailabilityCommand(driver_id, r.available))
        return driver_out(d)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc))


def driver_out(d):
    return DriverOut(d.id.value, d.name, d.status.value, d.current_route_id)


@app.get("/api/v1/routes", response_model=list[RouteOut], dependencies=[Depends(allow_roles("admin", "farmaceutico", "medico", "conductor"))])
def list_routes(status: str | None = None, driver_id: str | None = None):
    c = get_container()
    routes = c.routes.list()
    if status:
        routes = [r for r in routes if r.status.value == status.upper()]
    if driver_id:
        routes = [r for r in routes if r.driver_id and r.driver_id.value == driver_id]
    return [route_out(r, c) for r in routes]


@app.get("/api/v1/routes/{route_id}", response_model=RouteOut, dependencies=[Depends(allow_roles("admin", "farmaceutico", "medico", "conductor"))])
def get_route(route_id: str):
    c = get_container()
    route = c.routes.get(RouteId.from_string(route_id))
    if not route:
        raise HTTPException(status_code=404, detail="Ruta no encontrada")
    return route_out(route, c)


@app.post("/api/v1/routes/{route_id}/start", response_model=RouteOut, dependencies=[Depends(allow_roles("admin", "conductor"))])
def start_route(route_id: str):
    try:
        c = get_container()
        return route_out(c.start_route.execute(route_id), c)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc))


@app.post("/api/v1/routes/{route_id}/stops/{order_id}/complete", response_model=RouteOut, dependencies=[Depends(allow_roles("admin", "conductor"))])
def complete_stop(route_id: str, order_id: str):
    try:
        c = get_container()
        return route_out(c.complete_stop.execute(route_id, order_id), c)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc))


def route_out(route, c=None):
    c = c or get_container()
    stops = []
    for stop in route.stops:
        order = c.orders.get(stop.order_id)
        stops.append({
            "order_id": stop.order_id.value, "sequence": stop.sequence,
            "completed": stop.is_completed,
            "completed_at": stop.completed_at.isoformat() if stop.completed_at else None,
            "destination": order.destination.__dict__ if order else None,
            "order_status": order.status.value if order else None,
            "external_reference": order.external_reference if order else None,
        })
    return RouteOut(
        route_id=str(route.id), city=route.city, status=route.status.value,
        driver_id=route.driver_id.value if route.driver_id else None,
        stops=stops, created_at=route.created_at.isoformat()
    )
