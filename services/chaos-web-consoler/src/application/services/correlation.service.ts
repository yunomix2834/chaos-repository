import {ArenaEvent} from "../../domain/models/arena-event";
import {Injectable} from "@nestjs/common";

type Waiter = {
    resolve: (evt: ArenaEvent) => void;
    reject: (err: Error) => void;
    timeout: NodeJS.Timeout;
}

@Injectable()
export class CorrelationService {
    private waiters = new Map<string, Waiter>();

    waitForFinal(taskId: string, timeoutMs: number): Promise<ArenaEvent> {
        return new Promise((resolve, reject) => {
            const timeout = setTimeout(async () => {
                this.waiters.delete(taskId);
                reject(new Error(`timeout waiting final event taskId=${taskId}`));
            }, timeoutMs);

            this.waiters.set(taskId, { resolve, reject, timeout });
        })
    }

    /** Called by event subscriber when new event arrives */
    notify(evt: ArenaEvent): void {
        if (evt.status !== "SUCCEEDED" && evt.status !== "FAILED") return;

        const w = this.waiters.get(evt.taskId);
        if (!w) return;

        clearTimeout(w.timeout);
        this.waiters.delete(evt.taskId);
        w.resolve(evt);
    }

    shutdown(): void {
        for (const [taskid, w] of this.waiters.entries()) {
            clearTimeout(w.timeout);
            w.reject(new Error(`shutdown before completion taskid=${taskid}`));
            this.waiters.delete(taskid);
        }
    }
}