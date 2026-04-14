package main

import (
	"fmt"
	"log"

	"github.com/example/aigc-server-go/internal/catalog"
	"github.com/example/aigc-server-go/internal/config"
	"github.com/example/aigc-server-go/internal/crypto"
	"github.com/example/aigc-server-go/internal/db"
	"github.com/example/aigc-server-go/internal/gateway"
	"github.com/example/aigc-server-go/internal/middleware"
	"github.com/example/aigc-server-go/internal/repo"
	"github.com/example/aigc-server-go/internal/server"
	"github.com/example/aigc-server-go/internal/service"
	"github.com/gin-gonic/gin"
)

func main() {
	cfg := config.Load()
	cr, err := crypto.New(cfg.EncryptionKey)
	if err != nil {
		log.Fatal(err)
	}
	gormDB, err := db.Open(cfg)
	if err != nil {
		log.Fatal(err)
	}
	cat := catalog.New()
	gw := gateway.NewHTTPGateway()
	stores := &repo.Stores{DB: gormDB}
	srepo := &repo.ScriptRepo{DB: gormDB}
	local := service.NewLocalFile(cfg)
	scriptSvc := &service.ScriptProject{Repo: srepo, Local: local}
	rr := &service.RouterRouting{Store: stores}
	cap := &service.ModelCapability{}
	gen := &service.Generation{
		Store: stores, Script: scriptSvc, Cfg: cfg, Cat: cat, Crypto: cr, GW: gw,
		Local: local, Routing: rr, Cap: cap, VS: service.VideoStyle{},
	}
	connAdmin := &service.ConnectionAdmin{Store: stores, Cat: cat, Crypto: cr, GW: gw, Routing: rr}
	modelAdmin := &service.ModelAdmin{Store: stores, Cat: cat, Crypto: cr, GW: gw, Cap: cap}

	app := &server.App{
		Config:      cfg,
		DB:          gormDB,
		Crypto:      cr,
		Catalog:     cat,
		Gateway:     gw,
		Stores:      stores,
		ScriptRepo:  srepo,
		Script:      scriptSvc,
		Routing:     rr,
		Gen:         gen,
		Local:       local,
		Connections: connAdmin,
		Models:      modelAdmin,
	}

	r := gin.New()
	r.Use(middleware.Recovery())
	r.Use(middleware.CORSAPI(cfg))
	server.Register(r, app)

	addr := fmt.Sprintf(":%d", cfg.ServerPort)
	log.Printf("listening %s", addr)
	if err := r.Run(addr); err != nil {
		log.Fatal(err)
	}
}
