package repo

import (
	"errors"

	"github.com/example/aigc-server-go/internal/consts"
	"github.com/example/aigc-server-go/internal/model"
	"gorm.io/gorm"
)

type ScriptRepo struct {
	DB *gorm.DB
}

func (r *ScriptRepo) FindByID(projectID string) (*model.ScriptProjectAggregate, error) {
	var p model.ScriptProject
	if err := r.DB.Where("project_id = ?", projectID).First(&p).Error; err != nil {
		return nil, err
	}
	agg := &model.ScriptProjectAggregate{Project: &p}
	r.DB.Where("project_id = ?", projectID).Order("revision_index desc").Find(&agg.Revisions)
	r.DB.Where("project_id = ?", projectID).Find(&agg.Documents)
	r.DB.Where("project_id = ?", projectID).Find(&agg.Files)
	r.DB.Where("project_id = ?", projectID).Find(&agg.Assets)
	r.DB.Where("project_id = ?", projectID).Find(&agg.Keyframes)
	r.DB.Where("project_id = ?", projectID).Find(&agg.Shots)
	r.DB.Where("project_id = ?", projectID).Find(&agg.VideoTasks)
	r.DB.Where("project_id = ?", projectID).Find(&agg.PipelineRuns)
	return agg, nil
}

func (r *ScriptRepo) FindByIDIncludeDeleted(projectID string) (*model.ScriptProjectAggregate, error) {
	return r.FindByID(projectID)
}

// Save mirrors JpaScriptProjectRepository.save (delete children then insert).
func (r *ScriptRepo) Save(agg *model.ScriptProjectAggregate) error {
	if agg.Project == nil {
		return errors.New("nil project")
	}
	pid := agg.Project.ProjectID
	return r.DB.Transaction(func(tx *gorm.DB) error {
		if err := tx.Save(agg.Project).Error; err != nil {
			return err
		}
		tx.Where("project_id = ?", pid).Delete(&model.ScriptRevision{})
		tx.Where("project_id = ?", pid).Delete(&model.ScriptDocumentVersion{})
		tx.Where("project_id = ?", pid).Delete(&model.StoredFileRecord{})
		tx.Where("project_id = ?", pid).Delete(&model.ExtractedAsset{})
		tx.Where("project_id = ?", pid).Delete(&model.KeyframeRecord{})
		tx.Where("project_id = ?", pid).Delete(&model.StoryboardShot{})
		tx.Where("project_id = ?", pid).Delete(&model.VideoSegmentTask{})
		tx.Where("project_id = ?", pid).Delete(&model.PipelineRun{})

		for i := range agg.Revisions {
			agg.Revisions[i].ProjectID = pid
			if err := tx.Create(&agg.Revisions[i]).Error; err != nil {
				return err
			}
		}
		for i := range agg.Documents {
			agg.Documents[i].ProjectID = pid
			if err := tx.Create(&agg.Documents[i]).Error; err != nil {
				return err
			}
		}
		for i := range agg.Files {
			agg.Files[i].ProjectID = pid
			if err := tx.Create(&agg.Files[i]).Error; err != nil {
				return err
			}
		}
		for i := range agg.Assets {
			agg.Assets[i].ProjectID = pid
			if err := tx.Create(&agg.Assets[i]).Error; err != nil {
				return err
			}
		}
		for i := range agg.Keyframes {
			agg.Keyframes[i].ProjectID = pid
			if err := tx.Create(&agg.Keyframes[i]).Error; err != nil {
				return err
			}
		}
		for i := range agg.Shots {
			agg.Shots[i].ProjectID = pid
			if err := tx.Create(&agg.Shots[i]).Error; err != nil {
				return err
			}
		}
		for i := range agg.VideoTasks {
			agg.VideoTasks[i].ProjectID = pid
			if err := tx.Create(&agg.VideoTasks[i]).Error; err != nil {
				return err
			}
		}
		for i := range agg.PipelineRuns {
			agg.PipelineRuns[i].ProjectID = pid
			if err := tx.Create(&agg.PipelineRuns[i]).Error; err != nil {
				return err
			}
		}
		return nil
	})
}

func (r *ScriptRepo) ListSummaries(deleted bool) ([]model.ScriptProject, error) {
	q := r.DB.Model(&model.ScriptProject{})
	if deleted {
		q = q.Where("deleted_at IS NOT NULL")
	} else {
		q = q.Where("deleted_at IS NULL")
	}
	var rows []model.ScriptProject
	if err := q.Find(&rows).Error; err != nil {
		return nil, err
	}
	out := make([]model.ScriptProject, 0, len(rows))
	for _, p := range rows {
		if p.ProjectID == consts.WorkspaceProjectID {
			continue
		}
		out = append(out, p)
	}
	return out, nil
}

func (r *ScriptRepo) SoftDelete(projectID string) error {
	now := gorm.Expr("CURRENT_TIMESTAMP")
	return r.DB.Model(&model.ScriptProject{}).Where("project_id = ?", projectID).Updates(map[string]any{
		"deleted_at": now,
		"updated_at": now,
	}).Error
}
