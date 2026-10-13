from contextlib import contextmanager

from pymongo.errors import (
    ConnectionFailure,
    OperationFailure,
)

from .mongo import MongoConnection


class MongoUnitOfWork:
    def __init__(
        self,
        mongo: MongoConnection,
        max_retries: int = 3,
    ):
        self.mongo = mongo
        self.max_retries = max_retries
        self.session = None

    @contextmanager
    def transaction(self):
        last_error = None

        for attempt in range(self.max_retries):
            try:
                with self.mongo.client.start_session() as session:
                    self.session = session

                    with session.start_transaction():
                        yield session

                    return

            except (
                ConnectionFailure,
                OperationFailure,
            ) as exc:
                last_error = exc

                is_transient = exc.has_error_label("TransientTransactionError")

                if is_transient and attempt < self.max_retries - 1:
                    continue

                raise

            finally:
                self.session = None

        if last_error is not None:
            raise last_error
