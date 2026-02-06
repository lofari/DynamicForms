package com.lfr.dynamicforms.domain.model

import org.junit.Assert.*
import org.junit.Test

class FormElementDefaultValueTest {

    @Test
    fun `toggle defaultValue true returns true`() {
        val element = ToggleElement(id = "t1", label = "Toggle", defaultValue = true)
        assertEquals("true", element.getDefaultValue())
    }

    @Test
    fun `toggle defaultValue false returns false`() {
        val element = ToggleElement(id = "t2", label = "Toggle", defaultValue = false)
        assertEquals("false", element.getDefaultValue())
    }

    @Test
    fun `checkbox defaultValue true returns true`() {
        val element = CheckboxElement(id = "c1", label = "Checkbox", defaultValue = true)
        assertEquals("true", element.getDefaultValue())
    }

    @Test
    fun `checkbox defaultValue false returns false`() {
        val element = CheckboxElement(id = "c2", label = "Checkbox", defaultValue = false)
        assertEquals("false", element.getDefaultValue())
    }

    @Test
    fun `slider with min 5 returns 5 dot 0`() {
        val element = SliderElement(id = "s1", label = "Slider", min = 5f, max = 100f)
        assertEquals("5.0", element.getDefaultValue())
    }

    @Test
    fun `non-defaultable elements return null`() {
        val textField = TextFieldElement(id = "tf", label = "Text")
        assertNull(textField.getDefaultValue())

        val dropdown = DropdownElement(id = "dd", label = "Dropdown")
        assertNull(dropdown.getDefaultValue())

        val radio = RadioElement(id = "r", label = "Radio")
        assertNull(radio.getDefaultValue())

        val numberField = NumberFieldElement(id = "nf", label = "Number")
        assertNull(numberField.getDefaultValue())
    }
}
