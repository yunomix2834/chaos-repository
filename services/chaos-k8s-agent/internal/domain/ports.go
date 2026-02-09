package domain

import (
	"context"
)

type MsgMeta struct {
	RequestID   string
	TraceID     string
	TenantID    string
	Lang        string
	Subject     string
	MessageType string
	CreatedAt   string
}

type CommandEnvelope struct {
	Meta    MsgMeta
	Command ArenaCommand
	Ack     func() error
	Nak     func() error
}

type CommandConsumer interface {
	Run(ctx context.Context, handler func(context.Context, CommandEnvelope) error) error
}

type EventPublisher interface {
	PublishEvent(ctx context.Context, evt ArenaEvent, headers map[string]string) error
}

type K8sExecutor interface {
	ScaleDeployment(ctx context.Context, namespace, name string, replicas int) error
	KillPodsBySelector(ctx context.Context, namespace, selector string, percent int) (killed int, total int, err error)
}

type IncidentHandler interface {
	Type() string
	Handle(ctx context.Context, cmd ArenaCommand) (string, error) // returns message
}
