package server

import (
	"github.com/example/aigc-server-go/internal/catalog"
	"github.com/example/aigc-server-go/internal/config"
	"github.com/example/aigc-server-go/internal/crypto"
	"github.com/example/aigc-server-go/internal/gateway"
	"github.com/example/aigc-server-go/internal/repo"
	"github.com/example/aigc-server-go/internal/service"
	"gorm.io/gorm"
)

// App wires dependencies for HTTP handlers.
type App struct {
	Config      *config.Config
	DB          *gorm.DB
	Crypto      *crypto.Service
	Catalog     *catalog.Catalog
	Gateway     *gateway.HTTPGateway
	Stores      *repo.Stores
	ScriptRepo  *repo.ScriptRepo
	Script      *service.ScriptProject
	Routing     *service.RouterRouting
	Gen         *service.Generation
	Local       *service.LocalFile
	Connections *service.ConnectionAdmin
	Models      *service.ModelAdmin
}
