#!/usr/bin/env bash
set -euo pipefail

COMPOSE_STACK_NAME="${1:-chaos-task-manager-stack-compose}"
COMPOSE_NETWORK_NAME="${2:-chaos-task-manager-shared-net}"
COMPOSE_ROOT_DIR="${3:-.}"

COMPOSE_FILE_CANDIDATES=("docker-compose.yml" "docker-compose.yaml" "compose.yml" "compose.yaml")

log() { printf "\n[%s] %s\n" "$(date +'%F %T')" "$*"; }

ensure_docker_network() {
  if docker network inspect "$COMPOSE_NETWORK_NAME" >/dev/null 2>&1; then
    log "Docker network exists: $COMPOSE_NETWORK_NAME"
  else
    log "Creating docker network: $COMPOSE_NETWORK_NAME"
    docker network create "$COMPOSE_NETWORK_NAME" >/dev/null
  fi
}

find_compose_file_in_dir() {
  local dir="$1" f
  for f in "${COMPOSE_FILE_CANDIDATES[@]}"; do
    [[ -f "$dir/$f" ]] && realpath "$dir/$f" && return 0
  done

  local any
  any="$(ls -1 "$dir"/*.yml "$dir"/*.yaml 2>/dev/null | grep -Eiv 'override|overrides' | head -n 1 || true)"
  [[ -n "$any" ]] && realpath "$any" && return 0
  return 1
}

generate_override() {
  local compose_file_abs="$1"
  local workdir="$2"

  local services
  services="$(docker compose -f "$compose_file_abs" config --services 2>/dev/null || true)"
  [[ -z "$services" ]] && log "Skip (invalid compose): $compose_file_abs" && return 1

  local override
  override="$(mktemp -p "$workdir" "override-stack-XXXXXX.yml")"

  {
    echo "services:"
    while IFS= read -r svc; do
      [[ -z "$svc" ]] && continue
      cat <<EOF
  $svc:
    networks:
      - $COMPOSE_NETWORK_NAME
    labels:
      com.docker.compose.stack: "$COMPOSE_STACK_NAME"
EOF
    done <<<"$services"

    cat <<EOF

networks:
  $COMPOSE_NETWORK_NAME:
    external: true
EOF
  } >"$override"

  realpath "$override"
}

log "Stack label: $COMPOSE_STACK_NAME"
log "Shared network: $COMPOSE_NETWORK_NAME"
log "Root dir: $COMPOSE_ROOT_DIR"

ensure_docker_network

COMPOSE_ROOT_DIR_ABS="$(realpath "$COMPOSE_ROOT_DIR")"

shopt -s nullglob
for dir in "$COMPOSE_ROOT_DIR_ABS"/*/; do
  [[ -d "$dir" ]] || continue

  compose_file_abs="$(find_compose_file_in_dir "$dir" || true)"
  [[ -z "${compose_file_abs:-}" ]] && continue

  log "Found compose: $compose_file_abs"

  override_file_abs="$(generate_override "$compose_file_abs" "$dir" || true)"
  [[ -z "${override_file_abs:-}" ]] && continue

  # project per folder to avoid service name collisions
  project_name="${COMPOSE_STACK_NAME}-$(basename "${dir%/}")"

  log "Up: project=$project_name (dir=$dir)"
  (
    cd "$dir"
    docker compose -p "$project_name" -f "$compose_file_abs" -f "$override_file_abs" up -d
  )

  rm -f "$override_file_abs"
done

log "Done."
log "List containers in stack label:"
log "  docker ps --filter label=com.docker.compose.stack=$COMPOSE_STACK_NAME"
