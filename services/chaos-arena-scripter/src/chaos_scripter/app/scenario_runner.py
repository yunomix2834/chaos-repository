from __future__ import annotations

import os
import yaml

from chaos_scripter.domain.models import ArenaTask, SubmitResult, new_uuid
from chaos_scripter.app.dispatcher import TaskDispatcher

class ScenarioRunner:
    def __init__(self, dispatcher: TaskDispatcher, scenario_dir: str) -> None:
        self.dispatcher = dispatcher
        self.scenario_dir = scenario_dir

    def run(self, name: str, arena_id: str | None = None) -> list[SubmitResult]:
        arena_id = arena_id or new_uuid()
        path = os.path.join(self.scenario_dir, f"{name}.yaml")
        with open(path, "r", encoding="utf-8") as f:
            spec = yaml.safe_load(f) or {}

        steps = spec.get("steps", [])
        if not isinstance(steps, list) or not steps:
            raise ValueError(f"Scenario {name} has no steps")

        results: list[SubmitResult] = []
        for step in steps:
            t = ArenaTask(
                arena_id=arena_id,
                task_id=new_uuid(),
                type=str(step.get("type", "")).strip(),
                target=str(step.get("target", "")).strip(),
                value=int(step.get("value", 0) or 0),
                reason=str(step.get("reason", f"scenario={name}")).strip(),
            )
            if not t.type or not t.target:
                raise ValueError(f"Invalid step in scenario '{name}': {step}")

            results.append(self.dispatcher.submit(t))

        return results