from __future__ import annotations
import os, sys
import uuid
import logging

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
GEN = os.path.join(ROOT, "generated")
if GEN not in sys.path:
    sys.path.insert(0, GEN)

from chaos_scripter.logger import setup_logging
from chaos_scripter.config import settings
from chaos_scripter.domain.models import ArenaTask
from chaos_scripter.adapters.grpc_taskmanager.client import TaskManagerGrpcClient, GrpcMetadata
from chaos_scripter.app.dispatcher import TaskDispatcher

def main():
    setup_logging()
    log = logging.getLogger("cli_submit")

    arena_id = str(uuid.uuid4())
    task_id  = str(uuid.uuid4())

    task = ArenaTask(
        arena_id=arena_id,
        task_id=task_id,
        type="KILL_PODS",
        target="app=cart",
        value=30,
        reason="grpc smoke test",
        requested_by="cli",
    )

    client = TaskManagerGrpcClient(settings.grpc_target, settings.grpc_timeout_sec)
    try:
        dispatcher = TaskDispatcher(client, GrpcMetadata(lang=settings.lang, tenant_id=settings.tenant_id))
        ack = dispatcher.submit(task)
        log.info("ACK: arena_id=%s task_id=%s accepted=%s message=%s",
                 ack.arena_id, ack.task_id, ack.accepted, ack.message)
        print(ack.model_dump())
    finally:
        client.close()

if __name__ == "__main__":
    main()
