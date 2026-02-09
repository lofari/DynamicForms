package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.*
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EvaluateVisibilityUseCaseAdditionalTest {

    private lateinit var useCase: EvaluateVisibilityUseCase

    @Before
    fun setup() {
        useCase = EvaluateVisibilityUseCase()
    }

    @Test
    fun `less_than operator shows element when field is less`() {
        val element = TextFieldElement(
            id = "info", label = "Info",
            visibleWhen = VisibilityCondition("score", ConditionOperator.LESS_THAN, JsonPrimitive("5"))
        )
        val page = Page("p1", "Page", listOf(element))
        val visible = useCase.getVisibleElements(page, mapOf("score" to "3"))
        assertTrue(visible.contains(element))
    }

    @Test
    fun `less_than operator hides element when field is greater or equal`() {
        val element = TextFieldElement(
            id = "info", label = "Info",
            visibleWhen = VisibilityCondition("score", ConditionOperator.LESS_THAN, JsonPrimitive("5"))
        )
        val page = Page("p1", "Page", listOf(element))

        val visibleWhenGreater = useCase.getVisibleElements(page, mapOf("score" to "7"))
        assertFalse(visibleWhenGreater.contains(element))

        val visibleWhenEqual = useCase.getVisibleElements(page, mapOf("score" to "5"))
        assertFalse(visibleWhenEqual.contains(element))
    }

    @Test
    fun `contains operator matches case-insensitively`() {
        val element = TextFieldElement(
            id = "detail", label = "Detail",
            visibleWhen = VisibilityCondition("bio", ConditionOperator.CONTAINS, JsonPrimitive("hello"))
        )
        val page = Page("p1", "Page", listOf(element))
        val visible = useCase.getVisibleElements(page, mapOf("bio" to "Hello World"))
        assertTrue(visible.contains(element))
    }

    @Test
    fun `contains operator returns false on no match`() {
        val element = TextFieldElement(
            id = "detail", label = "Detail",
            visibleWhen = VisibilityCondition("bio", ConditionOperator.CONTAINS, JsonPrimitive("xyz"))
        )
        val page = Page("p1", "Page", listOf(element))
        val visible = useCase.getVisibleElements(page, mapOf("bio" to "Hello"))
        assertFalse(visible.contains(element))
    }

    @Test
    fun `greater_than with non-numeric field value returns false`() {
        val element = TextFieldElement(
            id = "info", label = "Info",
            visibleWhen = VisibilityCondition("score", ConditionOperator.GREATER_THAN, JsonPrimitive("5"))
        )
        val page = Page("p1", "Page", listOf(element))
        val visible = useCase.getVisibleElements(page, mapOf("score" to "abc"))
        assertFalse(visible.contains(element))
    }
}
