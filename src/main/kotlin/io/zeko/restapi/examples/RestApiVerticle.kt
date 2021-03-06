package io.zeko.restapi.examples

import io.vertx.core.logging.Logger
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.TimeoutHandler
import io.zeko.restapi.core.security.JWTAuthHandler
import io.zeko.restapi.core.security.JWTAuthRefreshHandler
import io.zeko.restapi.core.verticles.ZekoVerticle
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class RestApiVerticle : ZekoVerticle(), KoinComponent {
    val jwtAuth: JWTAuth by inject("jwtAuth")
    val jwtAuthRefresh: JWTAuth by inject("jwtAuthRefresh")
    val logger: Logger by inject()

    val skipAuth = listOf("/user/login", "/user/register", "/user/refresh-token", "/ping-health")

    override suspend fun start() {
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create());
        router.route().handler(TimeoutHandler.create(10000L, 503))

        router.route("/*").handler(JWTAuthHandler(jwtAuth, skipAuth))

        router.post("/user/refresh-token").handler(JWTAuthRefreshHandler(jwtAuth, jwtAuthRefresh))
        //auth access token 60s, refresh token 300s, only allow refresh after token expired
//        router.post("/user/refresh-token").handler(JWTAuthRefreshHandler(jwtAuth, jwtAuthRefresh, 60, 300, true))

        //Make some error
        router.get("/make-error").koto { ctx -> val s = 12 / 0; ctx.response().end("ok") }

        bindRoutes("io.zeko.restapi.examples.controllers.GeneratedRoutes", router, logger)
//        bindRoutes(io.zeko.restapi.examples.controllers.GeneratedRoutes(vertx), router, logger)

        //handles error, output default status code, default message, log error
        handleRuntimeError(router, logger)

        //start running cron jobs
        startCronJobs("io.zeko.restapi.examples.jobs.GeneratedCrons", logger)
//        startCronJobs(io.zeko.restapi.examples.jobs.GeneratedCrons(vertx, logger), logger)

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(config.getInteger("http.port", 9999))
    }
}
