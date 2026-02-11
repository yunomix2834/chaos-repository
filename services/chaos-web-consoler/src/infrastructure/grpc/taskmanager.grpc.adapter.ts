import { Injectable } from "@nestjs/common";
import { TaskManagerPort } from "../../domain/ports/taskmanager.port";
import { ArenaCommand } from "../../domain/models/arena-command";
import { SubmitAck } from "../../domain/models/submit-result";
import { Metadata, credentials } from "@grpc/grpc-js";
import { loadArenaProto } from "./proto-loader";

type GrpcClient = {
    Submit: (
        req: any,
        meta: Metadata,
        cb: (err: any, resp: any) => void
    ) => void;
};

@Injectable()
export class TaskManagerGrpcAdapter implements TaskManagerPort {
    private client: GrpcClient;

    constructor(
        // inject config values in app.module.ts
        private readonly target: string,
        private readonly timeoutMs: number,
        private readonly protoPath: string,
    ) {
        const pkg = loadArenaProto(this.protoPath);
        // package chaos.v1; service TaskManagerService
        const svcCtor = pkg.chaos?.v1?.TaskManagerService;
        if (!svcCtor) throw new Error("TaskManagerService not found in proto");

        this.client = new svcCtor(this.target, credentials.createInsecure());
    }

    submit(cmd: ArenaCommand, meta?: Record<string, string>): Promise<SubmitAck> {
        const md = new Metadata();
        for (const [k, v] of Object.entries(meta ?? {})) {
            if (v !== undefined && v !== null && String(v).length > 0) md.add(k.toLowerCase(), String(v));
        }

        const req = {
            arena_id: cmd.arenaId,
            task_id: cmd.taskId,
            type: cmd.type,
            target: cmd.target,
            value: cmd.value ?? 0,
            reason: cmd.reason ?? "",
        };

        return new Promise((resolve, reject) => {
            const deadline = Date.now() + this.timeoutMs;

            // @grpc/grpc-js supports deadline via options, but easiest is wrap with setTimeout
            const timer = setTimeout(() => reject(new Error("gRPC timeout")), this.timeoutMs + 50);

            this.client.Submit(req, md, (err, resp) => {
                clearTimeout(timer);
                if (err) return reject(err);

                resolve({
                    arenaId: resp.arena_id ?? resp.arenaId,
                    taskId: resp.task_id ?? resp.taskId,
                    accepted: Boolean(resp.accepted),
                    message: String(resp.message ?? ""),
                });
            });
        });
    }
}
