//package com.tlc.mcp.action.tool;
//
//import com.tlc.mcp.tool.AbstractMcpTool;
//import com.tlc.mcp.tool.McpToolContext;
//import com.tlc.mcp.tool.McpToolResult;
//import com.tlc.mcp.tool.SchemaDefinition;
//import org.junit.jupiter.api.Test;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class AbstractMcpToolTest {
//
//    private static McpToolContext ctx(Object identity) {
//        return new McpToolContext<>("test-tool", "1", identity);
//    }
//
//    private static SchemaDefinition schema(String... fieldNames) {
//        final List<SchemaDefinition.FieldDef> fields = java.util.Arrays.stream(fieldNames)
//                .map(n -> SchemaDefinition.FieldDef.builder(n).build())
//                .toList();
//        return SchemaDefinition.of(fields);
//    }
//
//
//    @Test
//    void isVisible_default_returnsTrue() {
//        final AbstractMcpTool<Object> tool = new PlainTool();
//        assertTrue(tool.isVisible(ctx(null)));
//        assertTrue(tool.isVisible(ctx("some-user")));
//    }
//
//    @Test
//    void inputSchema_default_returnsGenerated() {
//        final AbstractMcpTool<Object> tool = new PlainTool();
//        final SchemaDefinition generated = schema("query", "limit");
//        assertSame(generated, tool.inputSchema(generated, ctx(null)));
//        assertSame(generated, tool.inputSchema(generated, ctx("user-x")));
//    }
//
//    @Test
//    void isAuthorized_default_returnsTrue() {
//        final AbstractMcpTool<Object> tool = new PlainTool();
//        assertTrue(tool.isAuthorized(ctx(null)));
//        assertTrue(tool.isAuthorized(ctx("admin")));
//    }
//
//    @Test
//    void isVisible_override_filtersAnonymous() {
//        final AbstractMcpTool<Object> tool = new AdminOnlyTool();
//        assertFalse(tool.isVisible(ctx(null)));
//        assertTrue(tool.isVisible(ctx("admin-user")));
//    }
//
//    @Test
//    void inputSchema_override_hidesFieldForAnonymous() {
//        final AbstractMcpTool<Object> tool = new SensitiveTool();
//        final SchemaDefinition base = schema("query", "sensitiveField");
//
//        final SchemaDefinition anonSchema = tool.inputSchema(base, ctx(null));
//        assertTrue(anonSchema.field("sensitiveField").isEmpty());
//        assertTrue(anonSchema.field("query").isPresent());
//
//        final SchemaDefinition userSchema = tool.inputSchema(base, ctx("authenticated-user"));
//        assertTrue(userSchema.field("sensitiveField").isPresent());
//        assertTrue(userSchema.field("query").isPresent());
//    }
//
//    @Test
//    void inputSchema_override_addsFieldForAdmin() {
//        final AbstractMcpTool<Object> tool = new AdminFieldTool();
//        final SchemaDefinition base = schema("query");
//
//        final SchemaDefinition adminSchema = tool.inputSchema(base, ctx("admin"));
//        assertTrue(adminSchema.field("adminParam").isPresent());
//
//        final SchemaDefinition userSchema = tool.inputSchema(base, ctx("regular"));
//        assertTrue(userSchema.field("adminParam").isEmpty());
//    }
//
//    @Test
//    void unauthorized_returnsErrorResult() {
//        final AbstractMcpTool<Object> tool = new PlainTool();
//        final McpToolResult result = tool.unauthorized();
//        assertTrue(result.isError());
//        assertFalse(result.content().isEmpty());
//        assertEquals("Unauthorized", result.content().get(0).data());
//    }
//
//
//    static class PlainTool extends AbstractMcpTool<Object> {
//        public McpToolResult execute() { return McpToolResult.text("ok"); }
//    }
//
//    static class AdminOnlyTool extends AbstractMcpTool<Object> {
//        @Override
//        public boolean isVisible(McpToolContext<Object> ctx) {
//            return ctx.identity() != null;
//        }
//        public McpToolResult execute() { return McpToolResult.text("ok"); }
//    }
//
//    static class SensitiveTool extends AbstractMcpTool<Object> {
//        @Override
//        public SchemaDefinition inputSchema(SchemaDefinition generated, McpToolContext<Object> ctx) {
//            if (ctx.identity() == null) {
//                return generated.withoutField("sensitiveField");
//            }
//            return generated;
//        }
//        public McpToolResult execute() { return McpToolResult.text("ok"); }
//    }
//
//    static class AdminFieldTool extends AbstractMcpTool<Object> {
//        @Override
//        public SchemaDefinition inputSchema(SchemaDefinition generated, McpToolContext<Object> ctx) {
//            if ("admin".equals(ctx.identity())) {
//                return generated.withField(SchemaDefinition.FieldDef.builder("adminParam")
//                        .description("Admin-only parameter").required(false).build());
//            }
//            return generated;
//        }
//        public McpToolResult execute() { return McpToolResult.text("ok"); }
//    }
//}
