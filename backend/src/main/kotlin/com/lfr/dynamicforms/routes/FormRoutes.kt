package com.lfr.dynamicforms.routes

import com.lfr.dynamicforms.model.FormSubmission
import com.lfr.dynamicforms.model.SubmissionResponse
import com.lfr.dynamicforms.storage.FormStore
import com.lfr.dynamicforms.storage.SubmissionStore
import com.lfr.dynamicforms.validation.Validator
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.concurrent.ConcurrentHashMap

private val processedIdempotencyKeys = ConcurrentHashMap<String, SubmissionResponse>()

fun Route.formRoutes(
    formStore: FormStore,
    submissionStore: SubmissionStore,
    validator: Validator
) {
    get("/forms") {
        call.respond(formStore.getAllSummaries())
    }

    get("/forms/{formId}") {
        val formId = call.parameters["formId"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing formId")
        val form = formStore.getForm(formId)
            ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Form not found"))
        call.respond(form)
    }

    post("/forms/{formId}/submit") {
        val formId = call.parameters["formId"]
            ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing formId")
        val form = formStore.getForm(formId)
            ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Form not found"))

        val idempotencyKey = call.request.headers["Idempotency-Key"]
        if (idempotencyKey != null) {
            val cached = processedIdempotencyKeys[idempotencyKey]
            if (cached != null) {
                call.respond(HttpStatusCode.OK, cached)
                return@post
            }
        }

        val submission = call.receive<FormSubmission>()
        val errors = validator.validate(form, submission.values)
        if (errors.isNotEmpty()) {
            val response = SubmissionResponse(
                success = false,
                message = "Validation failed",
                fieldErrors = errors
            )
            if (idempotencyKey != null) {
                processedIdempotencyKeys[idempotencyKey] = response
            }
            call.respond(HttpStatusCode.OK, response)
            return@post
        }
        submissionStore.addSubmission(formId, submission.values)
        val response = SubmissionResponse(success = true, message = "Form submitted successfully")
        if (idempotencyKey != null) {
            processedIdempotencyKeys[idempotencyKey] = response
        }
        call.respond(HttpStatusCode.OK, response)
    }
}
