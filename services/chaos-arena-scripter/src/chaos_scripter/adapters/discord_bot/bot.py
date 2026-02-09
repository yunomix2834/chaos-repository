from __future__ import annotations

import logging
import discord
from discord import app_commands

from chaos_scripter.domain.models import ArenaTask, new_uuid, TaskType
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
        return interaction.channel_id == self.channel_id

    async def _deny_if_not_allowed(self, interaction: discord.Interaction) -> bool:
        if self._allowed(interaction):
            return False
        await interaction.response.send_message("Only test in channel has been config.", ephemeral=True)
        return True

    def _register_commands(self) -> None:
        @self.tree.command(name="scale", description="Scale deployment to replicas (via TaskManager)")
        @app_commands.describe(
            target="deployment/<name> OR <ns>/deployment/<name>",
            replicas="replicas > 0",
            reason="audit reason",
            arena_id="optional arena uuid"
        )
        async def scale(interaction: discord.Interaction, target: str, replicas: int, reason: str = "discord", arena_id: str | None = None):
            if await self._deny_if_not_allowed(interaction):
                return
            await interaction.response.defer(thinking=True)

            task = ArenaTask(
                arena_id=arena_id or new_uuid(),
                task_id=new_uuid(),
                type=TaskType.SCALE.value,
                target=target.strip(),
                value=int(replicas),
                reason=reason.strip(),
                requested_by=interaction.user.name,
            )
            try:
                ack = self.dispatcher.submit(task)
                await interaction.followup.send(f"✅ SCALE submitted: accepted={ack.accepted} msg=`{ack.message}`\n- arena=`{ack.arena_id}` task=`{ack.task_id}`")
            except Exception as e:
                log.exception("scale failed")
                await interaction.followup.send(f"❌ scale failed: `{e}`")

        @self.tree.command(name="kill_pods", description="Kill percent of pods by label selector (via TaskManager)")
        @app_commands.describe(
            selector="label selector, e.g. app=cart",
            percent="1..100",
            reason="audit reason",
            arena_id="optional arena uuid"
        )
        async def kill_pods(interaction: discord.Interaction, selector: str, percent: int, reason: str = "discord", arena_id: str | None = None):
            if await self._deny_if_not_allowed(interaction):
                return
            await interaction.response.defer(thinking=True)

            task = ArenaTask(
                arena_id=arena_id or new_uuid(),
                task_id=new_uuid(),
                type=TaskType.KILL_PODS.value,
                target=selector.strip(),
                value=int(percent),
                reason=reason.strip(),
                requested_by=interaction.user.name,
            )
            try:
                ack = self.dispatcher.submit(task)
                await interaction.followup.send(f"✅ KILL_PODS submitted: accepted={ack.accepted} msg=`{ack.message}`\n- arena=`{ack.arena_id}` task=`{ack.task_id}`")
            except Exception as e:
                log.exception("kill_pods failed")
                await interaction.followup.send(f"❌ kill_pods failed: `{e}`")

        @self.tree.command(name="rollback_scale", description="Rollback SCALE to previous replicas (via TaskManager)")
        @app_commands.describe(
            namespace="namespace, e.g. default",
            deployment="deployment name, e.g. cart",
            previous_replicas="replicas to rollback to",
            reason="audit reason",
            arena_id="optional arena uuid"
        )
        async def rollback_scale(interaction: discord.Interaction, namespace: str, deployment: str, previous_replicas: int, reason: str = "discord", arena_id: str | None = None):
            if await self._deny_if_not_allowed(interaction):
                return
            await interaction.response.defer(thinking=True)

            rb_target = rollback_scale_target(namespace.strip(), deployment.strip(), int(previous_replicas))

            task = ArenaTask(
                arena_id=arena_id or new_uuid(),
                task_id=new_uuid(),
                type=TaskType.ROLLBACK.value,
                target=rb_target,
                value=0,
                reason=reason.strip(),
                requested_by=interaction.user.name,
            )
            try:
                ack = self.dispatcher.submit(task)
                await interaction.followup.send(f"✅ ROLLBACK(SCALE) submitted: accepted={ack.accepted} msg=`{ack.message}`\n- arena=`{ack.arena_id}` task=`{ack.task_id}`\n- target=`{rb_target}`")
            except Exception as e:
                log.exception("rollback_scale failed")
                await interaction.followup.send(f"❌ rollback_scale failed: `{e}`")


        @self.tree.command(name="run_scenario", description="Run a YAML scenario by file name in scenarios/")
        async def run_scenario(interaction: discord.Interaction, name: str, arena_id: str | None = None):
            if await self._deny_if_not_allowed(interaction):
                return
            await interaction.response.defer(thinking=True)
            try:
                results = self.scenario_runner.run(name=name.strip(), arena_id=arena_id)
                accepted = sum(1 for r in results if r.accepted)
                arena = results[0].arena_id if results else (arena_id or "unknown")
                await interaction.followup.send(f"🏁 Scenario `{name}` submitted arena=`{arena}` tasks=`{len(results)}` accepted=`{accepted}`")
            except Exception as e:
                log.exception("run_scenario failed")
                await interaction.followup.send(f"❌ run_scenario failed: `{e}`")

    async def setup_hook(self) -> None:
        if self.guild_id:
            guild = discord.Object(id=self.guild_id)
            self.tree.copy_global_to(guild=guild)
            synced = await self.tree.sync(guild=guild)
            log.info("Synced %d commands to guild=%s: %s", len(synced), self.guild_id, [c.name for c in synced])
        else:
            synced = await self.tree.sync()
            log.info("Synced %d global commands: %s", len(synced), [c.name for c in synced])