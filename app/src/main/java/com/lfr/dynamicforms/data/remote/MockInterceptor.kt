package com.lfr.dynamicforms.data.remote

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class MockInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method

        val (code, json) = when {
            method == "GET" && path == "/forms" -> 200 to FORM_LIST_JSON
            method == "GET" && path.matches(Regex("/forms/[^/]+")) -> {
                val formId = path.removePrefix("/forms/")
                200 to getFormJson(formId)
            }
            method == "POST" && path.matches(Regex("/forms/[^/]+/submit")) -> 200 to SUBMIT_SUCCESS_JSON
            else -> 404 to """{"error":"not found"}"""
        }

        return Response.Builder()
            .code(code)
            .message("OK")
            .protocol(Protocol.HTTP_1_1)
            .request(request)
            .body(json.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun getFormJson(formId: String): String = when (formId) {
        "registration_v1" -> REGISTRATION_FORM_JSON
        "feedback_v1" -> FEEDBACK_FORM_JSON
        else -> REGISTRATION_FORM_JSON
    }
}

private val FORM_LIST_JSON = """
[
  {"formId":"registration_v1","title":"User Registration","description":"Register a new account"},
  {"formId":"feedback_v1","title":"Feedback Form","description":"Share your feedback"}
]
""".trimIndent()

private val SUBMIT_SUCCESS_JSON = """
{"success":true,"message":"Form submitted successfully"}
""".trimIndent()

private val REGISTRATION_FORM_JSON = """
{
  "formId": "registration_v1",
  "title": "User Registration",
  "pages": [
    {
      "pageId": "page_1",
      "title": "Personal Info",
      "elements": [
        {
          "type": "section_header",
          "id": "header_personal",
          "label": "Personal Information",
          "subtitle": "Tell us about yourself"
        },
        {
          "type": "text_field",
          "id": "full_name",
          "label": "Full Name",
          "required": true,
          "validation": {
            "minLength": 2,
            "maxLength": 100,
            "errorMessage": "Name must be 2-100 characters"
          }
        },
        {
          "type": "number_field",
          "id": "age",
          "label": "Age",
          "required": true,
          "validation": {"min": 18, "max": 120}
        },
        {
          "type": "date_picker",
          "id": "birth_date",
          "label": "Date of Birth"
        },
        {
          "type": "dropdown",
          "id": "role",
          "label": "Role",
          "required": true,
          "options": [
            {"value": "dev", "label": "Developer"},
            {"value": "design", "label": "Designer"},
            {"value": "pm", "label": "Product Manager"}
          ]
        },
        {
          "type": "text_field",
          "id": "company_name",
          "label": "Company Name",
          "visibleWhen": {"fieldId": "role", "operator": "equals", "value": "dev"}
        }
      ]
    },
    {
      "pageId": "page_2",
      "title": "Preferences",
      "elements": [
        {
          "type": "toggle",
          "id": "newsletter",
          "label": "Subscribe to newsletter",
          "defaultValue": true
        },
        {
          "type": "slider",
          "id": "experience",
          "label": "Years of experience",
          "min": 0,
          "max": 30,
          "step": 1
        },
        {
          "type": "radio",
          "id": "contact_pref",
          "label": "Preferred contact method",
          "required": true,
          "options": [
            {"value": "email", "label": "Email"},
            {"value": "phone", "label": "Phone"},
            {"value": "none", "label": "No contact"}
          ]
        },
        {
          "type": "multi_select",
          "id": "interests",
          "label": "Interests",
          "options": [
            {"value": "android", "label": "Android"},
            {"value": "ios", "label": "iOS"},
            {"value": "web", "label": "Web"},
            {"value": "backend", "label": "Backend"}
          ]
        },
        {
          "type": "checkbox",
          "id": "terms",
          "label": "I agree to the terms and conditions",
          "required": true
        }
      ]
    },
    {
      "pageId": "page_3",
      "title": "Additional",
      "elements": [
        {
          "type": "text_field",
          "id": "bio",
          "label": "Bio",
          "multiline": true
        },
        {
          "type": "label",
          "id": "upload_info",
          "label": "Attachments",
          "text": "Upload your resume and sign below."
        },
        {
          "type": "file_upload",
          "id": "resume",
          "label": "Upload Resume",
          "allowedTypes": ["pdf", "doc", "docx"],
          "maxFileSize": 5242880
        },
        {
          "type": "signature",
          "id": "signature",
          "label": "Signature",
          "required": true
        }
      ]
    }
  ]
}
""".trimIndent()

private val FEEDBACK_FORM_JSON = """
{
  "formId": "feedback_v1",
  "title": "Feedback Form",
  "pages": [
    {
      "pageId": "fb_page_1",
      "title": "Your Feedback",
      "elements": [
        {
          "type": "section_header",
          "id": "fb_header",
          "label": "We value your feedback"
        },
        {
          "type": "slider",
          "id": "rating",
          "label": "Overall Rating",
          "min": 1,
          "max": 5,
          "step": 1,
          "required": true
        },
        {
          "type": "text_field",
          "id": "comments",
          "label": "Comments",
          "multiline": true
        },
        {
          "type": "radio",
          "id": "recommend",
          "label": "Would you recommend us?",
          "required": true,
          "options": [
            {"value": "yes", "label": "Yes"},
            {"value": "no", "label": "No"},
            {"value": "maybe", "label": "Maybe"}
          ]
        }
      ]
    }
  ]
}
""".trimIndent()
