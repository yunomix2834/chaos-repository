from __future__ import annotations

import logging
import discord
from discord import app_commands

from chaos_scripter.domain.models import ArenaTask, new_uuid
from chaos_scripter.app.dispatcher import TaskDispatcher
from chaos_scripter.app.scenario_runner import ScenarioRunner

log = logging.getLogger(__name__)

class ChaosDiscordBot(discord.Client):
    def __init__(self, dispatcher: TaskDispatcher, scenario_runner: ScenarioRunner, guild_id: int | None):
        intents = discord.Intents.default()
        super().__init__(intents=intents)

        self.tree = app_commands.CommandTree(self)
        self.dispatcher = dispatcher
        self.scenario_runner = scenario_runner
        self.guild_id = guild_id

        self._register_commands()

    def _register_commands(self) -> None:
        @self.tree.command(name="spawn", description="Spawn a chaos task to TaskManager via gRPC")
        @app_commands.describe(
            type="Task type e.g. KILL_PODS, SCALE",
            target='Target e.g. "app=cart" or "deployment/cart"',
            value="Integer value e.g. 30 or replicas",
            reason="Reason for audit",
            arena_id="Optional arena id (UUID). If empty, auto generate",
        )
        async def spawn(
                interaction: discord.Interaction,
                type: str,
                target: str,
                value: int = 0,
                reason: str = "discord",
                arena_id: str | None = None,
        ):
            await interaction.response.defer(thinking=True)
            user = f"{interaction.user.name}#{interaction.user.discriminator}"
            task = ArenaTask(
                arena_id=arena_id or new_uuid(),
                task_id=new_uuid(),
                type=type.strip(),
                target=target.strip(),
                value=int(value),
                reason=reason.strip(),
                requested_by=user,
            )

            try:
                ack = self.dispatcher.submit(task)
                msg = (
                    f"Submitted\n"
                    f"- arena_id: `{ack.arena_id}`\n"
                    f"- task_id: `{ack.task_id}`\n"
                    f"- accepted: `{ack.accepted}`\n"
                    f"- message: `{ack.message}`"
                )
                await interaction.followup.send(msg)
            except Exception as e:
                log.exception("spawn failed")
                await interaction.followup.send(f"❌ submit failed: `{e}`")

        @self.tree.command(name="run_scenario", description="Run a YAML scenario (submit multiple tasks)")
        @app_commands.describe(
            name="Scenario file name without .yaml",
            arena_id="Optional arena id (UUID). If empty, auto generate",
        )
        async def run_scenario(
                interaction: discord.Interaction,
                name: str,
                arena_id: str | None = None,
        ):
            await interaction.response.defer(thinking=True)
            try:
                results = self.scenario_runner.run(name=name.strip(), arena_id=arena_id)
                accepted = sum(1 for r in results if r.accepted)
                arena = results[0].arena_id if results else (arena_id or "unknown")
                lines = "\n".join(
                    [f"- `{r.task_id}` accepted={r.accepted} msg={r.message}" for r in results[:10]]
                )
                if len(results) > 10:
                    lines += f"\n...and {len(results) - 10} more"
                await interaction.followup.send(
                    f"🏁 Scenario `{name}` submitted\n"
                    f"- arena_id: `{arena}`\n"
                    f"- tasks: `{len(results)}` accepted=`{accepted}`\n"
                    f"{lines}"
                )
            except Exception as e:
                log.exception("run_scenario failed")
                await interaction.followup.send(f"❌ run_scenario failed: `{e}`")

        async def setup_hook(self) -> None:
            if self.guild_id:
                guild = discord.Object(id=self.guild_id)
                self.tree.copy_global_to(guild=guild)
                await self.tree.sync(guild=guild)
                log.info("Discord commands synced to guild=%s", self.guild_id)
            else:
                await self.tree.sync()
                log.info("Discord commands synced globally")