package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.*
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ValidatePageUseCaseTest {

    private lateinit var useCase: ValidatePageUseCase

    @Before
    fun setup() {
        useCase = ValidatePageUseCase(EvaluateVisibilityUseCase())
    }

    @Test
    fun `required field with empty value returns error`() {
        val page = Page("p1", "Page", listOf(
            TextFieldElement(id = "name", label = "Name", required = true)
        ))
        val errors = useCase.validate(page, emptyMap())
        assertEquals("Name is required", errors["name"])
    }

    @Test
    fun `optional empty field returns no error`() {
        val page = Page("p1", "Page", listOf(
            TextFieldElement(id = "bio", label = "Bio", required = false)
        ))
        val errors = useCase.validate(page, emptyMap())
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `text minLength validation`() {
        val page = Page("p1", "Page", listOf(
            TextFieldElement(
                id = "name", label = "Name",
                validation = TextValidation(minLength = 3, errorMessage = "Too short")
            )
        ))
        val errors = useCase.validate(page, mapOf("name" to "ab"))
        assertEquals("Too short", errors["name"])

        val noErrors = useCase.validate(page, mapOf("name" to "abc"))
        assertTrue(noErrors.isEmpty())
    }

    @Test
    fun `number range validation`() {
        val page = Page("p1", "Page", listOf(
            NumberFieldElement(
                id = "age", label = "Age",
                validation = NumberValidation(min = 18.0, max = 120.0)
            )
        ))
        assertNotNull(useCase.validate(page, mapOf("age" to "10"))["age"])
        assertTrue(useCase.validate(page, mapOf("age" to "25")).isEmpty())
        assertNotNull(useCase.validate(page, mapOf("age" to "abc"))["age"])
    }

    @Test
    fun `hidden field is not validated`() {
        val page = Page("p1", "Page", listOf(
            TextFieldElement(
                id = "company", label = "Company", required = true,
                visibleWhen = VisibilityCondition("role", ConditionOperator.EQUALS, JsonPrimitive("dev"))
            )
        ))
        val errors = useCase.validate(page, mapOf("role" to "design"))
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `firstPageWithErrors returns correct index`() {
        val pages = listOf(
            Page("p1", "Page 1", listOf(TextFieldElement(id = "a", label = "A"))),
            Page("p2", "Page 2", listOf(TextFieldElement(id = "b", label = "B")))
        )
        assertEquals(1, useCase.firstPageWithErrors(pages, mapOf("b" to "error")))
        assertEquals(0, useCase.firstPageWithErrors(pages, mapOf("a" to "error")))
    }
}
