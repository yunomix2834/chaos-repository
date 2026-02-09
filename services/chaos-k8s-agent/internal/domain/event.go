package domain

type TaskStatus string

const (
	TaskRunning   TaskStatus = "RUNNING"
	TaskSucceeded TaskStatus = "SUCCEEDED"
	TaskFailed    TaskStatus = "FAILED"
)

type ArenaEvent struct {
	ArenaID   string     `json:"arenaId"`
	TaskID    string     `json:"taskId"`
	Type      string     `json:"type"`
	Target    string     `json:"target"`
	Value     int        `json:"value"`
	Status    TaskStatus `json:"status"`
	Message   string     `json:"message"`
	Timestamp string     `json:"ts"`

	TraceID   string `json:"traceId,omitempty"`
	RequestID string `json:"requestId,omitempty"`
	TenantID  string `json:"tenantId,omitempty"`
	Lang      string `json:"lang,omitempty"`
	Source    string `json:"source,omitempty"`
}
