package app

import (
	"chaos-k8s-agent/internal/domain"
	"context"
	"fmt"
	"log"
	"strings"
)

type HandlerRegistry struct {
	handlers map[string]domain.IncidentHandler
}

func NewRegistry(list ...domain.IncidentHandler) *HandlerRegistry {
	m := make(map[string]domain.IncidentHandler)
	for _, h := range list {
		m[strings.ToUpper(strings.TrimSpace(h.Type()))] = h
	}
	return &HandlerRegistry{handlers: m}
}

func (r *HandlerRegistry) Resolve(typ string) (domain.IncidentHandler, bool) {
	h, ok := r.handlers[strings.ToUpper(strings.TrimSpace(typ))]
	return h, ok
}

type Router struct {
	reg *HandlerRegistry
}

func NewRouter(reg *HandlerRegistry) *Router {
	return &Router{reg: reg}
}

func (rt *Router) Dispatch(ctx context.Context, cmd domain.ArenaCommand) (string, error) {
	typ := strings.ToUpper(strings.TrimSpace(cmd.Type))
	log.Printf("[DISPATCH] type=%s target=%s value=%d", typ, cmd.Target, cmd.Value)

	h, ok := rt.reg.Resolve(typ)
	if !ok {
		return "", fmt.Errorf("no handler for type=%s", cmd.Type)
	}
	return h.Handle(ctx, cmd)
}
