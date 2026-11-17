package com.tlc.mcp.action.internal.schema;

import com.tlc.mcp.internal.schema.McpToolValidator;
import com.tlc.mcp.internal.schema.McpValidationException;
import com.tlc.mcp.tool.SchemaDefinition;
import com.tlc.mcp.tool.SchemaDefinition.FieldDef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McpToolValidatorTest {

    private McpToolValidator validator;

    @BeforeEach
    void setUp() {
        validator = new McpToolValidator();
    }


    @Test
    void requiredField_missing_producesError() {
        final SchemaDefinition schema = schema(
                FieldDef.builder("name").type("string").required(true).build()
        );
        final McpValidationException ex = assertThrows(McpValidationException.class,
                () -> validator.validate(Map.of(), schema));
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("name") && e.contains("required")));
    }

    @Test
    void requiredField_withDefaultValue_notEnforced() {
        final SchemaDefinition schema = schema(
                FieldDef.builder("limit").type("integer").required(true).defaultValue("10").build()
        );
        assertDoesNotThrow(() -> validator.validate(Map.of(), schema));
    }

    @Test
    void optionalField_missing_noError() {
        final SchemaDefinition schema = schema(
                FieldDef.builder("q").type("string").required(false).build()
        );
        assertDoesNotThrow(() -> validator.validate(Map.of(), schema));
    }

    @Test
    void presentField_noConstraints_valid() {
        final SchemaDefinition schema = schema(
                FieldDef.builder("name").type("string").required(true).build()
        );
        assertDoesNotThrow(() -> validator.validate(Map.of("name", "Alice"), schema));
    }


    @Test
    void minViolation_producesError() {
        final SchemaDefinition schema = schema(
                FieldDef.builder("age").type("integer").min("18").required(false).build()
        );
        final McpValidationException ex = assertThrows(McpValidationException.class,
                () -> validator.validate(Map.of("age", "10"), schema));
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("age") && e.contains(">=")));
    }

    @Test
    void maxViolation_producesError() {
        final SchemaDefinition schema = schema(
                FieldDef.builder("count").type("integer").max("100").required(false).build()
        );
        final McpValidationException ex = assertThrows(McpValidationException.class,
                () -> validator.validate(Map.of("count", "999"), schema));
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("count") && e.contains("<=")));
    }

    @Test
    void numericBoundsExact_valid() {
        final SchemaDefinition schema = schema(
                FieldDef.builder("n").type("integer").min("1").max("10").required(false).build()
        );
        assertDoesNotThrow(() -> validator.validate(Map.of("n", "10"), schema));
        assertDoesNotThrow(() -> validator.validate(Map.of("n", "1"), schema));
    }


    @Test
    void minLengthViolation_producesError() {
        final SchemaDefinition schema = schema(
                FieldDef.builder("code").type("string").minLength(3).required(false).build()
        );
        final McpValidationException ex = assertThrows(McpValidationException.class,
                () -> validator.validate(Map.of("code", "AB"), schema));
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("code") && e.contains(">=")));
    }

    @Test
    void maxLengthViolation_producesError() {
        final SchemaDefinition schema = schema(
                FieldDef.builder("code").type("string").maxLength(3).required(false).build()
        );
        final McpValidationException ex = assertThrows(McpValidationException.class,
                () -> validator.validate(Map.of("code", "ABCD"), schema));
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("code") && e.contains("<=")));
    }


    @Test
    void patternMismatch_producesError() {
        final SchemaDefinition schema = schema(
                FieldDef.builder("postcode").type("string").pattern("^[A-Z]{2}\\d{4}$").required(false).build()
        );
        final McpValidationException ex = assertThrows(McpValidationException.class,
                () -> validator.validate(Map.of("postcode", "invalid"), schema));
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("postcode") && e.contains("pattern")));
    }

    @Test
    void patternMatch_valid() {
        final SchemaDefinition schema = schema(
                FieldDef.builder("postcode").type("string").pattern("^[A-Z]{2}\\d{4}$").required(false).build()
        );
        assertDoesNotThrow(() -> validator.validate(Map.of("postcode", "AB1234"), schema));
    }


    @Test
    void singleSelectInvalidOption_producesError() {
        final SchemaDefinition schema = schema(
                FieldDef.builder("status").type("string")
                        .options(List.of("ACTIVE", "INACTIVE")).required(false).build()
        );
        final McpValidationException ex = assertThrows(McpValidationException.class,
                () -> validator.validate(Map.of("status", "DELETED"), schema));
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("status") && e.contains("one of")));
    }

    @Test
    void singleSelectValidOption_valid() {
        final SchemaDefinition schema = schema(
                FieldDef.builder("status").type("string")
                        .options(List.of("ACTIVE", "INACTIVE")).required(false).build()
        );
        assertDoesNotThrow(() -> validator.validate(Map.of("status", "ACTIVE"), schema));
    }


    @Test
    void multiSelect_notArray_producesError() {
        final SchemaDefinition schema = schema(
                FieldDef.builder("tags").type("array").multiSelect(true).required(false).build()
        );
        final McpValidationException ex = assertThrows(McpValidationException.class,
                () -> validator.validate(Map.of("tags", "single-string"), schema));
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("tags") && e.contains("array")));
    }

    @Test
    void multiSelect_invalidElement_producesError() {
        final SchemaDefinition schema = schema(
                FieldDef.builder("colors").type("array").multiSelect(true)
                        .options(List.of("RED", "GREEN", "BLUE")).required(false).build()
        );
        final McpValidationException ex = assertThrows(McpValidationException.class,
                () -> validator.validate(Map.of("colors", List.of("RED", "PURPLE")), schema));
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("colors") && e.contains("PURPLE")));
    }

    @Test
    void multiSelect_validElements_valid() {
        final SchemaDefinition schema = schema(
                FieldDef.builder("colors").type("array").multiSelect(true)
                        .options(List.of("RED", "GREEN", "BLUE")).required(false).build()
        );
        assertDoesNotThrow(() -> validator.validate(Map.of("colors", List.of("RED", "BLUE")), schema));
    }


    @Test
    void nestedObject_requiredNestedField_missing_producesError() {
        final FieldDef street = FieldDef.builder("street").type("string").required(true).build();
        final FieldDef address = FieldDef.builder("address").type("object")
                .nestedFields(List.of(street)).required(false).build();
        final SchemaDefinition schema = schema(address);
        final McpValidationException ex = assertThrows(McpValidationException.class,
                () -> validator.validate(Map.of("address", Map.of()), schema));
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("address.street") && e.contains("required")));
    }

    @Test
    void nestedObject_notAMap_producesError() {
        final FieldDef street = FieldDef.builder("street").type("string").required(true).build();
        final FieldDef address = FieldDef.builder("address").type("object")
                .nestedFields(List.of(street)).required(false).build();
        final SchemaDefinition schema = schema(address);
        final McpValidationException ex = assertThrows(McpValidationException.class,
                () -> validator.validate(Map.of("address", "not-a-map"), schema));
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("address") && e.contains("object")));
    }

    @Test
    void nestedArray_notAList_producesError() {
        final FieldDef value = FieldDef.builder("value").type("string").required(true).build();
        final FieldDef values = FieldDef.builder("values").type("array")
                .itemType("object").nestedFields(List.of(value)).required(false).build();

        final SchemaDefinition schema = schema(values);
        final McpValidationException ex = assertThrows(McpValidationException.class,
                () -> validator.validate(Map.of("values", Map.of("value", "x")), schema));
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("values") && e.contains("array")));
    }

    @Test
    void nestedArray_itemMissingRequiredField_producesIndexedError() {
        final FieldDef value = FieldDef.builder("value").type("string").required(true).build();
        final FieldDef values = FieldDef.builder("values").type("array")
                .itemType("object").nestedFields(List.of(value)).required(false).build();

        final SchemaDefinition schema = schema(values);
        final McpValidationException ex = assertThrows(McpValidationException.class,
                () -> validator.validate(Map.of("values", List.of(Map.of())), schema));
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("values[0].value") && e.contains("required")));
    }

    @Test
    void nestedArray_validObjects_valid() {
        final FieldDef value = FieldDef.builder("value").type("string").required(true).build();
        final FieldDef valueEntityId = FieldDef.builder("valueEntityId").type("integer").required(false).build();
        final FieldDef values = FieldDef.builder("values").type("array")
                .itemType("object").nestedFields(List.of(value, valueEntityId)).required(false).build();

        final SchemaDefinition schema = schema(values);
        assertDoesNotThrow(() -> validator.validate(
                Map.of("values", List.of(Map.of("value", "suthan"), Map.of("value", "test", "valueEntityId", 12))),
                schema));
    }


    @Test
    void multipleViolations_allCollected_notFailFast() {
        final SchemaDefinition schema = schema(
                FieldDef.builder("name").type("string").required(true).build(),
                FieldDef.builder("age").type("integer").min("0").required(true).build(),
                FieldDef.builder("code").type("string").pattern("^[A-Z]+$").required(false).build()
        );
        final McpValidationException ex = assertThrows(McpValidationException.class,
                () -> validator.validate(Map.of("age", "-1", "code", "123"), schema));
        assertTrue(ex.errors().size() >= 2, "Expected ≥2 errors but got: " + ex.errors());
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("name")));
        assertTrue(ex.errors().stream().anyMatch(e -> e.contains("age")));
    }

    @Test
    void nullArgs_treatedAsEmpty_requiredViolation() {
        final SchemaDefinition schema = schema(
                FieldDef.builder("x").type("string").required(true).build()
        );
        final McpValidationException ex = assertThrows(McpValidationException.class,
                () -> validator.validate(null, schema));
        assertFalse(ex.errors().isEmpty());
    }


    private static SchemaDefinition schema(FieldDef... fields) {
        return SchemaDefinition.of(List.of(fields));
    }
}
