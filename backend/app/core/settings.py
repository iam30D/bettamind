from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    service_name: str = "Bettamind Optional Backend"
    version: str = "0.1.0"
    enable_docs: bool = True
    database_url: str = "postgresql+psycopg://bettamind:bettamind@localhost:5432/bettamind"

    model_config = SettingsConfigDict(env_prefix="BETTAMIND_", env_file=".env")
