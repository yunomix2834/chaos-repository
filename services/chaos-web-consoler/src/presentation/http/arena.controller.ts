import { Body, Controller, Inject, Post, Headers } from "@nestjs/common";
import { SubmitCommandDto, SubmitAndWaitDto } from "./dto";
import { SubmitCommandUsecase } from "../../application/usecases/submit-command-usecase";
import { SubmitAndWaitUsecase } from "../../application/usecases/submit-and-wait.usecase";
import { ConfigService } from "../../config/config.service";

@Controller("/api/arena")
export class ArenaController {
    constructor(
        private readonly submitUc: SubmitCommandUsecase,
        private readonly submitWaitUc: SubmitAndWaitUsecase,
        private readonly cfg: ConfigService,
    ) {}

    private buildMeta(h: Record<string, string | undefined>) {
        // gRPC metadata keys MUST be lowercase in your Java interceptor
        return {
            "x-lang": h["x-lang"] || this.cfg.xLang,
            "x-tenant-id": h["x-tenant-id"] || this.cfg.xTenantId || "",
            "x-trace-id": h["x-trace-id"] || "",
            "x-request-id": h["x-request-id"] || "",
        };
    }

    @Post("/submit")
    async submit(@Body() dto: SubmitCommandDto, @Headers() headers: any) {
        const meta = this.buildMeta(headers);
        // if caller doesn't send trace/request, use taskId
        meta["x-trace-id"] = meta["x-trace-id"] || dto.taskId;
        meta["x-request-id"] = meta["x-request-id"] || dto.taskId;

        const ack = await this.submitUc.execute(dto, meta);
        return { ack };
    }

    @Post("/submit-and-wait")
    async submitAndWait(@Body() dto: SubmitAndWaitDto, @Headers() headers: any) {
        const meta = this.buildMeta(headers);
        meta["x-trace-id"] = meta["x-trace-id"] || dto.taskId;
        meta["x-request-id"] = meta["x-request-id"] || dto.taskId;

        const res = await this.submitWaitUc.execute(dto, meta, dto.waitTimeoutMs);
        return res;
    }
}
