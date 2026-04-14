package repo

import (
	"errors"

	"github.com/example/aigc-server-go/internal/model"
	"gorm.io/gorm"
)

type Stores struct {
	DB *gorm.DB
}

func (s *Stores) AllConnections() ([]model.ConnectionConfig, error) {
	var rows []model.ConnectionConfig
	err := s.DB.Order("created_at desc").Find(&rows).Error
	return rows, err
}

func (s *Stores) GetConnection(id string) (*model.ConnectionConfig, error) {
	var c model.ConnectionConfig
	if err := s.DB.Where("id = ?", id).First(&c).Error; err != nil {
		return nil, err
	}
	return &c, nil
}

func (s *Stores) SaveConnection(c *model.ConnectionConfig) error {
	return s.DB.Save(c).Error
}

func (s *Stores) DeleteConnection(id string) error {
	return s.DB.Delete(&model.ConnectionConfig{}, "id = ?", id).Error
}

func (s *Stores) AllModels() ([]model.ModelConfig, error) {
	var rows []model.ModelConfig
	err := s.DB.Order("created_at desc").Find(&rows).Error
	return rows, err
}

func (s *Stores) GetModel(id string) (*model.ModelConfig, error) {
	var m model.ModelConfig
	if err := s.DB.Where("id = ?", id).First(&m).Error; err != nil {
		return nil, err
	}
	return &m, nil
}

func (s *Stores) SaveModel(m *model.ModelConfig) error {
	return s.DB.Save(m).Error
}

func (s *Stores) DeleteModel(id string) error {
	return s.DB.Delete(&model.ModelConfig{}, "id = ?", id).Error
}

func (s *Stores) GetRouting() (*model.RoutingConfig, error) {
	var r model.RoutingConfig
	err := s.DB.Where("id = ?", 1).First(&r).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			r = model.RoutingConfig{
				ID:                        1,
				Strategy:                  "priority",
				PriorityConnectionIDsJSON: "[]",
				FailoverEnabled:           false,
				FailoverTimeoutSeconds:    10,
				TimeScheduleJSON:          "[]",
			}
			if err := s.DB.Create(&r).Error; err != nil {
				return nil, err
			}
			return &r, nil
		}
		return nil, err
	}
	return &r, nil
}

func (s *Stores) SaveRouting(r *model.RoutingConfig) error {
	return s.DB.Save(r).Error
}

func (s *Stores) SaveTask(t *model.GenerationTask) error {
	return s.DB.Save(t).Error
}

func (s *Stores) GetTask(id string) (*model.GenerationTask, error) {
	var t model.GenerationTask
	if err := s.DB.Where("task_id = ?", id).First(&t).Error; err != nil {
		return nil, err
	}
	return &t, nil
}

func (s *Stores) DeleteTask(id string) error {
	return s.DB.Delete(&model.GenerationTask{}, "task_id = ?", id).Error
}

func (s *Stores) ListTasksForPage(owner string, mode *string, page, pageSize int) ([]model.GenerationTask, error) {
	q := s.DB.Model(&model.GenerationTask{}).Where("owner_id = ?", owner)
	if mode != nil && *mode != "" {
		q = q.Where("mode = ?", *mode)
	}
	offset := (page - 1) * pageSize
	if offset < 0 {
		offset = 0
	}
	var rows []model.GenerationTask
	err := q.Order("created_at desc").Limit(pageSize).Offset(offset).Find(&rows).Error
	return rows, err
}

func (s *Stores) CountTasks(owner string, mode *string) int64 {
	q := s.DB.Model(&model.GenerationTask{}).Where("owner_id = ?", owner)
	if mode != nil && *mode != "" {
		q = q.Where("mode = ?", *mode)
	}
	var n int64
	q.Count(&n)
	return n
}

// Router API keys
func (s *Stores) ListRouterKeys() ([]model.RouterAPIKey, error) {
	var rows []model.RouterAPIKey
	err := s.DB.Order("created_at desc").Find(&rows).Error
	return rows, err
}

func (s *Stores) SaveRouterKey(k *model.RouterAPIKey) error {
	return s.DB.Save(k).Error
}

func (s *Stores) GetRouterKey(id string) (*model.RouterAPIKey, error) {
	var k model.RouterAPIKey
	if err := s.DB.Where("id = ?", id).First(&k).Error; err != nil {
		return nil, err
	}
	return &k, nil
}

func (s *Stores) FindRouterKeyByValue(v string) (*model.RouterAPIKey, error) {
	var k model.RouterAPIKey
	if err := s.DB.Where("key_value = ?", v).First(&k).Error; err != nil {
		return nil, err
	}
	return &k, nil
}

func (s *Stores) DeleteRouterKey(id string) error {
	return s.DB.Delete(&model.RouterAPIKey{}, "id = ?", id).Error
}

func (s *Stores) SaveRouterLog(l *model.RouterRequestLog) error {
	return s.DB.Create(l).Error
}

func (s *Stores) ListRouterLogs(limit int) ([]model.RouterRequestLog, error) {
	var rows []model.RouterRequestLog
	q := s.DB.Order("timestamp desc")
	if limit > 0 {
		q = q.Limit(limit)
	}
	err := q.Find(&rows).Error
	return rows, err
}
