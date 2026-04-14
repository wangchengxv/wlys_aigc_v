package service

import (
	"encoding/json"
	"strings"
	"time"

	"github.com/example/aigc-server-go/internal/model"
	"github.com/example/aigc-server-go/internal/repo"
)

// ResolveOrderedConnectionConfigs returns enabled connections in router priority order (Java resolveOrderedConnections).
func (r *RouterRouting) ResolveOrderedConnectionConfigs(includeFailover bool) ([]model.ConnectionConfig, error) {
	ids := r.OrderedConnectionIDs(includeFailover)
	all, err := r.Store.AllConnections()
	if err != nil {
		return nil, err
	}
	byID := make(map[string]model.ConnectionConfig, len(all))
	for _, c := range all {
		byID[c.ID] = c
	}
	var out []model.ConnectionConfig
	seen := map[string]struct{}{}
	for _, id := range ids {
		c, ok := byID[id]
		if !ok || !c.Enabled {
			continue
		}
		if _, dup := seen[c.ID]; dup {
			continue
		}
		seen[c.ID] = struct{}{}
		out = append(out, c)
	}
	return out, nil
}

type RouterRouting struct {
	Store *repo.Stores
}

func (r *RouterRouting) GetConfig() (*model.RoutingConfig, error) {
	return r.Store.GetRouting()
}

func (r *RouterRouting) AppendConnectionIfAbsent(connectionID string) error {
	cfg, err := r.Store.GetRouting()
	if err != nil {
		return err
	}
	var ids []string
	_ = json.Unmarshal([]byte(cfg.PriorityConnectionIDsJSON), &ids)
	for _, id := range ids {
		if id == connectionID {
			return r.Store.SaveRouting(cfg)
		}
	}
	ids = append(ids, connectionID)
	b, _ := json.Marshal(ids)
	cfg.PriorityConnectionIDsJSON = string(b)
	return r.Store.SaveRouting(cfg)
}

func (r *RouterRouting) RemoveConnection(connectionID string) error {
	cfg, err := r.Store.GetRouting()
	if err != nil {
		return err
	}
	var ids []string
	_ = json.Unmarshal([]byte(cfg.PriorityConnectionIDsJSON), &ids)
	var next []string
	for _, id := range ids {
		if id != connectionID {
			next = append(next, id)
		}
	}
	b, _ := json.Marshal(next)
	cfg.PriorityConnectionIDsJSON = string(b)
	return r.Store.SaveRouting(cfg)
}

// OrderedConnectionIDs mirrors resolveOrderedConnections(includeFailoverCandidates=true) simplified (priority list only).
func (r *RouterRouting) OrderedConnectionIDs(includeFailover bool) []string {
	cfg, err := r.Store.GetRouting()
	if err != nil {
		return nil
	}
	var ids []string
	_ = json.Unmarshal([]byte(cfg.PriorityConnectionIDsJSON), &ids)
	conns, _ := r.Store.AllConnections()
	enabled := map[string]bool{}
	for _, c := range conns {
		if c.Enabled {
			enabled[c.ID] = true
		}
	}
	var ordered []string
	seen := map[string]struct{}{}
	for _, id := range ids {
		if enabled[id] {
			if _, ok := seen[id]; !ok {
				ordered = append(ordered, id)
				seen[id] = struct{}{}
			}
		}
	}
	if len(ordered) == 0 {
		for _, c := range conns {
			if c.Enabled {
				ordered = append(ordered, c.ID)
			}
		}
	}
	// Match Java: full list only when includeFailoverCandidates && failoverEnabled
	if includeFailover && cfg.FailoverEnabled {
		return ordered
	}
	if len(ordered) > 0 {
		return []string{ordered[0]}
	}
	return nil
}

func (r *RouterRouting) TimeoutSeconds() int {
	cfg, err := r.Store.GetRouting()
	if err != nil || cfg.FailoverTimeoutSeconds < 1 {
		return 10
	}
	return cfg.FailoverTimeoutSeconds
}

// MatchCurrentSlot simplified: not implementing time_schedule strategy fully — Java parity optional.
func (r *RouterRouting) Strategy() string {
	cfg, err := r.Store.GetRouting()
	if err != nil {
		return "priority"
	}
	return cfg.Strategy
}

func (r *RouterRouting) matchTimeSchedule() string {
	cfg, _ := r.Store.GetRouting()
	if !strings.EqualFold(cfg.Strategy, "time_schedule") {
		return ""
	}
	// Parse time_schedule_json minimally — return first connection if parse fails
	var slots []struct {
		Start        string `json:"start"`
		End          string `json:"end"`
		ConnectionID string `json:"connectionId"`
	}
	_ = json.Unmarshal([]byte(cfg.TimeScheduleJSON), &slots)
	now := time.Now()
	for _, s := range slots {
		if s.ConnectionID == "" {
			continue
		}
		// Simplified: skip strict LocalTime parse in stub
		_ = now
		return s.ConnectionID
	}
	return ""
}
