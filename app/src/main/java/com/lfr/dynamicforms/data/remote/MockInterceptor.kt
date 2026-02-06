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
        "safety_inspection_v1" -> SAFETY_INSPECTION_FORM_JSON
        "job_application_v1" -> JOB_APPLICATION_FORM_JSON
        "event_registration_v1" -> EVENT_REGISTRATION_FORM_JSON
        else -> REGISTRATION_FORM_JSON
    }
}

private val FORM_LIST_JSON = """
[
  {"formId":"registration_v1","title":"User Registration","description":"Register a new account","pageCount":3,"fieldCount":12},
  {"formId":"feedback_v1","title":"Feedback Form","description":"Share your feedback","pageCount":1,"fieldCount":4},
  {"formId":"safety_inspection_v1","title":"Safety Inspection","description":"Workplace safety checklist with photo evidence","pageCount":3,"fieldCount":14},
  {"formId":"job_application_v1","title":"Job Application","description":"Apply for an open position","pageCount":3,"fieldCount":15},
  {"formId":"event_registration_v1","title":"Event Registration","description":"Register for an upcoming event","pageCount":2,"fieldCount":13}
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

private val SAFETY_INSPECTION_FORM_JSON = """
{
  "formId": "safety_inspection_v1",
  "title": "Safety Inspection",
  "pages": [
    {
      "pageId": "si_page_1",
      "title": "Site Details",
      "elements": [
        {
          "type": "section_header",
          "id": "si_header_site",
          "label": "Inspection Site",
          "subtitle": "Identify the location and inspector"
        },
        {
          "type": "text_field",
          "id": "site_name",
          "label": "Site / Building Name",
          "required": true
        },
        {
          "type": "dropdown",
          "id": "site_area",
          "label": "Area",
          "required": true,
          "options": [
            {"value": "warehouse", "label": "Warehouse"},
            {"value": "office", "label": "Office"},
            {"value": "factory", "label": "Factory Floor"},
            {"value": "outdoor", "label": "Outdoor / Yard"}
          ]
        },
        {
          "type": "text_field",
          "id": "inspector_name",
          "label": "Inspector Name",
          "required": true
        },
        {
          "type": "date_picker",
          "id": "inspection_date",
          "label": "Inspection Date",
          "required": true
        }
      ]
    },
    {
      "pageId": "si_page_2",
      "title": "Checklist",
      "elements": [
        {
          "type": "section_header",
          "id": "si_header_checks",
          "label": "Safety Checks",
          "subtitle": "Check each item and note any issues"
        },
        {
          "type": "checkbox",
          "id": "fire_exits_clear",
          "label": "Fire exits are unobstructed and clearly marked"
        },
        {
          "type": "checkbox",
          "id": "extinguishers_ok",
          "label": "Fire extinguishers are present and inspected"
        },
        {
          "type": "checkbox",
          "id": "ppe_available",
          "label": "Required PPE is available and in good condition"
        },
        {
          "type": "checkbox",
          "id": "first_aid_stocked",
          "label": "First aid kits are stocked and accessible"
        },
        {
          "type": "toggle",
          "id": "hazards_found",
          "label": "Were any hazards found?"
        },
        {
          "type": "repeating_group",
          "id": "hazard_list",
          "label": "Hazards",
          "minItems": 1,
          "maxItems": 5,
          "visibleWhen": {"fieldId": "hazards_found", "operator": "equals", "value": "true"},
          "elements": [
            {
              "type": "text_field",
              "id": "hazard_desc",
              "label": "Description",
              "required": true
            },
            {
              "type": "dropdown",
              "id": "hazard_severity",
              "label": "Severity",
              "required": true,
              "options": [
                {"value": "low", "label": "Low"},
                {"value": "medium", "label": "Medium"},
                {"value": "high", "label": "High"},
                {"value": "critical", "label": "Critical"}
              ]
            }
          ]
        }
      ]
    },
    {
      "pageId": "si_page_3",
      "title": "Evidence & Sign-off",
      "elements": [
        {
          "type": "section_header",
          "id": "si_header_evidence",
          "label": "Supporting Evidence"
        },
        {
          "type": "file_upload",
          "id": "photo_evidence",
          "label": "Photo Evidence",
          "allowedTypes": ["jpg", "png", "heic"]
        },
        {
          "type": "text_field",
          "id": "additional_notes",
          "label": "Additional Notes",
          "multiline": true
        },
        {
          "type": "signature",
          "id": "inspector_signature",
          "label": "Inspector Signature",
          "required": true
        }
      ]
    }
  ]
}
""".trimIndent()

private val JOB_APPLICATION_FORM_JSON = """
{
  "formId": "job_application_v1",
  "title": "Job Application",
  "pages": [
    {
      "pageId": "ja_page_1",
      "title": "Personal Details",
      "elements": [
        {
          "type": "section_header",
          "id": "ja_header_personal",
          "label": "About You",
          "subtitle": "Tell us who you are"
        },
        {
          "type": "text_field",
          "id": "applicant_name",
          "label": "Full Name",
          "required": true,
          "validation": {"minLength": 2, "maxLength": 80}
        },
        {
          "type": "text_field",
          "id": "email",
          "label": "Email Address",
          "required": true,
          "validation": {"pattern": "^[^@]+@[^@]+\\.[^@]+$", "errorMessage": "Enter a valid email"}
        },
        {
          "type": "text_field",
          "id": "phone",
          "label": "Phone Number"
        },
        {
          "type": "dropdown",
          "id": "position",
          "label": "Position Applied For",
          "required": true,
          "options": [
            {"value": "android_dev", "label": "Android Developer"},
            {"value": "ios_dev", "label": "iOS Developer"},
            {"value": "backend_dev", "label": "Backend Developer"},
            {"value": "designer", "label": "UI/UX Designer"},
            {"value": "qa", "label": "QA Engineer"}
          ]
        },
        {
          "type": "radio",
          "id": "work_auth",
          "label": "Are you authorized to work in this country?",
          "required": true,
          "options": [
            {"value": "yes", "label": "Yes"},
            {"value": "no", "label": "No"},
            {"value": "visa_needed", "label": "I need visa sponsorship"}
          ]
        }
      ]
    },
    {
      "pageId": "ja_page_2",
      "title": "Experience",
      "elements": [
        {
          "type": "section_header",
          "id": "ja_header_exp",
          "label": "Work Experience"
        },
        {
          "type": "slider",
          "id": "years_exp",
          "label": "Total Years of Experience",
          "min": 0,
          "max": 30,
          "step": 1
        },
        {
          "type": "repeating_group",
          "id": "work_history",
          "label": "Previous Positions",
          "minItems": 1,
          "maxItems": 5,
          "elements": [
            {
              "type": "text_field",
              "id": "company",
              "label": "Company",
              "required": true
            },
            {
              "type": "text_field",
              "id": "job_title",
              "label": "Job Title",
              "required": true
            },
            {
              "type": "text_field",
              "id": "duration",
              "label": "Duration (e.g., 2 years)"
            }
          ]
        },
        {
          "type": "section_header",
          "id": "ja_header_skills",
          "label": "Skills"
        },
        {
          "type": "multi_select",
          "id": "tech_skills",
          "label": "Technical Skills",
          "options": [
            {"value": "kotlin", "label": "Kotlin"},
            {"value": "java", "label": "Java"},
            {"value": "swift", "label": "Swift"},
            {"value": "python", "label": "Python"},
            {"value": "typescript", "label": "TypeScript"},
            {"value": "go", "label": "Go"},
            {"value": "sql", "label": "SQL"}
          ]
        }
      ]
    },
    {
      "pageId": "ja_page_3",
      "title": "Documents",
      "elements": [
        {
          "type": "section_header",
          "id": "ja_header_docs",
          "label": "Upload Documents"
        },
        {
          "type": "file_upload",
          "id": "resume",
          "label": "Resume / CV",
          "required": true,
          "allowedTypes": ["pdf", "docx"]
        },
        {
          "type": "file_upload",
          "id": "cover_letter",
          "label": "Cover Letter",
          "allowedTypes": ["pdf", "docx"]
        },
        {
          "type": "toggle",
          "id": "has_portfolio",
          "label": "I have a portfolio to share"
        },
        {
          "type": "text_field",
          "id": "portfolio_url",
          "label": "Portfolio URL",
          "visibleWhen": {"fieldId": "has_portfolio", "operator": "equals", "value": "true"}
        },
        {
          "type": "text_field",
          "id": "cover_note",
          "label": "Anything else you'd like us to know?",
          "multiline": true
        },
        {
          "type": "checkbox",
          "id": "consent",
          "label": "I consent to the processing of my personal data",
          "required": true
        }
      ]
    }
  ]
}
""".trimIndent()

private val EVENT_REGISTRATION_FORM_JSON = """
{
  "formId": "event_registration_v1",
  "title": "Event Registration",
  "pages": [
    {
      "pageId": "ev_page_1",
      "title": "Attendee Info",
      "elements": [
        {
          "type": "section_header",
          "id": "ev_header_attendee",
          "label": "Attendee Information",
          "subtitle": "Who's coming?"
        },
        {
          "type": "text_field",
          "id": "attendee_name",
          "label": "Full Name",
          "required": true
        },
        {
          "type": "text_field",
          "id": "attendee_email",
          "label": "Email",
          "required": true,
          "validation": {"pattern": "^[^@]+@[^@]+\\.[^@]+$", "errorMessage": "Enter a valid email"}
        },
        {
          "type": "dropdown",
          "id": "ticket_type",
          "label": "Ticket Type",
          "required": true,
          "options": [
            {"value": "general", "label": "General Admission"},
            {"value": "vip", "label": "VIP"},
            {"value": "speaker", "label": "Speaker"},
            {"value": "sponsor", "label": "Sponsor"}
          ]
        },
        {
          "type": "text_field",
          "id": "company_org",
          "label": "Company / Organization",
          "visibleWhen": {"fieldId": "ticket_type", "operator": "not_equals", "value": "general"}
        },
        {
          "type": "text_field",
          "id": "talk_title",
          "label": "Talk Title",
          "required": true,
          "visibleWhen": {"fieldId": "ticket_type", "operator": "equals", "value": "speaker"}
        },
        {
          "type": "text_field",
          "id": "talk_abstract",
          "label": "Talk Abstract",
          "multiline": true,
          "visibleWhen": {"fieldId": "ticket_type", "operator": "equals", "value": "speaker"}
        },
        {
          "type": "number_field",
          "id": "group_size",
          "label": "Number of Attendees in Your Group",
          "visibleWhen": {"fieldId": "ticket_type", "operator": "equals", "value": "sponsor"},
          "validation": {"min": 1, "max": 20}
        }
      ]
    },
    {
      "pageId": "ev_page_2",
      "title": "Preferences",
      "elements": [
        {
          "type": "section_header",
          "id": "ev_header_prefs",
          "label": "Event Preferences"
        },
        {
          "type": "multi_select",
          "id": "sessions",
          "label": "Which sessions will you attend?",
          "options": [
            {"value": "keynote", "label": "Opening Keynote"},
            {"value": "workshop_android", "label": "Android Workshop"},
            {"value": "workshop_ios", "label": "iOS Workshop"},
            {"value": "panel_ai", "label": "AI Panel Discussion"},
            {"value": "networking", "label": "Networking Mixer"}
          ],
          "validation": {"minSelections": 1, "errorMessage": "Select at least one session"}
        },
        {
          "type": "radio",
          "id": "dietary",
          "label": "Dietary Requirements",
          "options": [
            {"value": "none", "label": "None"},
            {"value": "vegetarian", "label": "Vegetarian"},
            {"value": "vegan", "label": "Vegan"},
            {"value": "halal", "label": "Halal"},
            {"value": "other", "label": "Other"}
          ]
        },
        {
          "type": "text_field",
          "id": "dietary_other",
          "label": "Please specify dietary requirements",
          "visibleWhen": {"fieldId": "dietary", "operator": "equals", "value": "other"}
        },
        {
          "type": "toggle",
          "id": "needs_accommodation",
          "label": "I need accessibility accommodations"
        },
        {
          "type": "text_field",
          "id": "accommodation_details",
          "label": "Please describe your needs",
          "multiline": true,
          "visibleWhen": {"fieldId": "needs_accommodation", "operator": "equals", "value": "true"}
        },
        {
          "type": "checkbox",
          "id": "code_of_conduct",
          "label": "I agree to the event code of conduct",
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
