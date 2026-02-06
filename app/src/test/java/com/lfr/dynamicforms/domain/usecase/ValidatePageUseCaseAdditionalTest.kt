package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ValidatePageUseCaseAdditionalTest {

    private lateinit var useCase: ValidatePageUseCase

    @Before
    fun setup() {
        useCase = ValidatePageUseCase(EvaluateVisibilityUseCase())
    }

    @Test
    fun `text maxLength validation rejects too-long value`() {
        val page = Page("p1", "Page", listOf(
            TextFieldElement(
                id = "name", label = "Name",
                validation = TextValidation(maxLength = 5)
            )
        ))
        val errors = useCase.validate(page, mapOf("name" to "123456"))
        assertEquals("Maximum 5 characters", errors["name"])
    }

    @Test
    fun `text maxLength validation accepts valid length`() {
        val page = Page("p1", "Page", listOf(
            TextFieldElement(
                id = "name", label = "Name",
                validation = TextValidation(maxLength = 5)
            )
        ))
        val errors = useCase.validate(page, mapOf("name" to "12345"))
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `text pattern validation rejects non-matching value`() {
        val page = Page("p1", "Page", listOf(
            TextFieldElement(
                id = "code", label = "Code",
                validation = TextValidation(pattern = "^[a-z]+$")
            )
        ))
        val errors = useCase.validate(page, mapOf("code" to "ABC123"))
        assertEquals("Invalid format", errors["code"])
    }

    @Test
    fun `text pattern validation accepts matching value`() {
        val page = Page("p1", "Page", listOf(
            TextFieldElement(
                id = "code", label = "Code",
                validation = TextValidation(pattern = "^[a-z]+$")
            )
        ))
        val errors = useCase.validate(page, mapOf("code" to "abc"))
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `text pattern uses custom errorMessage when provided`() {
        val page = Page("p1", "Page", listOf(
            TextFieldElement(
                id = "pin", label = "PIN",
                validation = TextValidation(pattern = "^\\d+$", errorMessage = "Numbers only")
            )
        ))
        val errors = useCase.validate(page, mapOf("pin" to "abc"))
        assertEquals("Numbers only", errors["pin"])
    }

    @Test
    fun `selection minSelections validation rejects too few`() {
        val page = Page("p1", "Page", listOf(
            MultiSelectElement(
                id = "tags", label = "Tags",
                options = listOf(SelectOption("a", "A"), SelectOption("b", "B"), SelectOption("c", "C")),
                validation = SelectionValidation(minSelections = 2)
            )
        ))
        val errors = useCase.validate(page, mapOf("tags" to "a"))
        assertEquals("Select at least 2", errors["tags"])
    }

    @Test
    fun `selection maxSelections validation rejects too many`() {
        val page = Page("p1", "Page", listOf(
            MultiSelectElement(
                id = "tags", label = "Tags",
                options = listOf(SelectOption("a", "A"), SelectOption("b", "B"), SelectOption("c", "C")),
                validation = SelectionValidation(maxSelections = 2)
            )
        ))
        val errors = useCase.validate(page, mapOf("tags" to "a,b,c"))
        assertEquals("Select at most 2", errors["tags"])
    }

    @Test
    fun `validateAllPages aggregates errors from multiple pages`() {
        val pages = listOf(
            Page("p1", "Page 1", listOf(
                TextFieldElement(id = "first", label = "First", required = true)
            )),
            Page("p2", "Page 2", listOf(
                TextFieldElement(id = "second", label = "Second", required = true)
            ))
        )
        val errors = useCase.validateAllPages(pages, emptyMap())
        assertEquals(2, errors.size)
        assertEquals("First is required", errors["first"])
        assertEquals("Second is required", errors["second"])
    }
}
