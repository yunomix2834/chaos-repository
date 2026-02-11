import {Injectable} from "@nestjs/common";
import * as taskmanagerPort from "../../domain/ports/taskmanager.port";
import {CorrelationService} from "../services/correlation.service";
import {SubmitAndWaitResult} from "../../domain/models/submit-result";
import {ArenaCommand} from "../../domain/models/arena-command";

@Injectable()
export class SubmitAndWaitUsecase {
    constructor(
        private readonly tm: taskmanagerPort.TaskManagerPort,
        private readonly corr: CorrelationService
    ) {}

    async execute(
        cmd: ArenaCommand,
        meta: Record<string, string>,
        waitTimeoutMs: number
    ): Promise<SubmitAndWaitResult> {
        const ack = await this.tm.submit(cmd, meta);
        if (!ack.accepted) return { ack };

        const finalEvt = await this.corr.waitForFinal(cmd.taskId, waitTimeoutMs);
        return {ack, finalEvent: finalEvt};
    }
}