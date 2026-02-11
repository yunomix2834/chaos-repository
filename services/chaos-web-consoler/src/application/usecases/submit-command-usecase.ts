import {Injectable} from "@nestjs/common";
import * as taskmanagerPort from "../../domain/ports/taskmanager.port";
import {ArenaCommand} from "../../domain/models/arena-command";
import {SubmitAck} from "../../domain/models/submit-result";

@Injectable()
export class SubmitCommandUsecase {
    constructor(private readonly tm: taskmanagerPort.TaskManagerPort) {
    }

    execute(cmd: ArenaCommand, meta?: Record<string, string>): Promise<SubmitAck> {
        return this.tm.submit(cmd, meta);
    }
}