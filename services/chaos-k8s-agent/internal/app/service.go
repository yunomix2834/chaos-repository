package app

import (
	"chaos-k8s-agent/internal/domain"
	"context"
	"time"
)

type Service struct {
	consumer  domain.CommandConsumer
	publisher domain.EventPublisher
	router    *Router
}

func NewService(consumer domain.CommandConsumer, publisher domain.EventPublisher, router *Router) *Service {
	return &Service{consumer: consumer, publisher: publisher, router: router}
}

func (s *Service) Run(ctx context.Context) error {
	return s.consumer.Run(ctx, func(ctx context.Context, env domain.CommandEnvelope) error {
		cmd := env.Command

		_ = s.publisher.PublishEvent(ctx, domain.ArenaEvent{
			ArenaID: cmd.ArenaID, TaskID: cmd.TaskID, Type: cmd.Type,
			Target: cmd.Target, Value: cmd.Value,
			Status:    domain.TaskRunning,
			Message:   "accepted by k8s-agent",
			Timestamp: time.Now(),
			TraceID:   env.Meta.TraceID, RequestID: env.Meta.RequestID,
			TenantID: env.Meta.TenantID, Lang: env.Meta.Lang,
			Source: "k8s-agent",
		}, nil)

		msg, err := s.router.Dispatch(ctx, cmd)
		if err != nil {
			_ = s.publisher.PublishEvent(ctx, domain.ArenaEvent{
				ArenaID: cmd.ArenaID, TaskID: cmd.TaskID, Type: cmd.Type,
				Target: cmd.Target, Value: cmd.Value,
				Status:    domain.TaskFailed,
				Message:   err.Error(),
				Timestamp: time.Now(),
				TraceID:   env.Meta.TraceID, RequestID: env.Meta.RequestID,
				TenantID: env.Meta.TenantID, Lang: env.Meta.Lang,
				Source: "k8s-agent",
			}, nil)
			_ = env.Nak()
			return err
		}

		_ = s.publisher.PublishEvent(ctx, domain.ArenaEvent{
			ArenaID: cmd.ArenaID, TaskID: cmd.TaskID, Type: cmd.Type,
			Target: cmd.Target, Value: cmd.Value,
			Status:    domain.TaskSucceeded,
			Message:   msg,
			Timestamp: time.Now(),
			TraceID:   env.Meta.TraceID, RequestID: env.Meta.RequestID,
			TenantID: env.Meta.TenantID, Lang: env.Meta.Lang,
			Source: "k8s-agent",
		}, nil)

		return env.Ack()
	})
}
