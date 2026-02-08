from __future__ import annotations

from pydantic import BaseModel, Field
from dotenv import load_dotenv
import os

load_dotenv()

class Settings(BaseModel):
    # Discord
    discord_token: str = Field(default_factory=lambda: os.getenv("DISCORD_TOKEN", ""))
    discord_guild_id: int | None = Field(default_factory=lambda: int(os.getenv("DISCORD_GUILD_ID", "0")) or None)

    # gRPC TaskManager
    grpc_target: str = Field(default_factory=lambda: os.getenv("TASKMANAGER_GRPC_TARGET", "localhost:9999"))
    grpc_timeout_sec: float = Field(default_factory=lambda: float(os.getenv("GRPC_TIMEOUT_SEC", "5")))
    lang: str = Field(default_factory=lambda: os.getenv("X_LANG", "en"))
    tenant_id: str | None = Field(default_factory=lambda: os.getenv("X_TENANT_ID") or None)

    # Scenario
    scenario_dir: str = Field(default_factory=lambda: os.getenv("SCENARIO_DIR", "scenarios"))

settings = Settings()