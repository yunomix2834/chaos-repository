package domain

import (
	"fmt"
	"strconv"
	"strings"
)

type RollbackSpec struct {
	Action    string
	Namespace string
	Name      string
	Value     int
}

func ParseRollbackTarget(target string) (RollbackSpec, error) {
	t := strings.TrimSpace(target)
	if t == "" {
		return RollbackSpec{}, fmt.Errorf("rollback target is blank")
	}

	parts := strings.Split(t, "|")
	if len(parts) != 4 {
		return RollbackSpec{}, fmt.Errorf("invalid rollback target: %q (expected ACTION|ns|name|value)", target)
	}

	action := strings.ToUpper(strings.TrimSpace(parts[0]))
	ns := strings.TrimSpace(parts[1])
	name := strings.TrimSpace(parts[2])
	vStr := strings.TrimSpace(parts[3])

	if action == "" || ns == "" || name == "" || vStr == "" {
		return RollbackSpec{}, fmt.Errorf("invalid rollback target: %q (empty field)", target)
	}

	v, err := strconv.Atoi(vStr)
	if err != nil {
		return RollbackSpec{}, fmt.Errorf("invalid rollback value: %q", vStr)
	}

	return RollbackSpec{
		Action:    action,
		Namespace: ns,
		Name:      name,
		Value:     v,
	}, nil
}
