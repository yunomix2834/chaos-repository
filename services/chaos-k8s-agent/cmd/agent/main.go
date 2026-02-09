package main

import (
	"chaos-k8s-agent/internal/adapters/k8s"
	"chaos-k8s-agent/internal/adapters/natsjs"
	"chaos-k8s-agent/internal/app"
	"chaos-k8s-agent/internal/config"
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"
)

func main() {
	cfg := config.Load()

	ctx, cancel := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer cancel()

	nc, err := natsjs.Connect(cfg.NatsURL, cfg.NatsUser, cfg.NatsPass)
	if err != nil {
		log.Fatalf("nats connect failed: %v", err)
	}
	defer func() {
		_ = nc.Conn.Drain()
		nc.Conn.Close()
	}()

	// K8s client
	kc, err := k8s.NewClient()
	if err != nil {
		log.Fatalf("k8s client failed: %v", err)
	}

	exec := &k8s.Executor{Client: kc, DryRun: cfg.DryRun}

	// Handlers
	reg := app.NewRegistry(
		&k8s.ScaleHandler{Exec: exec, DefaultNS: cfg.NamespaceDefault},
		&k8s.KillPodsHandler{Exec: exec, DefaultNS: cfg.NamespaceDefault},
	)
	router := app.NewRouter(reg)

	consumer := &natsjs.PullConsumer{
		JS:      nc.JS,
		Stream:  cfg.Stream,
		Subject: cfg.CmdSubject,
		Durable: cfg.Durable + "-cmd",
		Batch:   cfg.PullBatch,
		Timeout: cfg.PullTimeout,
	}

	publisher := &natsjs.Publisher{
		JS:      nc.JS,
		Subject: cfg.EvtSubject,
	}

	svc := app.NewService(consumer, publisher, router)

	log.Printf("chaos-k8s-agent started cmd=%s evt=%s durable=%s ns_default=%s dry_run=%v",
		cfg.CmdSubject, cfg.EvtSubject, cfg.Durable, cfg.NamespaceDefault, cfg.DryRun)

	// Run in background
	errCh := make(chan error, 1)
	go func() { errCh <- svc.Run(ctx) }()

	select {
	case <-ctx.Done():
		log.Println("shutdown requested")
		time.Sleep(200 * time.Millisecond)
	case err := <-errCh:
		log.Printf("service stopped: %v", err)
	}
}
