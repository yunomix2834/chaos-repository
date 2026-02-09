package k8s

import (
	"chaos-k8s-agent/internal/domain"
	"context"
	"fmt"
	"strings"
)

type ScaleHandler struct {
	Exec      domain.K8sExecutor
	DefaultNS string
}

func (h *ScaleHandler) Type() string { return "SCALE" }

func (h *ScaleHandler) Handle(ctx context.Context, cmd domain.ArenaCommand) (string, error) {
	ns, name, err := ParseDeploymentTarget(cmd.Target, h.DefaultNS)
	if err != nil {
		return "", err
	}
	if cmd.Value <= 0 {
		return "", fmt.Errorf("replicas must be > 0")
	}
	if err := h.Exec.ScaleDeployment(ctx, ns, name, cmd.Value); err != nil {
		return "", err
	}
	return fmt.Sprintf("scaled deployment %s/%s to %d replicas", ns, name, cmd.Value), nil
}

type KillPodsHandler struct {
	Exec      domain.K8sExecutor
	DefaultNS string
}

func (h *KillPodsHandler) Type() string { return "KILL_PODS" }

func (h *KillPodsHandler) Handle(ctx context.Context, cmd domain.ArenaCommand) (string, error) {
	ns, selector := ParseSelectorTarget(cmd.Target, h.DefaultNS)
	selector = strings.TrimSpace(selector)

	killed, total, err := h.Exec.KillPodsBySelector(ctx, ns, selector, cmd.Value)
	if err != nil {
		return "", err
	}
	return fmt.Sprintf("killed %d/%d pods selector=%q ns=%s", killed, total, selector, ns), nil
}

type RollbackHandler struct {
	Exec      domain.K8sExecutor
	DefaultNS string
}

func (h *RollbackHandler) Type() string { return "ROLLBACK" }

func (h *RollbackHandler) Handle(ctx context.Context, cmd domain.ArenaCommand) (string, error) {
	// Expected target format from scripter: "SCALE|default|cart|3"
	spec, err := domain.ParseRollbackTarget(cmd.Target)
	if err != nil {
		return "", err
	}

	switch spec.Action {
	case "SCALE":
		if spec.Value <= 0 {
			return "", fmt.Errorf("ROLLBACK SCALE replicas must be > 0")
		}

		// Build a normalized scale target and reuse existing parser expectations:
		// accept <ns>/deployment/<name>
		scaleTarget := fmt.Sprintf("%s/deployment/%s", spec.Namespace, spec.Name)
		ns, name, err := ParseDeploymentTarget(scaleTarget, h.DefaultNS)
		if err != nil {
			return "", err
		}

		if err := h.Exec.ScaleDeployment(ctx, ns, name, spec.Value); err != nil {
			return "", err
		}
		return fmt.Sprintf("rollback SCALE => scaled deployment %s/%s to %d replicas", ns, name, spec.Value), nil

	default:
		return "", fmt.Errorf("ROLLBACK currently supports only SCALE, got=%s", spec.Action)
	}
}
