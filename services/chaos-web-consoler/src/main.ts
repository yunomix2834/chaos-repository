import { NestFactory } from "@nestjs/core";
import { AppModule } from "./app.module";
import { ConfigService } from "./config/config.service";

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  const cfg = app.get(ConfigService);

  await app.listen(cfg.httpPort);
  // eslint-disable-next-line no-console
  console.log(`[web-console] listening on http://localhost:${cfg.httpPort}`);
}
bootstrap();
