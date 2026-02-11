export type TaskStatus = "RUNNING" | "SUCCEEDED" | "FAILED";

export interface ArenaEvent {
    arenaId: string;
    taskId: string;
    type: string;
    target: string;
    value: number;

    status: TaskStatus;
    message: string;
    ts: string;

    traceId?: string;
    requestId?: string;
    tenantId?: string;
    lang?: string;
    source?: string;
}