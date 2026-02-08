package config

import (
	"os"
	"strconv"
	"time"
)

type Config struct {
	// NATS
	NatsURL     string
	NatsUser    string
	NatsPass    string
	Stream      string
	CmdSubject  string
	EvtSubject  string
	Durable     string
	PullBatch   int
	PullTimeout time.Duration

	// K8s
	NamespaceDefault string
	DryRun           bool

	// App
	LogLevel string
}

func Load() Config {
	return Config{
		NatsURL:     getEnv("NATS_URL", "nats://localhost:4222"),
		NatsUser:    getEnv("NATS_USER", ""),
		NatsPass:    getEnv("NATS_PASS", ""),
		Stream:      getEnv("NATS_STREAM", "TASKS"),
		CmdSubject:  getEnv("NATS_CMD_SUBJECT", "task.cmd.arena"),
		EvtSubject:  getEnv("NATS_EVT_SUBJECT", "task.events.arena"),
		Durable:     getEnv("NATS_DURABLE", "chaos-k8s-agent"),
		PullBatch:   getEnvInt("NATS_PULL_BATCH", 10),
		PullTimeout: time.Duration(getEnvInt("NATS_PULL_TIMEOUT_MS", 1000)) * time.Millisecond,

		NamespaceDefault: getEnv("K8S_NAMESPACE_DEFAULT", "default"),
		DryRun:           getEnvBool("DRY_RUN", false),

		LogLevel: getEnv("LOG_LEVEL", "INFO"),
	}
}

func getEnv(k, d string) string {
	if v := os.Getenv(k); v != "" {
		return v
	}

	return d
}

func getEnvInt(k string, d int) int {
	if v := os.Getenv(k); v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			return n
		}
	}

	return d
}

func getEnvBool(k string, d bool) bool {
	if v := os.Getenv(k); v != "" {
		if b, err := strconv.ParseBool(v); err == nil {
			return b
		}
	}
	return d
}
