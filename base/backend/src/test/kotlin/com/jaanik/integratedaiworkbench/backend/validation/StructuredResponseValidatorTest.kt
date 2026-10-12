package com.jaanik.integratedaiworkbench.backend.validation

import com.jaanik.integratedaiworkbench.shared.model.ModelResponse
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StructuredResponseValidatorTest {
    private val validator = StructuredResponseValidator()
    private val schema = "{\"type\":\"object\",\"required\":[\"answer\"],\"properties\":{\"answer\":{\"type\":\"string\"}},\"additionalProperties\":false}"

    @Test fun `validates JSON syntax and declared field types`() {
        assertTrue(validator.validate(ModelResponse("{\"answer\":\"yes\"}"), schema).accepted)
        assertFalse(validator.validate(ModelResponse("{\"answer\":2}"), schema).accepted)
        assertFalse(validator.validate(ModelResponse("{\"other\":\"yes\"}"), schema).accepted)
        assertFalse(validator.validate(ModelResponse("{\"answer\":\"yes\",\"extra\":true}"), schema).accepted)
    }

    @Test fun `does not mistake JSON-looking content for validated structure`() {
        assertFalse(validator.validate(ModelResponse("{answer: yes}"), schema).accepted)
        assertFalse(validator.validate(ModelResponse("[\"answer\"]"), schema).accepted)
        assertFalse(validator.validate(ModelResponse("{\"answer\":\"yes\"}"), "{\"type\":\"array\"}").accepted)
    }
}
