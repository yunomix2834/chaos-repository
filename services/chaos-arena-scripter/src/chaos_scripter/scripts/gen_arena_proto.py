from __future__ import annotations

import subprocess
import sys
from pathlib import Path


def main() -> int:

    # chaos-repository/services/chaos-arena-scripter/src/chaos_scripter/scripts/gen_arena_proto.py
    here = Path(__file__).resolve()

    # service_dir = chaos-repository/services/chaos-arena-scripter
    service_dir = here.parents[3]

    # repo_root = chaos-repository
    repo_root = here.parents[5]

    proto_dir = repo_root / "proto"
    proto_file = proto_dir / "arena.proto"

    out_dir = service_dir / "src" / "chaos_scripter" / "generated"
    out_dir.mkdir(parents=True, exist_ok=True)

    if not proto_file.exists():
        print(f"[ERR] proto not found: {proto_file}", file=sys.stderr)
        return 2

    cmd = [
        sys.executable,
        "-m",
        "grpc_tools.protoc",
        f"-I{proto_dir}",
        f"--python_out={out_dir}",
        f"--grpc_python_out={out_dir}",
        str(proto_file),
    ]

    print("[CMD]", " ".join(cmd))
    return subprocess.run(cmd, cwd=str(service_dir)).returncode


if __name__ == "__main__":
    raise SystemExit(main())
