import { ArenaEvent } from "../models/arena-event";

export interface ArenaEventsPort {
    events$(): import("rxjs").Observable<ArenaEvent>;

    start(): Promise<void>;
    stop(): Promise<void>;
}