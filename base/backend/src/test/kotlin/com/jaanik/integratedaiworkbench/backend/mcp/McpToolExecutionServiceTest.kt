package com.jaanik.integratedaiworkbench.backend.mcp

import com.jaanik.integratedaiworkbench.shared.model.ToolCall
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class McpToolExecutionServiceTest {
    @Test fun `discovers only permitted read-only tools and dispatches a verified call`() {
        val client = FakeClient()
        val service = McpToolExecutionService({ client }, expectedProjectModule = "integrated-ai-workbench")

        assertEquals(listOf("get_project_modules"), service.discover(setOf("get_project_modules")).map { it.id })
        val decision = service.execute(listOf(ToolCall("get_project_modules")), setOf("get_project_modules"), emptySet())

        val completed = assertIs<ToolExecutionDecision.Completed>(decision)
        assertTrue(completed.result.contains("integrated-ai-workbench"))
        assertEquals(listOf("get_project_modules", "get_project_modules"), client.calls)
    }

    @Test fun `rejects a schema-invalid call before execution`() {
        val client = FakeClient()
        val service = McpToolExecutionService({ client }, expectedProjectModule = "integrated-ai-workbench")

        val decision = service.execute(listOf(ToolCall("read_file", mapOf("unknown" to "x"))), setOf("read_file"), emptySet())

        assertEquals("Tool arguments are invalid.", assertIs<ToolExecutionDecision.Rejected>(decision).reason)
        assertEquals(listOf("get_project_modules"), client.calls)
    }

    private class FakeClient : McpClient {
        val calls = mutableListOf<String>()
        override fun discoverTools(): List<McpToolDefinition> = listOf(
            tool("get_project_modules", emptyMap()),
            tool("read_file", mapOf("path" to "string")),
            tool("write_file", mapOf("path" to "string")),
        )
        override fun call(toolId: String, arguments: Map<String, String>): McpToolResult {
            calls += toolId
            return when (toolId) {
                "get_project_modules" -> McpToolResult("integrated-ai-workbench", false)
                else -> McpToolResult("ok", false)
            }
        }
        override fun close() = Unit
        private fun tool(id: String, properties: Map<String, String>): McpToolDefinition = McpToolDefinition(
            id, id, JsonValue.ObjectValue(linkedMapOf(
                "type" to JsonValue.StringValue("object"),
                "properties" to JsonValue.ObjectValue(properties.mapValues<String, String, JsonValue> { JsonValue.ObjectValue(mapOf("type" to JsonValue.StringValue(it.value))) }),
                "additionalProperties" to JsonValue.BooleanValue(false),
            )),
        )
    }
}
