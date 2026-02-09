package com.lfr.dynamicforms

import com.lfr.dynamicforms.routes.adminRoutes
import com.lfr.dynamicforms.routes.formRoutes
import com.lfr.dynamicforms.storage.DatabaseFactory
import com.lfr.dynamicforms.storage.FormStore
import com.lfr.dynamicforms.storage.SubmissionStore
import com.lfr.dynamicforms.validation.Validator
import com.lfr.dynamicforms.validation.VisibilityEvaluator
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    val jdbcUrl = System.getenv("JDBC_URL") ?: "jdbc:sqlite:dynamicforms.db"
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") { module(jdbcUrl) }
        .start(wait = true)
}

fun Application.module(jdbcUrl: String = "jdbc:sqlite:dynamicforms.db") {
    val appJson = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    DatabaseFactory.init(jdbcUrl)

    install(ContentNegotiation) {
        json(appJson)
    }
    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }
    install(CallLogging)

    val adminUser = System.getenv("ADMIN_USER") ?: "admin"
    val adminPass = System.getenv("ADMIN_PASS") ?: "admin"
    install(Authentication) {
        basic("admin-auth") {
            realm = "Admin"
            validate { credentials ->
                if (credentials.name == adminUser && credentials.password == adminPass) {
                    UserIdPrincipal(credentials.name)
                } else null
            }
        }
    }

    val formStore = FormStore(appJson)
    val submissionStore = SubmissionStore(appJson)
    val validator = Validator(VisibilityEvaluator())

    routing {
        formRoutes(formStore, submissionStore, validator)
        authenticate("admin-auth") {
            adminRoutes(formStore, submissionStore)
            staticResources("/admin", "admin", index = "index.html")
        }
    }
}
