package k8s

import (
	"fmt"
	"os"
	"path/filepath"

	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/clientcmd"
)

func NewClient() (*kubernetes.Clientset, error) {
	// in-cluster
	cfg, err := rest.InClusterConfig()
	if err == nil {
		return kubernetes.NewForConfig(cfg)
	}

	// fallback: local kubeconfig
	home, _ := os.UserHomeDir()
	kubeconfig := filepath.Join(home, ".kube", "config")
	cfg, err = clientcmd.BuildConfigFromFlags("", kubeconfig)
	if err != nil {
		return nil, fmt.Errorf("load kubeconfig failed: %w", err)
	}
	return kubernetes.NewForConfig(cfg)
}
