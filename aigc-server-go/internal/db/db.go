package db

import (
	"github.com/example/aigc-server-go/internal/config"
	"gorm.io/driver/mysql"
	"gorm.io/gorm"
)

func Open(cfg *config.Config) (*gorm.DB, error) {
	return gorm.Open(mysql.Open(cfg.DSN()), &gorm.Config{
		DisableForeignKeyConstraintWhenMigrating: true,
	})
}
