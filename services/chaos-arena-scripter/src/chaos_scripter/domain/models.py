from __future__ import annotations

import time
import uuid
from enum import Enum

from pydantic import BaseModel, Field


def new_uuid() -> str:
    return str(uuid.uuid4())


def now_ms() -> int:
    return int(time.time() * 1000)


class TaskType(str, Enum):
    KILL_PODS = "KILL_PODS"
    SCALE = "SCALE"
    ROLLBACK = "ROLLBACK"


def rollback_scale_target(ns: str, deploy: str, previous_replicas: int) -> str:
    return f"SCALE|{ns}|{deploy}|{int(previous_replicas)}"


class ArenaTask(BaseModel):
    arena_id: str = Field(default_factory=new_uuid)
    task_id: str = Field(default_factory=new_uuid)

    type: str
    target: str
    value: int = 0
    reason: str = ""

    requested_by: str | None = None
    created_at_ms: int = Field(default_factory=now_ms)


class SubmitResult(BaseModel):
    arena_id: str
    task_id: str
    accepted: bool
    message: str
