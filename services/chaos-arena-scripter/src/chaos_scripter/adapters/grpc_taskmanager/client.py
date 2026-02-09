from __future__ import annotations

import logging
from dataclasses import dataclass
import grpc

from chaos_scripter.generated.arena_pb2 import TaskCommand # type: ignore
from chaos_scripter.generated.arena_pb2_grpc import TaskManagerServiceStub # type: ignore

from chaos_scripter.domain.models import ArenaTask, SubmitResult

log = logging.getLogger(__name__)

@dataclass
class GrpcMetadata:
    lang: str = "en"
    tenant_id: str | None = None
    trace_id: str | None = None
    request_id: str | None = None

    def to_tuples(self) -> list[tuple[str, str]]:
        # gRPC metadata keys phải lowercase
        md: list[tuple[str, str]] = []
        if self.lang:
            md.append(("x-lang", self.lang))
        if self.tenant_id:
            md.append(("x-tenant-id", self.tenant_id))
        if self.trace_id:
            md.append(("x-trace-id", self.trace_id))
        if self.request_id:
            md.append(("x-request-id", self.request_id))
        return md

class TaskManagerGrpcClient:
    def __init__(self, target: str, timeout_sec: float = 5.0) -> None:
        self._target = target
        self._timeout = timeout_sec
        self._channel = grpc.insecure_channel(target)
        self._stub = TaskManagerServiceStub(self._channel)

    def close(self) -> None:
        try:
            self._channel.close()
        except Exception:
            pass

    def submit_task(self, task: ArenaTask, md: GrpcMetadata) -> SubmitResult:
        req = TaskCommand(
            arena_id=task.arena_id,
            task_id=task.task_id,
            type=task.type,
            target=task.target,
            value=int(task.value),
            reason=task.reason or "",
        )

        log.info("[GRPC][SUBMIT] target=%s arena=%s task=%s type=%s target=%s value=%s",
                 self._target, task.arena_id, task.task_id, task.type, task.target, task.value)

        ack = self._stub.Submit(req, timeout=self._timeout, metadata=md.to_tuples())
        return SubmitResult(
            arena_id=ack.arena_id,
            task_id=ack.task_id,
            accepted=bool(ack.accepted),
            message=str(ack.message),
        )