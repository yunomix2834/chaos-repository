import { Injectable } from "@nestjs/common";
import * as dotenv from "dotenv";
dotenv.config();

@Injectable()
export class ConfigService {
    httpPort = Number(process.env.HTTP_PORT ?? "3000");

    taskmanagerTarget = process.env.TASKMANAGER_GRPC_TARGET ?? "localhost:9999";
    grpcTimeoutMs = Number(process.env.GRPC_TIMEOUT_MS ?? "5000");
    protoPath = process.env.PROTO_PATH ?? "proto/arena.proto";

    natsUrl = process.env.NATS_URL ?? "nats://localhost:4222";
    natsUser = process.env.NATS_USER ?? "";
    natsPass = process.env.NATS_PASS ?? "";
    natsStream = process.env.NATS_STREAM ?? "TASKS";
    natsEvtSubject = process.env.NATS_EVT_SUBJECT ?? "task.events.*";
    natsEvtDurable = process.env.NATS_EVT_DURABLE ?? "web-console-evt";

    xLang = process.env.X_LANG ?? "en";
    xTenantId = process.env.X_TENANT_ID ?? "";
}
