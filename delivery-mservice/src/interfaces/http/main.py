import asyncio
import os
import py_eureka_client.eureka_client as eureka_client
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Header

from .schemas import (
    OrderIn,
    OrderOut,
    DriverIn,
    DriverOut,
    AvailabilityIn,
    RouteOut,
)
from ...infrastructure.config import settings
from ...infrastructure.container import Container
from ...infrastructure.workers import planning_loop, outbox_loop
from ...application.dtos import (
    CreateDeliveryOrderCommand,
    RegisterDriverCommand,
    AvailabilityCommand,
)

container: Container | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global container

    container = Container()

    planning_task = asyncio.create_task(planning_loop())
    outbox_task = asyncio.create_task(outbox_loop())

    registered = False
    try:
        await eureka_client.init_async(
            eureka_server=os.getenv("EUREKA_SERVER", "http://localhost:8761/eureka/"),
            app_name="delivery-service",
            instance_host=os.getenv("EUREKA_INSTANCE_HOST", "localhost"),
            instance_port=int(os.getenv("EUREKA_INSTANCE_PORT", "8002")),
        )
        registered = True
    except Exception as exc:
        print(f"No se pudo registrar en Eureka: {exc}")

    try:
        yield
    finally:
        if registered:
            await eureka_client.stop_async()

        planning_task.cancel()
        outbox_task.cancel()

        await asyncio.gather(
            planning_task,
            outbox_task,
            return_exceptions=True,
        )

        container.mongo.close()


app = FastAPI(
    title="Delivery Service",
    version="1.0.0",
    lifespan=lifespan,
)


@app.get("/health")
def health():
    return {
        "status": "ok",
        "service": settings.app_name,
    }


@app.get("/ready")
def ready():
    if container is None:
        raise HTTPException(
            status_code=503,
            detail="La aplicación aún no está inicializada",
        )

    try:
        container.mongo.ping()

        return {
            "status": "ready",
            "mongodb": "ok",
        }

    except Exception as exc:
        raise HTTPException(
            status_code=503,
            detail=str(exc),
        )


@app.post(
    "/api/v1/delivery-orders",
    response_model=OrderOut,
    status_code=201,
)
def create_order(
    r: OrderIn,
    idempotency_key: str | None = Header(
        default=None,
        alias="Idempotency-Key",
    ),
):
    if container is None:
        raise HTTPException(
            status_code=503,
            detail="La aplicación aún no está inicializada",
        )

    try:
        order = container.receive.execute(
            CreateDeliveryOrderCommand(
                r.order_id,
                r.destination.street,
                r.destination.city,
                r.destination.postal_code,
                r.destination.latitude,
                r.destination.longitude,
                r.destination.reference,
                r.priority,
                r.external_reference,
            )
        )

        return OrderOut(
            order_id=order.id.value,
            status=order.status.value,
            route_id=str(order.route_id) if order.route_id else None,
        )

    except Exception as exc:
        raise HTTPException(
            status_code=400,
            detail=str(exc),
        )


@app.post(
    "/api/v1/drivers",
    response_model=DriverOut,
    status_code=201,
)
def register_driver(r: DriverIn):
    if container is None:
        raise HTTPException(
            status_code=503,
            detail="La aplicación aún no está inicializada",
        )

    try:
        driver = container.register_driver.execute(
            RegisterDriverCommand(
                r.driver_id,
                r.name,
                r.phone,
            )
        )

        return DriverOut(
            driver_id=driver.id.value,
            name=driver.name,
            status=driver.status.value,
            current_route_id=driver.current_route_id,
        )

    except Exception as exc:
        raise HTTPException(
            status_code=400,
            detail=str(exc),
        )


@app.patch(
    "/api/v1/drivers/{driver_id}/availability",
    response_model=DriverOut,
)
def update_driver_availability(
    driver_id: str,
    r: AvailabilityIn,
):
    if container is None:
        raise HTTPException(
            status_code=503,
            detail="La aplicación aún no está inicializada",
        )

    try:
        driver = container.availability.execute(
            AvailabilityCommand(
                driver_id,
                r.available,
            )
        )

        return DriverOut(
            driver.id.value,
            driver.name,
            driver.status.value,
            driver.current_route_id,
        )

    except Exception as exc:
        raise HTTPException(
            status_code=400,
            detail=str(exc),
        )


@app.post(
    "/api/v1/routes/{route_id}/start",
    response_model=RouteOut,
)
def start_route(route_id: str):
    if container is None:
        raise HTTPException(
            status_code=503,
            detail="La aplicación aún no está inicializada",
        )

    try:
        route = container.start_route.execute(route_id)
        return route_out(route)

    except Exception as exc:
        raise HTTPException(
            status_code=400,
            detail=str(exc),
        )


@app.post(
    "/api/v1/routes/{route_id}/stops/{order_id}/complete",
    response_model=RouteOut,
)
def complete_stop(
    route_id: str,
    order_id: str,
):
    if container is None:
        raise HTTPException(
            status_code=503,
            detail="La aplicación aún no está inicializada",
        )

    try:
        route = container.complete_stop.execute(
            route_id,
            order_id,
        )

        return route_out(route)

    except Exception as exc:
        raise HTTPException(
            status_code=400,
            detail=str(exc),
        )


@app.get(
    "/api/v1/routes/{route_id}",
    response_model=RouteOut,
)
def get_route(route_id: str):
    if container is None:
        raise HTTPException(
            status_code=503,
            detail="La aplicación aún no está inicializada",
        )

    try:
        from ...domain.shared.ids import RouteId

        route = container.routes.get(RouteId.from_string(route_id))

        if not route:
            raise HTTPException(
                status_code=404,
                detail="Ruta no encontrada",
            )

        return route_out(route)

    except HTTPException:
        raise

    except Exception as exc:
        raise HTTPException(
            status_code=400,
            detail=str(exc),
        )


def route_out(route) -> RouteOut:
    return RouteOut(
        route_id=str(route.id),
        city=route.city,
        status=route.status.value,
        driver_id=(
            route.driver_id.value
            if route.driver_id
            else None
        ),
        stops=[
            {
                "order_id": stop.order_id.value,
                "sequence": stop.sequence,
                "completed": stop.is_completed,
            }
            for stop in route.stops
        ],
    )
