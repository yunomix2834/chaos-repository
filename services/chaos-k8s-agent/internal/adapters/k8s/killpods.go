package k8s

import (
	"context"
	"fmt"
	"math"
	"math/rand"
	"time"
)

func (e *Executor) KillPodsBySelector(ctx context.Context, namespace, selector string, percent int) (int, int, error) {
	if percent <= 0 {
		return 0, 0, fmt.Errorf("percent must be > 0")
	}
	if percent > 100 {
		percent = 100
	}

	list, err := e.Client.CoreV1().Pods(namespace).List(ctx, metav1ListOpts(selector))
	if err != nil {
		return 0, 0, err
	}

	total := len(list.Items)
	if total == 0 {
		return 0, 0, fmt.Errorf("no pods found selector=%s ns=%s", selector, namespace)
	}

	toKill := int(math.Ceil(float64(total) * float64(percent) / 100.0))
	if toKill <= 0 {
		toKill = 1
	}

	// shuffle
	r := rand.New(rand.NewSource(time.Now().UnixNano()))
	idx := r.Perm(total)

	killed := 0
	for i := 0; i < toKill && i < total; i++ {
		name := list.Items[idx[i]].Name
		if e.DryRun {
			killed++
			continue
		}
		err := e.Client.CoreV1().Pods(namespace).Delete(ctx, name, metav1DeleteOpts())
		if err != nil {
			return killed, total, err
		}
		killed++
	}
	return killed, total, nil
}

// Accept selector target formats:
// - "app=cart" (namespace default)
// - "ns:default app=cart"
func ParseSelectorTarget(target string, defaultNS string) (ns, selector string) {
	// simplest MVP: treat whole target as label selector, use defaultNS
	return defaultNS, target
}
