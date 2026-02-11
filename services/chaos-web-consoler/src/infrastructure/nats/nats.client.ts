import {connect, JetStreamClient, JetStreamManager, NatsConnection} from "nats";

export type NatsCtx = {
    nc: NatsConnection;
    js: JetStreamClient;
    jsm: JetStreamManager
}

export async function connectNats(url:string, user?: string, pass?: string): Promise<NatsCtx> {
    const nc = await connect({
        servers: url,
        user,
        pass,
        maxReconnectAttempts: -1,
        reconnectTimeWait: 2000,
    });

    const js = nc.jetstream();
    const jsm = await nc.jetstreamManager();
    return {nc, js, jsm};
}