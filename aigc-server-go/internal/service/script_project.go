package service

import (
	"strings"

	"github.com/example/aigc-server-go/internal/errs"
	"github.com/example/aigc-server-go/internal/model"
	"github.com/example/aigc-server-go/internal/repo"
)

type ScriptProject struct {
	Repo  *repo.ScriptRepo
	Local *LocalFile
}

func (s *ScriptProject) Require(projectID string) (*model.ScriptProjectAggregate, error) {
	agg, err := s.Repo.FindByID(projectID)
	if err != nil {
		return nil, errs.New(404, "剧本工程不存在")
	}
	if agg.Project.DeletedAt != nil {
		return nil, errs.New(404, "剧本工程不存在或已删除")
	}
	return agg, nil
}

func (s *ScriptProject) Save(agg *model.ScriptProjectAggregate) error {
	return s.Repo.Save(agg)
}

func (s *ScriptProject) UpsertFile(agg *model.ScriptProjectAggregate, rec *model.StoredFileRecord) {
	if agg == nil || rec == nil {
		return
	}
	var nf []model.StoredFileRecord
	for _, item := range agg.Files {
		if item.RelativePath == rec.RelativePath || item.FileID == rec.FileID {
			continue
		}
		nf = append(nf, item)
	}
	nf = append(nf, *rec)
	agg.Files = nf
}

func (s *ScriptProject) FindFile(agg *model.ScriptProjectAggregate, fileID string) *model.StoredFileRecord {
	if fileID == "" {
		return nil
	}
	for i := range agg.Files {
		if agg.Files[i].FileID == fileID {
			return &agg.Files[i]
		}
	}
	return nil
}

func (s *ScriptProject) ReadText(rec *model.StoredFileRecord) string {
	if rec == nil {
		return ""
	}
	return s.Local.ReadText(rec)
}

func (s *ScriptProject) ReadJSON(rec *model.StoredFileRecord) map[string]any {
	if rec == nil {
		return map[string]any{}
	}
	return s.Local.ReadJSON(rec)
}

// ResolveWorkflowModel stub — full Java WorkflowModelKey map in follow-up.
func (s *ScriptProject) ResolveWorkflowModel(p *model.ScriptProject, functionKey string, capability string) string {
	_ = functionKey
	_ = capability
	if capability == "text" && strings.TrimSpace(p.ExplicitTextModel) != "" {
		return p.ExplicitTextModel
	}
	if capability == "image" && strings.TrimSpace(p.ExplicitImageModel) != "" {
		return p.ExplicitImageModel
	}
	if capability == "video" && strings.TrimSpace(p.ExplicitVideoModel) != "" {
		return p.ExplicitVideoModel
	}
	return ""
}
