from __future__ import annotations

import logging
import discord
from discord import app_commands

from chaos_scripter.domain.models import ArenaTask, new_uuid, TaskType, rollback_scale_target
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

        @self.tree.command(name="run_scenario_file", description="Upload YAML scenario file and run it (submit tasks)")
        @app_commands.describe(
            file="Upload .yaml/.yml with key `steps:`",
            arena_id="optional arena uuid"
        )
        async def run_scenario_file(
            interaction: discord.Interaction,
            file: discord.Attachment,
            arena_id: str | None = None,
        ):
            if await self._deny_if_not_allowed(interaction):
                return

            await interaction.response.defer(thinking=True)

            await interaction.response.defer(thinking=True)

            fname = (file.filename or "").lower().strip()
            if not (fname.endswith(".yaml") or fname.endswith(".yml")):
                await interaction.followup.send("❌ File phải là `.yaml` hoặc `.yml`.")
                return

            max_bytes = 512 * 1024
            if file.size and file.size > max_bytes:
                await interaction.followup.send(f"❌ File quá lớn ({file.size} bytes). Giới hạn {max_bytes} bytes.")
                return

            try:
                import yaml
            except Exception as e:
                await interaction.followup.send(f"❌ Missing dependency PyYAML: `{e}`")
                return

            try:
                raw = await file.read()
                text = raw.decode("utf-8", errors="replace")
                spec = yaml.safe_load(text) or {}
            except Exception as e:
                log.exception("yaml parse failed")
                await interaction.followup.send(f"❌ Parse YAML failed: `{e}`")
                return

            steps = spec.get("steps", None)
            if not isinstance(steps, list) or not steps:
                await interaction.followup.send("❌ YAML không hợp lệ: cần `steps:` là list và không rỗng.")
                return

            real_arena = (arena_id or new_uuid()).strip()
            requested_by = f"{interaction.user.name}#{getattr(interaction.user, 'discriminator', '0')}"

            results = []
            try:
                for i, step in enumerate(steps, start=1):
                    if not isinstance(step, dict):
                        await interaction.followup.send(f"❌ Step #{i} không phải object: `{step}`")
                        return

                    typ = str(step.get("type", "")).strip().upper()
                    target = str(step.get("target", "")).strip()
                    value = int(step.get("value", 0) or 0)
                    reason = str(step.get("reason", f"uploaded:{file.filename}")).strip()

                    if not typ or not target:
                        await interaction.followup.send(f"❌ Step #{i} thiếu `type` hoặc `target`: `{step}`")
                        return

                    # Optional: validate type set
                    allowed = {t.value for t in TaskType}
                    if typ not in allowed:
                        await interaction.followup.send(
                            f"❌ Step #{i} type `{typ}` không hợp lệ. Allowed: {sorted(list(allowed))}"
                        )
                        return

                    task = ArenaTask(
                        arena_id=real_arena,
                        task_id=new_uuid(),
                        type=typ,
                        target=target,
                        value=value,
                        reason=reason,
                        requested_by=requested_by,
                    )

                    ack = self.dispatcher.submit(task)
                    results.append(ack)

            except Exception as e:
                log.exception("run_scenario_file submit failed")
                await interaction.followup.send(f"❌ Submit failed: `{e}`")
                return

            accepted = sum(1 for r in results if r.accepted)
            lines = "\n".join(
                [f"- `{r.task_id}` accepted={r.accepted} msg=`{r.message}`" for r in results[:10]]
            )
            if len(results) > 10:
                lines += f"\n...and {len(results) - 10} more"

            await interaction.followup.send(
                f"📎 Ran scenario file `{file.filename}`\n"
                f"- arena_id: `{real_arena}`\n"
                f"- tasks: `{len(results)}` accepted=`{accepted}`\n"
                f"{lines}"
            )

    async def setup_hook(self) -> None:
        if self.guild_id:
            guild = discord.Object(id=self.guild_id)
            self.tree.copy_global_to(guild=guild)
            synced = await self.tree.sync(guild=guild)
            log.info("Synced %d commands to guild=%s: %s", len(synced), self.guild_id, [c.name for c in synced])
        else:
            synced = await self.tree.sync()
            log.info("Synced %d global commands: %s", len(synced), [c.name for c in synced])