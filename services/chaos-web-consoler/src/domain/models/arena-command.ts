export type TaskType = "SCALE" | "KILL_PODS" | "ROLLBACK";

export interface ArenaCommand {
    arenaId: string;
    taskId: string;
    type: TaskType;
    target: string;
    value: number;
    reason?: string;
    requestedBy?: string;
}

