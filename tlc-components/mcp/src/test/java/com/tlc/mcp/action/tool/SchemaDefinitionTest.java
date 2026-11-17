package com.tlc.mcp.action.tool;

import com.tlc.commons.json.JsonObject;
import com.tlc.mcp.tool.SchemaDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchemaDefinitionTest {


    @Test
    void empty_hasNoFields() {
        assertTrue(SchemaDefinition.empty().fields().isEmpty());
    }

    @Test
    void of_preservesFieldOrder() {
        final SchemaDefinition.FieldDef a = field("a", "string");
        final SchemaDefinition.FieldDef b = field("b", "integer");
        final SchemaDefinition schema = SchemaDefinition.of(List.of(a, b));
        assertEquals(List.of(a, b), schema.fields());
    }

    @Test
    void field_returnsNamedField() {
        final SchemaDefinition.FieldDef f = field("query", "string");
        final SchemaDefinition schema = SchemaDefinition.of(List.of(f));
        assertTrue(schema.field("query").isPresent());
        assertFalse(schema.field("missing").isPresent());
    }


    @Test
    void withField_addsNewField() {
        final SchemaDefinition schema = SchemaDefinition.empty()
                .withField(field("x", "string"));
        assertEquals(1, schema.fields().size());
    }

    @Test
    void withField_replacesExistingName() {
        final SchemaDefinition.FieldDef old = field("x", "string");
        final SchemaDefinition.FieldDef replacement = field("x", "integer");
        final SchemaDefinition schema = SchemaDefinition.of(List.of(old))
                .withField(replacement);
        assertEquals(1, schema.fields().size());
        assertEquals("integer", schema.fields().get(0).fieldType());
    }

    @Test
    void withoutField_removesField() {
        final SchemaDefinition schema = SchemaDefinition.of(List.of(field("a", "string"), field("b", "integer")))
                .withoutField("a");
        assertEquals(1, schema.fields().size());
        assertEquals("b", schema.fields().get(0).name());
    }


    @Test
    void fieldDefBuilder_defaults() {
        final SchemaDefinition.FieldDef f = SchemaDefinition.FieldDef.builder("x").build();
        assertEquals("x", f.name());
        assertEquals("string", f.fieldType());
        assertEquals("", f.description());
        assertTrue(f.required());
        assertEquals("", f.defaultValue());
        assertEquals("", f.min());
        assertEquals("", f.max());
        assertEquals(-1, f.minLength());
        assertEquals(-1, f.maxLength());
        assertEquals("", f.pattern());
        assertTrue(f.options().isEmpty());
        assertFalse(f.multiSelect());
        assertTrue(f.nestedFields().isEmpty());
        assertNull(f.itemType());
    }

    @Test
    void fieldDefBuilder_allAttributes() {
        final SchemaDefinition.FieldDef f = SchemaDefinition.FieldDef.builder("limit")
                .type("integer")
                .description("Max results")
                .required(false)
                .defaultValue("10")
                .min("1")
                .max("100")
                .minLength(0)
                .maxLength(10)
                .pattern("\\d+")
                .options(List.of("a", "b"))
                .multiSelect(true)
                .itemType("string")
                .build();

        assertEquals("limit", f.name());
        assertEquals("integer", f.fieldType());
        assertEquals("Max results", f.description());
        assertFalse(f.required());
        assertEquals("10", f.defaultValue());
        assertEquals("1", f.min());
        assertEquals("100", f.max());
        assertEquals(0, f.minLength());
        assertEquals(10, f.maxLength());
        assertEquals("\\d+", f.pattern());
        assertEquals(List.of("a", "b"), f.options());
        assertTrue(f.multiSelect());
        assertEquals("string", f.itemType());
    }


    @Test
    void toJsonSchema_stringField() {
        final SchemaDefinition schema = SchemaDefinition.of(List.of(
                SchemaDefinition.FieldDef.builder("name").type("string")
                        .description("Full name").required(true).build()
        ));
        final JsonObject json = schema.toJsonSchema();
        assertEquals("object", json.getString("type"));
        final JsonObject props = json.getJsonObject("properties");
        assertTrue(props.containsKey("name"));
        final JsonObject nameProp = props.getJsonObject("name");
        assertEquals("string", nameProp.getString("type"));
        assertEquals("Full name", nameProp.getString("description"));
        assertTrue(json.getJsonArray("required").toList().contains("name"));
    }

    @Test
    void toJsonSchema_integerField() {
        final SchemaDefinition schema = SchemaDefinition.of(List.of(
                SchemaDefinition.FieldDef.builder("count").type("integer")
                        .min("0").max("1000").defaultValue("50").required(false).build()
        ));
        final JsonObject prop = schema.toJsonSchema().getJsonObject("properties").getJsonObject("count");
        assertEquals("integer", prop.getString("type"));
        assertNotNull(prop.opt("minimum"));
        assertNotNull(prop.opt("maximum"));
        assertEquals("50", prop.getString("default"));
        assertNull(schema.toJsonSchema().opt("required"));
    }

    @Test
    void toJsonSchema_enumField() {
        final SchemaDefinition schema = SchemaDefinition.of(List.of(
                SchemaDefinition.FieldDef.builder("status").type("string")
                        .options(List.of("ACTIVE", "INACTIVE")).build()
        ));
        final JsonObject prop = schema.toJsonSchema().getJsonObject("properties").getJsonObject("status");
        assertEquals(List.of("ACTIVE", "INACTIVE"), prop.getJsonArray("enum").toList());
    }

    @Test
    void toJsonSchema_multiSelectField() {
        final SchemaDefinition schema = SchemaDefinition.of(List.of(
                SchemaDefinition.FieldDef.builder("tags").type("array")
                        .options(List.of("X", "Y")).multiSelect(true).build()
        ));
        final JsonObject prop = schema.toJsonSchema().getJsonObject("properties").getJsonObject("tags");
        assertEquals("array", prop.getString("type"));
        assertTrue(prop.getBoolean("uniqueItems"));
        final JsonObject items = prop.getJsonObject("items");
        assertEquals(List.of("X", "Y"), items.getJsonArray("enum").toList());
    }

    @Test
    void toJsonSchema_typedArrayField() {
        final SchemaDefinition schema = SchemaDefinition.of(List.of(
                SchemaDefinition.FieldDef.builder("ids").type("array")
                        .itemType("integer").build()
        ));
        final JsonObject prop = schema.toJsonSchema().getJsonObject("properties").getJsonObject("ids");
        assertEquals("array", prop.getString("type"));
        final JsonObject items = prop.getJsonObject("items");
        assertEquals("integer", items.getString("type"));
    }

    @Test
    void toJsonSchema_nestedObjectField() {
        final SchemaDefinition.FieldDef inner = SchemaDefinition.FieldDef.builder("street")
                .type("string").required(true).build();
        final SchemaDefinition.FieldDef outer = SchemaDefinition.FieldDef.builder("address")
                .type("object").nestedFields(List.of(inner)).required(true).build();
        final SchemaDefinition schema = SchemaDefinition.of(List.of(outer));

        final JsonObject addrProp = schema.toJsonSchema().getJsonObject("properties").getJsonObject("address");
        assertEquals("object", addrProp.getString("type"));
        final JsonObject nested = addrProp.getJsonObject("properties");
        assertTrue(nested.containsKey("street"));
    }

    @Test
    void toJsonSchema_arrayOfNestedObjectsField() {
        final SchemaDefinition.FieldDef value = SchemaDefinition.FieldDef.builder("value")
                .type("string").required(true).build();
        final SchemaDefinition.FieldDef valueEntityId = SchemaDefinition.FieldDef.builder("valueEntityId")
                .type("integer").required(false).build();

        final SchemaDefinition.FieldDef values = SchemaDefinition.FieldDef.builder("values")
                .type("array")
                .itemType("object")
                .nestedFields(List.of(value, valueEntityId))
                .required(false)
                .build();

        final SchemaDefinition schema = SchemaDefinition.of(List.of(values));
        final JsonObject valuesProp = schema.toJsonSchema().getJsonObject("properties").getJsonObject("values");

        assertEquals("array", valuesProp.getString("type"));
        final JsonObject items = valuesProp.getJsonObject("items");
        assertEquals("object", items.getString("type"));

        final JsonObject nested = items.getJsonObject("properties");
        assertTrue(nested.containsKey("value"));
        assertTrue(nested.containsKey("valueEntityId"));

        final List<Object> required = items.getJsonArray("required").toList();
        assertTrue(required.contains("value"));
        assertFalse(required.contains("valueEntityId"));
    }

    @Test
    void toJsonSchema_stringConstraints() {
        final SchemaDefinition schema = SchemaDefinition.of(List.of(
                SchemaDefinition.FieldDef.builder("code").type("string")
                        .minLength(2).maxLength(5).pattern("^[A-Z]+$").build()
        ));
        final JsonObject prop = schema.toJsonSchema().getJsonObject("properties").getJsonObject("code");
        assertEquals(2, prop.getInt("minLength"));
        assertEquals(5, prop.getInt("maxLength"));
        assertEquals("^[A-Z]+$", prop.getString("pattern"));
    }

    @Test
    void toJsonSchema_additionalPropertiesFalse() {
        final JsonObject json = SchemaDefinition.empty().toJsonSchema();
        assertFalse(json.getBoolean("additionalProperties"));
    }

    @Test
    void toJsonSchema_requiredPropagatedForMandatoryFields() {
        final SchemaDefinition schema = SchemaDefinition.of(List.of(
                SchemaDefinition.FieldDef.builder("req").type("string").required(true).build(),
                SchemaDefinition.FieldDef.builder("opt").type("string").required(false).build()
        ));
        final List<Object> required = schema.toJsonSchema().getJsonArray("required").toList();
        assertTrue(required.contains("req"));
        assertFalse(required.contains("opt"));
    }

    @Test
    void toJsonSchema_fieldWithDefaultNotRequired() {
        final SchemaDefinition schema = SchemaDefinition.of(List.of(
                SchemaDefinition.FieldDef.builder("limit").type("integer")
                        .required(true).defaultValue("10").build()
        ));
        assertNull(schema.toJsonSchema().opt("required"));
    }


    private static SchemaDefinition.FieldDef field(String name, String type) {
        return SchemaDefinition.FieldDef.builder(name).type(type).build();
    }
}
