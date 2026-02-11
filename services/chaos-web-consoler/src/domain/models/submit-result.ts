export interface SubmitAck {
    arenaId: string;
    taskId: string;
    accepted: boolean;
    message: string;
}

export interface SubmitAndWaitResult {
    ack: SubmitAck;
    finalEvent?: import("./arena-event").ArenaEvent
}