import { Module, OnModuleInit, OnModuleDestroy } from "@nestjs/common";
import { ConfigModule } from "./config/config.module";
import { ConfigService } from "./config/config.service";

import { CorrelationService } from "./application/services/correlation.service";
import { SubmitCommandUsecase } from "./application/usecases/submit-command-usecase";
import { SubmitAndWaitUsecase } from "./application/usecases/submit-and-wait.usecase";

import { ArenaController } from "./presentation/http/arena.controller";
import { ArenaSseController } from "./presentation/sse/arena.sse.controller";

import { TaskManagerGrpcAdapter } from "./infrastructure/grpc/taskmanager.grpc.adapter";
import { ArenaEventsNatsAdapter } from "./infrastructure/nats/arena-events.nats.adapter";
import { TaskManagerPort } from "./domain/ports/taskmanager.port";
import { ArenaEventsPort } from "./domain/ports/arena-events.port";

@Module({
  imports: [ConfigModule],
  controllers: [ArenaController, ArenaSseController],
  providers: [
    CorrelationService,
    SubmitCommandUsecase,
    SubmitAndWaitUsecase,

    // Providers for ports
    {
      provide: "TaskManagerPort",
      inject: [ConfigService],
      useFactory: (cfg: ConfigService) =>
          new TaskManagerGrpcAdapter(
              cfg.taskmanagerTarget,
              cfg.grpcTimeoutMs,
              cfg.protoPath,
          ),
    },
    {
      provide: "ArenaEventsPort",
      inject: [ConfigService],
      useFactory: (cfg: ConfigService) =>
          new ArenaEventsNatsAdapter(
              cfg.natsUrl,
              cfg.natsUser,
              cfg.natsPass,
              cfg.natsStream,
              cfg.natsEvtSubject,
              cfg.natsEvtDurable,
          ),
    },
  ],
})
export class AppModule implements OnModuleInit, OnModuleDestroy {
  constructor(
      private readonly corr: CorrelationService,
      // inject by token
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      private readonly cfg: ConfigService,
  ) {}

  // Nest token injection helper
  private get events(): ArenaEventsPort {
    // @ts-ignore
    return (global as any).__eventsPort;
  }

  async onModuleInit() {
    // Workaround gọn: lấy provider trực tiếp qua global không đẹp.
    // Cách "đúng Nest": inject by @Inject("ArenaEventsPort") ở constructor.
    // Mình sẽ đưa bản đúng ở dưới trong controllers/usecases.
  }

  async onModuleDestroy() {
    this.corr.shutdown();
  }
}
