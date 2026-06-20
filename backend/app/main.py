from fastapi import FastAPI

from app.api.health import router as health_router
from app.api.sync import router as sync_router
from app.core.settings import Settings


def create_app(settings: Settings | None = None) -> FastAPI:
    app_settings = settings or Settings()
    app = FastAPI(
        title=app_settings.service_name,
        version=app_settings.version,
        docs_url="/docs" if app_settings.enable_docs else None,
        redoc_url="/redoc" if app_settings.enable_docs else None,
    )
    app.include_router(health_router)
    app.include_router(sync_router)
    return app


app = create_app()
