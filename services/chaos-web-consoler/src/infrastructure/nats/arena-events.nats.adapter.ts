import {Injectable, Logger, OnModuleDestroy, OnModuleInit} from "@nestjs/common";
import {ArenaEventsPort} from "../../domain/ports/arena-events.port";
import {consumerOpts, createInbox, StringCodec} from "nats";
import {connectNats, NatsCtx} from "./nats.client";
import {ArenaEvent} from "../../domain/models/arena-event";
import {Subject} from "rxjs";

@Injectable()
export class ArenaEventsNatsAdapter implements ArenaEventsPort, OnModuleInit, OnModuleDestroy {
    private readonly log = new Logger(ArenaEventsNatsAdapter.name);
    private readonly sc = StringCodec();

    private ctx?: NatsCtx;
    private readonly bus = new Subject<ArenaEvent>();
    private running = false;

    constructor(
        private readonly url: string,
        private readonly user: string,
        private readonly pass: string,
        private readonly stream: string,
        private readonly subject: string,  // task.events.*
        private readonly durable: string,
    ) {}

    events$(): import("rxjs").Observable<ArenaEvent> {
        return this.bus.asObservable();
    }

    async onModuleInit() {
        await this.start();
    }

    async onModuleDestroy() {
        await this.stop();
        this.bus.complete();
    }

    async start(): Promise<void> {
        if (this.running) return;
        this.running = true;

        this.ctx = await connectNats(this.url, this.user, this.pass);
        this.log.log(`[NATS] connected url=${this.url}`);

        // durable push consumer
        const opts = consumerOpts();
        opts.durable(this.durable);
        opts.manualAck();
        opts.ackExplicit();
        opts.deliverTo(createInbox());
        opts.bindStream(this.stream);
        // filter subject: task.events.*
        opts.filterSubject(this.subject);

        const sub = await this.ctx.js.subscribe(this.subject, opts);

        (async () => {
            for await (const m of sub) {
                try {
                    // Go agent publish raw JSON ArenaEvent
                    const text = this.sc.decode(m.data);
                    const evt = JSON.parse(text) as ArenaEvent;
                    this.bus.next(evt);
                    m.ack();
                } catch (e: any) {
                    this.log.warn(`parse/handle event failed: ${e?.message ?? e}`);
                    m.nak();
                }
            }
        })().catch((e) => this.log.error(`sub loop crashed: ${e?.message ?? e}`));
    }

    async stop(): Promise<void> {
        if (!this.running) return;
        this.running = false;

        try {
            await this.ctx?.nc.drain();
            this.ctx?.nc.close();
        } catch {}
    }
}