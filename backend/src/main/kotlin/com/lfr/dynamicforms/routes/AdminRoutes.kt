package com.lfr.dynamicforms.routes

import com.lfr.dynamicforms.model.FormDefinition
import com.lfr.dynamicforms.storage.FormStore
import com.lfr.dynamicforms.storage.SubmissionStore
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.adminRoutes(
    formStore: FormStore,
    submissionStore: SubmissionStore
) {
    post("/admin/forms") {
        val form = call.receive<FormDefinition>()
        formStore.saveForm(form)
        call.respond(HttpStatusCode.Created, form)
    }

    put("/admin/forms/{formId}") {
        val formId = call.parameters["formId"]
            ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing formId")
        formStore.getForm(formId)
            ?: return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "Form not found"))
        val form = call.receive<FormDefinition>()
        formStore.saveForm(form)
        call.respond(HttpStatusCode.OK, form)
    }

    delete("/admin/forms/{formId}") {
        val formId = call.parameters["formId"]
            ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing formId")
        if (formStore.deleteForm(formId)) {
            call.respond(HttpStatusCode.NoContent)
        } else {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Form not found"))
        }
    }

    get("/admin/forms/{formId}/submissions") {
        val formId = call.parameters["formId"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing formId")
        call.respond(submissionStore.getSubmissions(formId))
    }
}
