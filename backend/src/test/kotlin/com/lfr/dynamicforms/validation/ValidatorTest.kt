package com.lfr.dynamicforms.validation

import com.lfr.dynamicforms.model.*
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidatorTest {

    private val validator = Validator(VisibilityEvaluator())

    private fun formWith(vararg elements: FormElement): FormDefinition =
        FormDefinition(
            formId = "test",
            title = "Test",
            pages = listOf(Page("p1", "Page 1", elements.toList()))
        )

    @Test
    fun `required field with empty value returns error`() {
        val form = formWith(
            TextFieldElement(id = "name", label = "Name", required = true)
        )
        val errors = validator.validate(form, emptyMap())
        assertEquals("Name is required", errors["name"])
    }

    @Test
    fun `required field with value passes`() {
        val form = formWith(
            TextFieldElement(id = "name", label = "Name", required = true)
        )
        val errors = validator.validate(form, mapOf("name" to "John"))
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `optional field with empty value passes`() {
        val form = formWith(
            TextFieldElement(id = "name", label = "Name", required = false)
        )
        val errors = validator.validate(form, emptyMap())
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `text minLength validation`() {
        val form = formWith(
            TextFieldElement(
                id = "name", label = "Name",
                validation = TextValidation(minLength = 3)
            )
        )
        val errors = validator.validate(form, mapOf("name" to "ab"))
        assertEquals("Minimum 3 characters", errors["name"])
    }

    @Test
    fun `text maxLength validation`() {
        val form = formWith(
            TextFieldElement(
                id = "name", label = "Name",
                validation = TextValidation(maxLength = 5)
            )
        )
        val errors = validator.validate(form, mapOf("name" to "abcdef"))
        assertEquals("Maximum 5 characters", errors["name"])
    }

    @Test
    fun `text pattern validation`() {
        val form = formWith(
            TextFieldElement(
                id = "email", label = "Email",
                validation = TextValidation(
                    pattern = "^[^@]+@[^@]+\\.[^@]+$",
                    errorMessage = "Enter a valid email"
                )
            )
        )
        val errors = validator.validate(form, mapOf("email" to "invalid"))
        assertEquals("Enter a valid email", errors["email"])
    }

    @Test
    fun `text pattern validation passes for valid input`() {
        val form = formWith(
            TextFieldElement(
                id = "email", label = "Email",
                validation = TextValidation(
                    pattern = "^[^@]+@[^@]+\\.[^@]+$",
                    errorMessage = "Enter a valid email"
                )
            )
        )
        val errors = validator.validate(form, mapOf("email" to "test@example.com"))
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `number min validation`() {
        val form = formWith(
            NumberFieldElement(
                id = "age", label = "Age", required = true,
                validation = NumberValidation(min = 18.0)
            )
        )
        val errors = validator.validate(form, mapOf("age" to "10"))
        assertEquals("Minimum value is 18.0", errors["age"])
    }

    @Test
    fun `number max validation`() {
        val form = formWith(
            NumberFieldElement(
                id = "age", label = "Age",
                validation = NumberValidation(max = 120.0)
            )
        )
        val errors = validator.validate(form, mapOf("age" to "200"))
        assertEquals("Maximum value is 120.0", errors["age"])
    }

    @Test
    fun `number non-numeric value returns error`() {
        val form = formWith(
            NumberFieldElement(id = "age", label = "Age")
        )
        val errors = validator.validate(form, mapOf("age" to "abc"))
        assertEquals("Must be a number", errors["age"])
    }

    @Test
    fun `selection minSelections validation`() {
        val form = formWith(
            MultiSelectElement(
                id = "skills", label = "Skills",
                validation = SelectionValidation(minSelections = 2)
            )
        )
        val errors = validator.validate(form, mapOf("skills" to "kotlin"))
        assertEquals("Select at least 2", errors["skills"])
    }

    @Test
    fun `selection maxSelections validation`() {
        val form = formWith(
            MultiSelectElement(
                id = "skills", label = "Skills",
                validation = SelectionValidation(maxSelections = 2)
            )
        )
        val errors = validator.validate(form, mapOf("skills" to "kotlin,java,python"))
        assertEquals("Select at most 2", errors["skills"])
    }

    @Test
    fun `hidden field skips validation`() {
        val form = formWith(
            TextFieldElement(
                id = "company", label = "Company", required = true,
                visibleWhen = VisibilityCondition(
                    fieldId = "role",
                    operator = ConditionOperator.EQUALS,
                    value = JsonPrimitive("dev")
                )
            )
        )
        val errors = validator.validate(form, mapOf("role" to "pm"))
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `visible conditional field is validated`() {
        val form = formWith(
            TextFieldElement(
                id = "company", label = "Company", required = true,
                visibleWhen = VisibilityCondition(
                    fieldId = "role",
                    operator = ConditionOperator.EQUALS,
                    value = JsonPrimitive("dev")
                )
            )
        )
        val errors = validator.validate(form, mapOf("role" to "dev"))
        assertEquals("Company is required", errors["company"])
    }
}
