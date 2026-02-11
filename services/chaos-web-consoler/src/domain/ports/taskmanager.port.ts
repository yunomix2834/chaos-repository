import {ArenaCommand} from "../models/arena-command";
import {SubmitAck} from "../models/submit-result";

export interface TaskManagerPort {
    submit(cmd: ArenaCommand, meta?: Record<string, string>): Promise<SubmitAck>;
}