package domain

type NatsJetStreamRequestInfo struct {
	Subject     string            `json:"subject"`
	MessageType string            `json:"messageType"`
	RequestID   string            `json:"requestId"`
	TraceID     string            `json:"traceId"`
	TenantID    string            `json:"tenantId"`
	Lang        string            `json:"lang"`
	CreatedAt   string            `json:"createdAt"`
	Body        string            `json:"body"`
	Headers     map[string]string `json:"headers"`
}

type ArenaCommand struct {
	ArenaID string `json:"arenaId"`
	TaskID  string `json:"taskId"`
	Type    string `json:"type"`
	Target  string `json:"target"`
	Value   int    `json:"value"`
	Reason  string `json:"reason"`
}
