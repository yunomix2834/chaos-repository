from __future__ import annotations

import logging
import discord
from discord import app_commands

from chaos_scripter.domain.models import ArenaTask, new_uuid
from chaos_scripter.app.dispatcher import TaskDispatcher
from chaos_scripter.app.scenario_runner import ScenarioRunner

log = logging.getLogger(__name__)

class ChaosDiscordBot(discord.Client):
    def __init__(self, dispatcher: TaskDispatcher, scenario_runner: ScenarioRunner, guild_id: int | None, channel_id: int | None = None):
        intents = discord.Intents.default()
        super().__init__(intents=intents)

        self.tree = app_commands.CommandTree(self)
        self.dispatcher = dispatcher
        self.scenario_runner = scenario_runner
        self.guild_id = guild_id
        self.channel_id = channel_id

        self._register_commands()

    def _allowed(self, interaction: discord.Interaction) -> bool:
        if self.channel_id is None:
            return True
        return interaction.channel.id == self.channel_id

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
            if not self._allowed(interaction):
                await interaction.response.send_message(
                    "Chỉ được test trong channel đã cấu hình.", ephemeral=True
                )
                return

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
                await interaction.followup.send(
                    "✅ Submitted\n"
                    f"- arena_id: `{ack.arena_id}`\n"
                    f"- task_id: `{ack.task_id}`\n"
                    f"- accepted: `{ack.accepted}`\n"
                    f"- message: `{ack.message}`"
                )
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

            if not self._allowed(interaction):
                await interaction.response.send_message(
                    "Chỉ được test trong channel đã cấu hình.", ephemeral=True
                )
                return

            await interaction.response.defer(thinking=True)
            try:
                results = self.scenario_runner.run(name=name.strip(), arena_id=arena_id)
                accepted = sum(1 for r in results if r.accepted)
                arena = results[0].arena_id if results else (arena_id or "unknown")
                await interaction.followup.send(
                    f"🏁 Scenario `{name}` submitted\n"
                    f"- arena_id: `{arena}`\n"
                    f"- tasks: `{len(results)}` accepted=`{accepted}`"
                )
            except Exception as e:
                log.exception("run_scenario failed")
                await interaction.followup.send(f"❌ run_scenario failed: `{e}`")

        @self.tree.command(name="run_scenario_file", description="Upload YAML scenario file and run it")
        @app_commands.describe(file="Upload .yaml/.yml file", arena_id="Optional arena id (UUID)")
        async def run_scenario_file(
            interaction: discord.Interaction,
            file: discord.Attachment,
            arena_id: str | None = None,
        ):
            if not self._allowed(interaction):
                await interaction.response.send_message(
                    "Chỉ được test trong channel đã cấu hình.", ephemeral=True
                )
                return

            await interaction.response.defer(thinking=True)

            fname = (file.filename or "").lower()
            if not (fname.endswith(".yaml") or fname.endswith(".yml")):
                await interaction.followup.send("❌ File phải là `.yaml` hoặc `.yml`.")
                return

            try:
                import yaml
                raw = await file.read()
                spec = yaml.safe_load(raw.decode("utf-8", errors="replace")) or {}
                steps = spec.get("steps", [])

                if not isinstance(steps, list) or not steps:
                    await interaction.followup.send("❌ YAML không có `steps` hoặc `steps` rỗng.")
                    return

                real_arena = arena_id or new_uuid()
                user = f"{interaction.user.name}#{interaction.user.discriminator}"
                results = []

                for step in steps:
                    t = ArenaTask(
                        arena_id=real_arena,
                        task_id=new_uuid(),
                        type=str(step.get("type", "")).strip(),
                        target=str(step.get("target", "")).strip(),
                        value=int(step.get("value", 0) or 0),
                        reason=str(step.get("reason", f"uploaded:{file.filename}")).strip(),
                        requested_by=user,
                    )
                    if not t.type or not t.target:
                        await interaction.followup.send(f"❌ Step invalid: `{step}`")
                        return
                    results.append(self.dispatcher.submit(t))

                accepted = sum(1 for r in results if r.accepted)
                await interaction.followup.send(
                    f"📎 Ran `{file.filename}`\n"
                    f"- arena_id: `{real_arena}`\n"
                    f"- tasks: `{len(results)}` accepted=`{accepted}`"
                )

            except Exception as e:
                log.exception("run_scenario_file failed")
                await interaction.followup.send(f"❌ run_scenario_file failed: `{e}`")

    async def setup_hook(self) -> None:
        cmds = [c.name for c in self.tree.get_commands()]
        log.info("Commands in code: %s", cmds)

        if self.guild_id:
            guild = discord.Object(id=self.guild_id)
            self.tree.copy_global_to(guild=guild)
            synced = await self.tree.sync(guild=guild)
            log.info("Synced %d commands to guild=%s: %s", len(synced), self.guild_id, [c.name for c in synced])
        else:
            synced = await self.tree.sync()
            log.info("Synced %d global commands: %s", len(synced), [c.name for c in synced])