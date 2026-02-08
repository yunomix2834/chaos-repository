from __future__ import annotations

import logging
from chaos_scripter.domain.models import ArenaTask, SubmitResult
from chaos_scripter.adapters.grpc_taskmanager.client import TaskManagerGrpcClient, GrpcMetadata

log = logging.getLogger(__name__)

class TaskDispatcher:
    def __init__(self, grpc_client: TaskManagerGrpcClient, base_md: GrpcMetadata) -> None:
        self.grpc_client = grpc_client
        self.base_md = base_md

    def submit(self, task: ArenaTask) -> SubmitResult:
        md = GrpcMetadata(
            lang=self.base_md.lang,
            tenant_id=self.base_md.tenant_id,
            trace_id=self.base_md.trace_id or task.task_id,
            request_id=self.base_md.request_id or task.task_id,
        )
        return self.grpc_client.submit_task(task, md)