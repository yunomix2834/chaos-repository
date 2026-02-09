from __future__ import annotations

import logging

from chaos_scripter.logger import setup_logging
from chaos_scripter.config import settings
from chaos_scripter.adapters.grpc_taskmanager.client import TaskManagerGrpcClient, GrpcMetadata
from chaos_scripter.app.dispatcher import TaskDispatcher
from chaos_scripter.app.scenario_runner import ScenarioRunner
from chaos_scripter.adapters.discord_bot.bot import ChaosDiscordBot

log = logging.getLogger(__name__)

def main() -> None:
    setup_logging()

    if not settings.discord_token:
        raise RuntimeError("DISCORD_TOKEN is required")

    grpc_client = TaskManagerGrpcClient(
        target=settings.grpc_target,
        timeout_sec=settings.grpc_timeout_sec,
    )

    dispatcher = TaskDispatcher(grpc_client, GrpcMetadata(lang=settings.lang, tenant_id=settings.tenant_id))
    runner = ScenarioRunner(dispatcher, settings.scenario_dir)

    bot = ChaosDiscordBot(
        dispatcher=dispatcher,
        scenario_runner=runner,
        guild_id=settings.discord_guild_id,
        channel_id=settings.discord_channel_id,
    )

    try:
        log.info("Starting Discord bot; grpc_target=%s scenario_dir=%s", settings.grpc_target, settings.scenario_dir)
        bot.run(settings.discord_token)
    finally:
        grpc_client.close()

if __name__ == "__main__":
    main()