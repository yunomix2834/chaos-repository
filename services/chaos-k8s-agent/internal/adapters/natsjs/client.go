package natsjs

import (
	"github.com/nats-io/nats.go"
	"time"
)

type Client struct {
	Conn *nats.Conn
	JS   nats.JetStreamContext
}

func Connect(url, user, pass string) (*Client, error) {
	opts := []nats.Option{
		nats.Timeout(5 * time.Second),
		nats.MaxReconnects(-1),
		nats.ReconnectWait(5 * time.Second),
		nats.PingInterval(30 * time.Second),
		nats.MaxPingsOutstanding(5),
	}
	if user != "" && pass != "" {
		opts = append(opts, nats.UserInfo(user, pass))
	}
	nc, err := nats.Connect(url, opts...)
	if err != nil {
		return nil, err
	}
	js, err := nc.JetStream()
	if err != nil {
		_ = nc.Drain()
		nc.Close()
		return nil, err
	}
	return &Client{Conn: nc, JS: js}, nil
}
