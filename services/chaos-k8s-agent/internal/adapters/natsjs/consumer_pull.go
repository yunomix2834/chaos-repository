package natsjs

import (
	"chaos-k8s-agent/internal/domain"
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/nats-io/nats.go"
)

type PullConsumer struct {
	JS      nats.JetStreamContext
	Stream  string
	Subject string
	Durable string
	Batch   int
	Timeout time.Duration
}

func (c *PullConsumer) Run(ctx context.Context, handler func(context.Context, domain.CommandEnvelope) error) error {
	sub, err := c.JS.PullSubscribe(
		c.Subject,
		c.Durable,
		nats.BindStream(c.Stream),
	)
	if err != nil {
		return fmt.Errorf("pull subscribe failed: %w", err)
	}

	for {
		select {
		case <-ctx.Done():
			return ctx.Err()
		default:
			// pull then fetch
			if err := sub.Pull(c.Batch); err != nil {
				time.Sleep(200 * time.Millisecond)
				continue
			}

			msgs, err := sub.Fetch(c.Batch, nats.MaxWait(c.Timeout))
			if err != nil && err != nats.ErrTimeout {
				time.Sleep(300 * time.Millisecond)
				continue
			}

			for _, m := range msgs {
				env, perr := parseMessage(m)
				if perr != nil {
					_ = m.Nak()
					continue
				}
				_ = handler(ctx, env) // ack/nak handled inside
			}
		}
	}
}

func parseMessage(m *nats.Msg) (domain.CommandEnvelope, error) {
	var req domain.NatsJetStreamRequestInfo
	if err := json.Unmarshal(m.Data, &req); err != nil {
		var cmd domain.ArenaCommand
		if err2 := json.Unmarshal(m.Data, &cmd); err2 != nil {
			return domain.CommandEnvelope{}, fmt.Errorf("unmarshal requestInfo failed: %v / %v", err, err2)
		}
		return domain.CommandEnvelope{
			Meta: domain.MsgMeta{
				RequestID:   headerFirst(m, "X-Request-Id"),
				TraceID:     headerFirst(m, "X-Trace-Id"),
				TenantID:    headerFirst(m, "X-Tenant-Id"),
				Lang:        headerFirst(m, "X-Lang"),
				Subject:     m.Subject,
				MessageType: headerFirst(m, "X-Message-Type"),
				CreatedAt:   time.Now(),
			},
			Command: cmd,
			Ack:     func() error { return m.Ack() },
			Nak:     func() error { return m.Nak() },
		}, nil
	}

	var cmd domain.ArenaCommand
	if err := json.Unmarshal([]byte(req.Body), &cmd); err != nil {
		return domain.CommandEnvelope{}, fmt.Errorf("unmarshal arena command failed: %w", err)
	}

	meta := domain.MsgMeta{
		RequestID:   req.RequestID,
		TraceID:     req.TraceID,
		TenantID:    req.TenantID,
		Lang:        req.Lang,
		Subject:     req.Subject,
		MessageType: req.MessageType,
		CreatedAt:   req.CreatedAt,
	}
	// header fallback if fields empty
	if meta.RequestID == "" {
		meta.RequestID = headerFirst(m, "X-Request-Id")
	}
	if meta.TraceID == "" {
		meta.TraceID = headerFirst(m, "X-Trace-Id")
	}
	if meta.Lang == "" {
		meta.Lang = headerFirst(m, "X-Lang")
	}

	return domain.CommandEnvelope{
		Meta:    meta,
		Command: cmd,
		Ack:     func() error { return m.Ack() },
		Nak:     func() error { return m.Nak() },
	}, nil
}

func headerFirst(m *nats.Msg, key string) string {
	if m.Header == nil {
		return ""
	}
	return m.Header.Get(key)
}
