import asyncio
import logging
from .config import settings

log = logging.getLogger(__name__)


async def planning_loop(container):
    while True:
        try:
            container.process_routes.execute()
        except Exception:
            log.exception("Error en planning worker")
        await asyncio.sleep(settings.route_planner_interval_seconds)


async def outbox_loop(container):
    while True:
        try:
            for doc in container.outbox.pending(settings.outbox_batch_size):
                try:
                    from ..domain.events.events import DomainEvent
                    event = DomainEvent(
                        doc["event_name"], doc["occurred_at"],
                        doc["aggregate_id"], doc["payload"]
                    )
                    container.publisher.publish(event)
                    container.outbox.published(doc["_id"])
                except Exception as exc:
                    log.exception("Error publicando evento")
                    container.outbox.failed(doc["_id"], str(exc))
        except Exception:
            log.exception("Error en outbox worker")
        await asyncio.sleep(settings.outbox_poll_seconds)
