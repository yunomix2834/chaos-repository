package natsjs

import (
	"chaos-k8s-agent/internal/domain"
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/nats-io/nats.go"
)

type Publisher struct {
	JS      nats.JetStreamContext
	Subject string
}

func (p *Publisher) PublishEvent(ctx context.Context, evt domain.ArenaEvent, headers map[string]string) error {
	b, err := json.Marshal(evt)
	if err != nil {
		return err
	}

	h := nats.Header{}
	for k, v := range headers {
		h.Set(k, v)
	}
	// Align with Java headers style (optional)
	if evt.RequestID != "" {
		h.Set("X-Request-Id", evt.RequestID)
	}
	if evt.TraceID != "" {
		h.Set("X-Trace-Id", evt.TraceID)
	}
	h.Set("X-Message-Type", "ARENA_EVENT")
	if evt.Lang != "" {
		h.Set("X-Lang", evt.Lang)
	}

	msg := &nats.Msg{
		Subject: p.Subject,
		Header:  h,
		Data:    b,
	}

	_, err = p.JS.PublishMsg(msg, nats.Context(ctx))
	if err != nil {
		return fmt.Errorf("publish event failed: %w", err)
	}
	_ = time.Now() // keep for future metrics hook
	return nil
}
