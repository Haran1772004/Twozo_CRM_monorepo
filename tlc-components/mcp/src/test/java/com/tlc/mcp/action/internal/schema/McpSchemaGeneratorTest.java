package com.tlc.mcp.action.internal.schema;

import com.tlc.mcp.internal.registry.ParameterSpec;
import com.tlc.mcp.internal.schema.McpSchemaGenerator;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;
import com.tlc.mcp.tool.SchemaDefinition;
import com.tlc.mcp.tool.ToolParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McpSchemaGeneratorTest {

    private McpSchemaGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new McpSchemaGenerator();
    }


    @Test
    void stringParam_mapsToStringType() throws Exception {
        final SchemaDefinition schema = generate(Tools.StringTool.class);
        assertEquals("string", fieldType(schema, "query"));
    }

    @Test
    void intParam_mapsToIntegerType() throws Exception {
        final SchemaDefinition schema = generate(Tools.IntTool.class);
        assertEquals("string", fieldType(schema, "query"));
        assertEquals("integer", fieldType(schema, "limit"));
    }

    @Test
    void longParam_mapsToIntegerType() throws Exception {
        final SchemaDefinition schema = generate(Tools.LongTool.class);
        assertEquals("integer", fieldType(schema, "id"));
    }

    @Test
    void doubleParam_mapsToNumberType() throws Exception {
        final SchemaDefinition schema = generate(Tools.DoubleTool.class);
        assertEquals("number", fieldType(schema, "score"));
    }

    @Test
    void booleanParam_mapsToBooleanType() throws Exception {
        final SchemaDefinition schema = generate(Tools.BoolTool.class);
        assertEquals("boolean", fieldType(schema, "active"));
    }


    @Test
    void enumParam_mapsToStringWithEnumValues() throws Exception {
        final SchemaDefinition schema = generate(Tools.EnumTool.class);
        final SchemaDefinition.FieldDef f = schema.field("status").orElseThrow();
        assertEquals("string", f.fieldType());
        assertTrue(f.options().containsAll(List.of("ACTIVE", "INACTIVE")));
    }


    @Test
    void listParam_mapsToArrayWithItemType() throws Exception {
        final SchemaDefinition schema = generate(Tools.ListTool.class);
        final SchemaDefinition.FieldDef f = schema.field("ids").orElseThrow();
        assertEquals("array", f.fieldType());
        assertEquals("integer", f.itemType());
    }

    @Test
    void arrayParam_mapsToArrayWithItemType() throws Exception {
        final SchemaDefinition schema = generate(Tools.ArrayTool.class);
        final SchemaDefinition.FieldDef f = schema.field("tags").orElseThrow();
        assertEquals("array", f.fieldType());
        assertEquals("string", f.itemType());
    }


    @Test
    void pojoParam_mapsToObjectWithNestedFields() throws Exception {
        final SchemaDefinition schema = generate(Tools.PojoTool.class);
        final SchemaDefinition.FieldDef f = schema.field("request").orElseThrow();
        assertEquals("object", f.fieldType());
        assertFalse(f.nestedFields().isEmpty());
        assertTrue(f.nestedFields().stream().anyMatch(nf -> "query".equals(nf.name())));
    }

    @Test
    void nestedPojoInPojo_reflected() throws Exception {
        final SchemaDefinition schema = generate(Tools.DeepPojoTool.class);
        final SchemaDefinition.FieldDef outer = schema.field("outer").orElseThrow();
        assertEquals("object", outer.fieldType());
        assertTrue(outer.nestedFields().stream().anyMatch(nf -> "inner".equals(nf.name())));
        outer.nestedFields().stream()
                .filter(nf -> "inner".equals(nf.name()))
                .findFirst()
                .ifPresent(inner -> assertEquals("object", inner.fieldType()));
    }


    @Test
    void mcpToolContextParam_excludedFromSchema() throws Exception {
        final SchemaDefinition schema = generate(Tools.ContextTool.class);
        assertTrue(schema.field("ctx").isEmpty(), "McpToolContext should not appear in schema");
        assertTrue(schema.field("name").isPresent());
        assertEquals(1, schema.fields().size());
    }

    @Test
    void mapCatchAllParam_excludedFromSchema() throws Exception {
        final SchemaDefinition schema = generate(Tools.CatchAllTool.class);
        assertTrue(schema.field("extra").isEmpty(), "Catch-all map should not appear in schema");
        assertTrue(schema.field("name").isPresent());
        assertEquals(1, schema.fields().size());
    }


    @Test
    void circularReference_throwsIllegalArgumentException() throws Exception {
        final Method method = Tools.CircularTool.class.getMethod("execute", Tools.CircularA.class);
        final List<ParameterSpec> specs = List.of(
                new ParameterSpec("a", Tools.CircularA.class, false, false)
        );
        assertThrows(IllegalArgumentException.class, () -> generator.generate(method, specs));
    }


    @Test
    void toolParamAnnotation_description_propagated() throws Exception {
        final SchemaDefinition schema = generate(Tools.AnnotatedTool.class);
        final SchemaDefinition.FieldDef f = schema.field("query").orElseThrow();
        assertEquals("Search query", f.description());
    }

    @Test
    void toolParamAnnotation_defaultValue_marksOptional() throws Exception {
        final SchemaDefinition schema = generate(Tools.AnnotatedTool.class);
        final SchemaDefinition.FieldDef f = schema.field("limit").orElseThrow();
        assertEquals("10", f.defaultValue());
    }


    private SchemaDefinition generate(Class<?> toolClass) throws Exception {
        final Method method = findExecute(toolClass);
        final List<ParameterSpec> specs = buildSpecs(method);
        return generator.generate(method, specs);
    }

    private static Method findExecute(Class<?> cls) {
        for (Method m : cls.getMethods()) {
            if ("execute".equals(m.getName())) return m;
        }
        throw new IllegalStateException("No execute() in " + cls.getSimpleName());
    }

    private static List<ParameterSpec> buildSpecs(Method method) {
        return java.util.Arrays.stream(method.getParameters())
                .map(p -> {
                    final boolean isCtx = McpToolContext.class.isAssignableFrom(p.getType());
                    final boolean isCatchAll = Map.class.isAssignableFrom(p.getType())
                            && p.getAnnotation(ToolParam.class) == null;
                    final ToolParam ann = p.getAnnotation(ToolParam.class);
                    final String name = (ann != null && !ann.name().isEmpty())
                            ? ann.name() : p.getName();
                    return new ParameterSpec(name, p.getType(), isCtx, isCatchAll);
                })
                .toList();
    }

    private static String fieldType(SchemaDefinition schema, String name) {
        return schema.field(name).orElseThrow(() ->
                new AssertionError("Field '" + name + "' not found in schema")).fieldType();
    }


    static class Tools {

        static class StringTool {
            public McpToolResult execute(@ToolParam(description = "q") String query) { return null; }
        }

        static class IntTool {
            public McpToolResult execute(
                    @ToolParam(description = "q") String query,
                    @ToolParam(description = "l") int limit) { return null; }
        }

        static class LongTool {
            public McpToolResult execute(@ToolParam(description = "id") long id) { return null; }
        }

        static class DoubleTool {
            public McpToolResult execute(@ToolParam(description = "s") double score) { return null; }
        }

        static class BoolTool {
            public McpToolResult execute(@ToolParam(description = "a") boolean active) { return null; }
        }

        enum Status { ACTIVE, INACTIVE }

        static class EnumTool {
            public McpToolResult execute(@ToolParam(description = "s") Status status) { return null; }
        }

        static class ListTool {
            public McpToolResult execute(@ToolParam(description = "ids") List<Integer> ids) { return null; }
        }

        static class ArrayTool {
            public McpToolResult execute(@ToolParam(description = "tags") String[] tags) { return null; }
        }

        static class SimpleRequest {
            @ToolParam(description = "search text")
            public String query;
        }

        static class PojoTool {
            public McpToolResult execute(@ToolParam(description = "req") SimpleRequest request) { return null; }
        }

        static class InnerPojo {
            @ToolParam(description = "value")
            public String value;
        }

        static class OuterPojo {
            @ToolParam(description = "inner")
            public InnerPojo inner;
        }

        static class DeepPojoTool {
            public McpToolResult execute(@ToolParam(description = "outer") OuterPojo outer) { return null; }
        }

        static class ContextTool {
            public McpToolResult execute(
                    @ToolParam(description = "n") String name,
                    McpToolContext<Object> ctx) { return null; }
        }

        static class CatchAllTool {
            public McpToolResult execute(
                    @ToolParam(description = "n") String name,
                    Map<String, Object> extra) { return null; }
        }

        static class CircularA {
            @ToolParam(description = "b")
            public CircularB b;
        }

        static class CircularB {
            @ToolParam(description = "a")
            public CircularA a;
        }

        static class CircularTool {
            public McpToolResult execute(CircularA a) { return null; }
        }

        static class AnnotatedTool {
            public McpToolResult execute(
                    @ToolParam(description = "Search query") String query,
                    @ToolParam(description = "Limit", defaultValue = "10") int limit) { return null; }
        }
    }
}
