from __future__ import annotations

import argparse
import logging

from chaos_scripter.adapters.grpc_taskmanager.client import (
    GrpcMetadata,
    TaskManagerGrpcClient,
)
from chaos_scripter.app.dispatcher import TaskDispatcher
from chaos_scripter.config import settings
from chaos_scripter.domain.models import ArenaTask, TaskType, new_uuid
from chaos_scripter.logger import setup_logging


def main():
    setup_logging()
    log = logging.getLogger("cli_submit")

    p = argparse.ArgumentParser()
    p.add_argument("--type", required=True, choices=[t.value for t in TaskType])
    p.add_argument("--target", default="")
    p.add_argument("--value", type=int, default=0)
    p.add_argument("--reason", default="cli")
    p.add_argument("--arena-id", default="")
    args = p.parse_args()

    arena_id = args.arena_id or new_uuid()

    task = ArenaTask(
        arena_id=arena_id,
        task_id=new_uuid(),
        type=args.type,
        target=args.target.strip(),
        value=int(args.value),
        reason=args.reason.strip(),
        requested_by="cli",
    )

    client = TaskManagerGrpcClient(settings.grpc_target, settings.grpc_timeout_sec)
    try:
        dispatcher = TaskDispatcher(
            client, GrpcMetadata(lang=settings.lang, tenant_id=settings.tenant_id)
        )
        ack = dispatcher.submit(task)
        log.info(
            "ACK accepted=%s msg=%s arena=%s task=%s",
            ack.accepted,
            ack.message,
            ack.arena_id,
            ack.task_id,
        )
        print(ack.model_dump())
    finally:
        client.close()


if __name__ == "__main__":
    main()
