package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.*
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EvaluateVisibilityUseCaseTest {

    private lateinit var useCase: EvaluateVisibilityUseCase

    @Before
    fun setup() {
        useCase = EvaluateVisibilityUseCase()
    }

    @Test
    fun `element without visibleWhen is always visible`() {
        val element = TextFieldElement(id = "name", label = "Name")
        assertTrue(useCase.isVisible(element, emptyMap()))
    }

    @Test
    fun `equals operator matches field value`() {
        val element = TextFieldElement(
            id = "company",
            label = "Company",
            visibleWhen = VisibilityCondition("role", ConditionOperator.EQUALS, JsonPrimitive("dev"))
        )
        assertTrue(useCase.isVisible(element, mapOf("role" to "dev")))
        assertFalse(useCase.isVisible(element, mapOf("role" to "design")))
    }

    @Test
    fun `not_equals operator works`() {
        val element = TextFieldElement(
            id = "other",
            label = "Other",
            visibleWhen = VisibilityCondition("role", ConditionOperator.NOT_EQUALS, JsonPrimitive("admin"))
        )
        assertTrue(useCase.isVisible(element, mapOf("role" to "dev")))
        assertFalse(useCase.isVisible(element, mapOf("role" to "admin")))
    }

    @Test
    fun `greater_than operator compares numbers`() {
        val element = TextFieldElement(
            id = "senior_info",
            label = "Senior Info",
            visibleWhen = VisibilityCondition("experience", ConditionOperator.GREATER_THAN, JsonPrimitive(5))
        )
        assertTrue(useCase.isVisible(element, mapOf("experience" to "10")))
        assertFalse(useCase.isVisible(element, mapOf("experience" to "3")))
        assertFalse(useCase.isVisible(element, mapOf("experience" to "5")))
    }

    @Test
    fun `is_empty and is_not_empty operators work`() {
        val emptyCheck = TextFieldElement(
            id = "a", label = "A",
            visibleWhen = VisibilityCondition("field", ConditionOperator.IS_EMPTY, JsonPrimitive(""))
        )
        assertTrue(useCase.isVisible(emptyCheck, mapOf("field" to "")))
        assertFalse(useCase.isVisible(emptyCheck, mapOf("field" to "x")))

        val notEmptyCheck = TextFieldElement(
            id = "b", label = "B",
            visibleWhen = VisibilityCondition("field", ConditionOperator.IS_NOT_EMPTY, JsonPrimitive(""))
        )
        assertTrue(useCase.isVisible(notEmptyCheck, mapOf("field" to "x")))
        assertFalse(useCase.isVisible(notEmptyCheck, mapOf("field" to "")))
    }

    @Test
    fun `missing field treated as empty string`() {
        val element = TextFieldElement(
            id = "x", label = "X",
            visibleWhen = VisibilityCondition("missing", ConditionOperator.EQUALS, JsonPrimitive("val"))
        )
        assertFalse(useCase.isVisible(element, emptyMap()))
    }

    @Test
    fun `getVisibleElements filters page elements`() {
        val page = Page(
            pageId = "p1",
            title = "Page 1",
            elements = listOf(
                TextFieldElement(id = "always", label = "Always"),
                TextFieldElement(
                    id = "conditional", label = "Conditional",
                    visibleWhen = VisibilityCondition("toggle", ConditionOperator.EQUALS, JsonPrimitive("true"))
                )
            )
        )
        val visible = useCase.getVisibleElements(page, mapOf("toggle" to "false"))
        assertEquals(1, visible.size)
        assertEquals("always", visible[0].id)
    }
}
