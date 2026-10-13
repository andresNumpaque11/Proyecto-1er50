from pymongo import MongoClient, ASCENDING
from pymongo.database import Database


class MongoConnection:
    def __init__(self, uri: str, database: str):
        self.client = MongoClient(
            uri,
            serverSelectionTimeoutMS=5000,
            retryWrites=True,
            retryReads=True,
        )

        self.db: Database = self.client[database]

    def ping(self) -> bool:
        self.client.admin.command("ping")
        return True

    def close(self) -> None:
        self.client.close()

    def ensure_indexes(self) -> None:
        self.db.routes.create_index(
            [
                ("status", ASCENDING),
                ("city", ASCENDING),
                ("created_at", ASCENDING),
            ]
        )

        self.db.routes.create_index(
            [
                ("status", ASCENDING),
                ("created_at", ASCENDING),
            ]
        )

        self.db.drivers.create_index([("status", ASCENDING)])

        self.db.outbox_events.create_index(
            [
                ("status", ASCENDING),
                ("created_at", ASCENDING),
            ]
        )
