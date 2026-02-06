package com.lfr.dynamicforms.data.serialization

import com.lfr.dynamicforms.domain.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.*
import org.junit.Test

class FormDeserializationTest {

    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        isLenient = true
    }

    // region Individual element tests

    @Test
    fun `deserialize text_field element`() {
        val jsonString = """
        {
            "formId": "test",
            "title": "Test",
            "pages": [{
                "pageId": "p1",
                "title": "Page 1",
                "elements": [{
                    "type": "text_field",
                    "id": "name",
                    "label": "Full Name",
                    "required": true,
                    "validation": {
                        "minLength": 2,
                        "maxLength": 100,
                        "errorMessage": "Name must be 2-100 characters"
                    }
                }]
            }]
        }
        """.trimIndent()

        val form = json.decodeFromString<Form>(jsonString)
        val element = form.pages[0].elements[0]

        assertTrue(element is TextFieldElement)
        val textField = element as TextFieldElement
        assertEquals("name", textField.id)
        assertEquals("Full Name", textField.label)
        assertTrue(textField.required)
        assertNotNull(textField.validation)
        assertEquals(2, textField.validation!!.minLength)
        assertEquals(100, textField.validation!!.maxLength)
        assertEquals("Name must be 2-100 characters", textField.validation!!.errorMessage)
    }

    @Test
    fun `deserialize number_field element`() {
        val jsonString = """
        {
            "formId": "test",
            "title": "Test",
            "pages": [{
                "pageId": "p1",
                "title": "Page 1",
                "elements": [{
                    "type": "number_field",
                    "id": "age",
                    "label": "Age",
                    "required": true,
                    "validation": {
                        "min": 18,
                        "max": 120
                    }
                }]
            }]
        }
        """.trimIndent()

        val form = json.decodeFromString<Form>(jsonString)
        val element = form.pages[0].elements[0]

        assertTrue(element is NumberFieldElement)
        val numberField = element as NumberFieldElement
        assertEquals("age", numberField.id)
        assertEquals("Age", numberField.label)
        assertTrue(numberField.required)
        assertNotNull(numberField.validation)
        assertEquals(18.0, numberField.validation!!.min!!, 0.001)
        assertEquals(120.0, numberField.validation!!.max!!, 0.001)
    }

    @Test
    fun `deserialize dropdown with options`() {
        val jsonString = """
        {
            "formId": "test",
            "title": "Test",
            "pages": [{
                "pageId": "p1",
                "title": "Page 1",
                "elements": [{
                    "type": "dropdown",
                    "id": "role",
                    "label": "Role",
                    "required": true,
                    "options": [
                        {"value": "dev", "label": "Developer"},
                        {"value": "design", "label": "Designer"},
                        {"value": "pm", "label": "Product Manager"}
                    ]
                }]
            }]
        }
        """.trimIndent()

        val form = json.decodeFromString<Form>(jsonString)
        val element = form.pages[0].elements[0]

        assertTrue(element is DropdownElement)
        val dropdown = element as DropdownElement
        assertEquals("role", dropdown.id)
        assertEquals("Role", dropdown.label)
        assertTrue(dropdown.required)
        assertEquals(3, dropdown.options.size)
        assertEquals("dev", dropdown.options[0].value)
        assertEquals("Developer", dropdown.options[0].label)
        assertEquals("pm", dropdown.options[2].value)
        assertEquals("Product Manager", dropdown.options[2].label)
    }

    @Test
    fun `deserialize toggle with defaultValue`() {
        val jsonString = """
        {
            "formId": "test",
            "title": "Test",
            "pages": [{
                "pageId": "p1",
                "title": "Page 1",
                "elements": [{
                    "type": "toggle",
                    "id": "newsletter",
                    "label": "Subscribe to newsletter",
                    "defaultValue": true
                }]
            }]
        }
        """.trimIndent()

        val form = json.decodeFromString<Form>(jsonString)
        val element = form.pages[0].elements[0]

        assertTrue(element is ToggleElement)
        val toggle = element as ToggleElement
        assertEquals("newsletter", toggle.id)
        assertEquals("Subscribe to newsletter", toggle.label)
        assertTrue(toggle.defaultValue)
    }

    @Test
    fun `deserialize repeating_group with nested elements`() {
        val jsonString = """
        {
            "formId": "test",
            "title": "Test",
            "pages": [{
                "pageId": "p1",
                "title": "Page 1",
                "elements": [{
                    "type": "repeating_group",
                    "id": "addresses",
                    "label": "Addresses",
                    "minItems": 1,
                    "maxItems": 5,
                    "elements": [
                        {
                            "type": "text_field",
                            "id": "street",
                            "label": "Street",
                            "required": true
                        }
                    ]
                }]
            }]
        }
        """.trimIndent()

        val form = json.decodeFromString<Form>(jsonString)
        val element = form.pages[0].elements[0]

        assertTrue(element is RepeatingGroupElement)
        val group = element as RepeatingGroupElement
        assertEquals("addresses", group.id)
        assertEquals("Addresses", group.label)
        assertEquals(1, group.minItems)
        assertEquals(5, group.maxItems)
        assertEquals(1, group.elements.size)
        assertTrue(group.elements[0] is TextFieldElement)
        assertEquals("street", group.elements[0].id)
    }

    @Test
    fun `deserialize visibility condition`() {
        val jsonString = """
        {
            "formId": "test",
            "title": "Test",
            "pages": [{
                "pageId": "p1",
                "title": "Page 1",
                "elements": [{
                    "type": "text_field",
                    "id": "company_name",
                    "label": "Company Name",
                    "visibleWhen": {
                        "fieldId": "role",
                        "operator": "equals",
                        "value": "dev"
                    }
                }]
            }]
        }
        """.trimIndent()

        val form = json.decodeFromString<Form>(jsonString)
        val element = form.pages[0].elements[0]

        assertTrue(element is TextFieldElement)
        val textField = element as TextFieldElement
        assertNotNull(textField.visibleWhen)
        assertEquals("role", textField.visibleWhen!!.fieldId)
        assertEquals(ConditionOperator.EQUALS, textField.visibleWhen!!.operator)
        assertEquals("dev", textField.visibleWhen!!.value.content)
    }

    // endregion

    // region Full form tests

    @Test
    fun `deserialize full registration form`() {
        val jsonString = """
        {
          "formId": "registration_v1",
          "title": "User Registration",
          "pages": [
            {
              "pageId": "page_1", "title": "Personal Info",
              "elements": [
                {"type":"section_header","id":"header_personal","label":"Personal Information","subtitle":"Tell us about yourself"},
                {"type":"text_field","id":"full_name","label":"Full Name","required":true,"validation":{"minLength":2,"maxLength":100,"errorMessage":"Name must be 2-100 characters"}},
                {"type":"number_field","id":"age","label":"Age","required":true,"validation":{"min":18,"max":120}},
                {"type":"date_picker","id":"birth_date","label":"Date of Birth"},
                {"type":"dropdown","id":"role","label":"Role","required":true,"options":[{"value":"dev","label":"Developer"},{"value":"design","label":"Designer"},{"value":"pm","label":"Product Manager"}]},
                {"type":"text_field","id":"company_name","label":"Company Name","visibleWhen":{"fieldId":"role","operator":"equals","value":"dev"}}
              ]
            },
            {
              "pageId": "page_2", "title": "Preferences",
              "elements": [
                {"type":"toggle","id":"newsletter","label":"Subscribe to newsletter","defaultValue":true},
                {"type":"slider","id":"experience","label":"Years of experience","min":0,"max":30,"step":1},
                {"type":"radio","id":"contact_pref","label":"Preferred contact method","required":true,"options":[{"value":"email","label":"Email"},{"value":"phone","label":"Phone"},{"value":"none","label":"No contact"}]},
                {"type":"multi_select","id":"interests","label":"Interests","options":[{"value":"android","label":"Android"},{"value":"ios","label":"iOS"},{"value":"web","label":"Web"},{"value":"backend","label":"Backend"}]},
                {"type":"checkbox","id":"terms","label":"I agree to the terms and conditions","required":true}
              ]
            },
            {
              "pageId": "page_3", "title": "Additional",
              "elements": [
                {"type":"text_field","id":"bio","label":"Bio","multiline":true},
                {"type":"label","id":"upload_info","label":"Attachments","text":"Upload your resume and sign below."},
                {"type":"signature","id":"signature","label":"Signature","required":true}
              ]
            }
          ]
        }
        """.trimIndent()

        val form = json.decodeFromString<Form>(jsonString)

        assertEquals("registration_v1", form.formId)
        assertEquals("User Registration", form.title)
        assertEquals(3, form.pages.size)

        assertEquals("Personal Info", form.pages[0].title)
        assertEquals(6, form.pages[0].elements.size)

        assertEquals("Preferences", form.pages[1].title)
        assertEquals(5, form.pages[1].elements.size)

        assertEquals("Additional", form.pages[2].title)
        assertEquals(3, form.pages[2].elements.size)
    }

    @Test
    fun `deserialize full feedback form`() {
        val jsonString = """
        {
          "formId": "feedback_v1", "title": "Feedback Form",
          "pages": [
            {
              "pageId": "fb_page_1", "title": "Your Feedback",
              "elements": [
                {"type":"section_header","id":"fb_header","label":"We value your feedback"},
                {"type":"slider","id":"rating","label":"Overall Rating","min":1,"max":5,"step":1,"required":true},
                {"type":"text_field","id":"comments","label":"Comments","multiline":true},
                {"type":"radio","id":"recommend","label":"Would you recommend us?","required":true,"options":[{"value":"yes","label":"Yes"},{"value":"no","label":"No"},{"value":"maybe","label":"Maybe"}]}
              ]
            }
          ]
        }
        """.trimIndent()

        val form = json.decodeFromString<Form>(jsonString)

        assertEquals("feedback_v1", form.formId)
        assertEquals("Feedback Form", form.title)
        assertEquals(1, form.pages.size)
        assertEquals("Your Feedback", form.pages[0].title)
        assertEquals(4, form.pages[0].elements.size)
    }

    @Test
    fun `deserialize form list`() {
        val jsonString = """
        [
          {"formId":"registration_v1","title":"User Registration","description":"Register a new account","pageCount":3,"fieldCount":12},
          {"formId":"feedback_v1","title":"Feedback Form","description":"Share your feedback","pageCount":1,"fieldCount":3}
        ]
        """.trimIndent()

        val list = json.decodeFromString(ListSerializer(FormSummary.serializer()), jsonString)

        assertEquals(2, list.size)
        assertEquals("User Registration", list[0].title)
        assertEquals("registration_v1", list[0].formId)
        assertEquals("Register a new account", list[0].description)
        assertEquals(3, list[0].pageCount)
        assertEquals(12, list[0].fieldCount)
        assertEquals("Feedback Form", list[1].title)
        assertEquals("feedback_v1", list[1].formId)
    }

    @Test
    fun `unknown keys are ignored`() {
        val jsonString = """
        {
            "formId": "test",
            "title": "Test",
            "pages": [{
                "pageId": "p1",
                "title": "Page 1",
                "elements": [{
                    "type": "text_field",
                    "id": "name",
                    "label": "Full Name",
                    "foo": "bar",
                    "extra_number": 42
                }]
            }]
        }
        """.trimIndent()

        val form = json.decodeFromString<Form>(jsonString)
        val element = form.pages[0].elements[0]

        assertTrue(element is TextFieldElement)
        assertEquals("name", element.id)
        assertEquals("Full Name", element.label)
    }

    // endregion
}
