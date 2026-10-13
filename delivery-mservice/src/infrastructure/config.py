from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "delivery-service"
    app_env: str = "development"
    host: str = "0.0.0.0"
    port: int = 8000
    mongo_uri: str = "mongodb://localhost:27017"
    mongo_database: str = "delivery_service"
    route_max_stops: int = 10
    route_max_wait_seconds: int = 600
    route_planner_interval_seconds: int = 10
    outbox_poll_seconds: int = 2
    outbox_batch_size: int = 50
    event_publisher_mode: str = "log"
    event_publisher_url: str | None = None
    event_publisher_timeout_seconds: int = 5
    model_config = SettingsConfigDict(env_file=".env", case_sensitive=False)


settings = Settings()
