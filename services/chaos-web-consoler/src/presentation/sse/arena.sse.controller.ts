import { Controller, Inject, MessageEvent, Param, Sse } from "@nestjs/common";
import { Observable, filter, map } from "rxjs";
import * as arenaEventsPort from "../../domain/ports/arena-events.port";

@Controller("/api/sse")
export class ArenaSseController {
    constructor(@Inject("ArenaEventsPort") private readonly events: arenaEventsPort.ArenaEventsPort) {}

    @Sse("/arena/:arenaId")
    streamArena(@Param("arenaId") arenaId: string): Observable<MessageEvent> {
        return this.events.events$().pipe(
            filter((e) => e.arenaId === arenaId),
            map((e) => ({ data: e } as MessageEvent)),
        );
    }

    @Sse("/task/:taskId")
    streamTask(@Param("taskId") taskId: string): Observable<MessageEvent> {
        return this.events.events$().pipe(
            filter((e) => e.taskId === taskId),
            map((e) => ({ data: e } as MessageEvent)),
        );
    }
}
