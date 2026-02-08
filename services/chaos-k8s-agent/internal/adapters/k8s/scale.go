package k8s

import (
	"context"
	"fmt"
	"strings"

	"k8s.io/client-go/kubernetes"
)

type Executor struct {
	Client *kubernetes.Clientset
	DryRun bool
}

func (e *Executor) ScaleDeployment(ctx context.Context, namespace, name string, replicas int) error {
	if e.DryRun {
		return nil
	}
	scale, err := e.Client.AppsV1().Deployments(namespace).GetScale(ctx, name, metav1GetOpts())
	if err != nil {
		return err
	}
	scale.Spec.Replicas = int32(replicas)
	_, err = e.Client.AppsV1().Deployments(namespace).UpdateScale(ctx, name, scale, metav1UpdateOpts())
	return err
}

// helper: parse "deployment/cart" or "ns/deployment/cart"
func ParseDeploymentTarget(target string, defaultNS string) (ns, name string, err error) {
	// accepted:
	// - "deployment/cart"
	// - "ns/deployment/cart"
	parts := strings.Split(strings.TrimSpace(target), "/")
	if len(parts) == 2 && parts[0] == "deployment" {
		return defaultNS, parts[1], nil
	}
	if len(parts) == 3 && parts[1] == "deployment" {
		return parts[0], parts[2], nil
	}
	return "", "", fmt.Errorf("invalid target format for SCALE: %s (expected deployment/<name> or <ns>/deployment/<name>)", target)
}
